/*
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gridnine.platform.elsa.core.storage.database.jdbc;

import com.gridnine.platform.elsa.common.core.l10n.SupportedLocalesProvider;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.utils.LocaleUtils;
import com.gridnine.platform.elsa.common.core.utils.TextUtils;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JdbcEnumMapperImpl implements EnumMapper {
    private final Map<String, Map<String, Integer>> name2Id = new HashMap<>();
    private final Map<String, Map<Integer, String>> id2name = new HashMap<>();

    public JdbcEnumMapperImpl(DomainMetaRegistry metaRegistry, JdbcTemplate template, SupportedLocalesProvider supportedLocalesProvider, JdbcDialect dialect) {
        metaRegistry.getEnums().values().forEach(it -> {
            var res = template.queryForList("select id, classname, enumconstant, %s from enummapping".formatted(TextUtils.join(supportedLocalesProvider.getSupportedLocales()
                    .stream().map(loc -> "%sname".formatted(loc.getLanguage())).toList(), ", ")));
            var map = new HashMap<String, Integer>();
            for (var enumItem : it.getItems().values()) {
                var row = res.stream().filter(it2 -> enumItem.getId().equals(it2.get("enumconstant")) && it.getId().equals(it2.get("classname"))).findFirst().orElse(null);
                if (row == null) {
                    Integer id = null;
                    boolean found = false;
                    while (id == null || found) {
                        id = template.query(dialect.getSequenceNextValueSql("intid"), rs -> {
                            rs.next();
                            return rs.getInt(1);
                        });
                        var id2 = id;
                        found = res.stream().anyMatch(it2 -> id2 != null && id2.equals(it2.get("id")));
                    }
                    var fid = id;
                    template.execute("insert into enummapping(id, classname, enumconstant, %s) values (?, ?, ?, %s)".formatted(
                            TextUtils.join(supportedLocalesProvider.getSupportedLocales().stream().map(loc -> "%sName".formatted(loc.getLanguage())).toList(), ", "),
                            TextUtils.join(supportedLocalesProvider.getSupportedLocales().stream().map(loc -> "?").toList(), ", ")),
                            (PreparedStatementCallback<Void>) ps -> {
                                ps.setInt(1, fid);
                                ps.setString(2, it.getId());
                                ps.setString(3, enumItem.getId());
                                var idx = 4;
                                for(var loc: supportedLocalesProvider.getSupportedLocales()) {
                                    ps.setString(idx, LocaleUtils.getLocalizedName(enumItem.getDisplayNames(), loc, enumItem.getId()));
                                    idx++;
                                }
                                ps.execute();
                                return null;
                            }
                    );
                    map.put(enumItem.getId(), id);
                } else {
                    var differs = supportedLocalesProvider.getSupportedLocales().stream().anyMatch(loc -> !LocaleUtils.getLocalizedName(enumItem.getDisplayNames(), loc, enumItem.getId()).equals(row.get("%sName".formatted(loc.getLanguage()))));
                    if (differs) {
                        template.execute("update enummapping set %s where enumconstant= ? and classname=?".formatted(
                                TextUtils.join(supportedLocalesProvider.getSupportedLocales().stream()
                                        .map(loc -> "%sName = ?".formatted(loc.getLanguage())).toList(), ", ")), (PreparedStatementCallback<Void>) ps -> {
                                            var idx = 1;
                                            for(var loc: supportedLocalesProvider.getSupportedLocales()) {
                                                ps.setString(idx, LocaleUtils.getLocalizedName(enumItem.getDisplayNames(), loc, enumItem.getId()));
                                                idx++;
                                            }
                                            ps.setString(idx, enumItem.getId());
                                            idx++;
                                            ps.setString(idx, it.getId());
                                            ps.execute();
                                            return null;
                                        });
                    }
                    map.put(enumItem.getId(), (Integer) row.get("id"));
                }

            }
            var nid = new LinkedHashMap<String, Integer>();
            name2Id.put(it.getId(), nid);
            var idn = new LinkedHashMap<Integer, String>();
            id2name.put(it.getId(), idn);
            map.forEach((name, id) -> {
                nid.put(name, id);
                idn.put(id, name);
            });
        });
    }

    @Override
    public int getId(Enum<?> value) {
        var className = value.getDeclaringClass().getName();
        if (!name2Id.containsKey(className)) {
            return value.ordinal();
        }
        var result = name2Id.get(className).get(value.name());
        if (result == null) {
            throw Xeption.forDeveloper("unknown value %s of enum %s".formatted(value.name(), className));
        }
        return result;
    }

    @Override
    public String getName(int id, Class<Enum<?>> cls) {
        var className = cls.getName();
        if (!id2name.containsKey(className)) {
            for (var item : cls.getEnumConstants()) {
                if (item.ordinal() == id) {
                    return item.name();
                }
            }
            throw Xeption.forDeveloper("unknown id %s of enum %s".formatted(id, className));
        }
        var result = id2name.get(className).get(id);
        if (result == null) {
            throw Xeption.forDeveloper("unknown id %s of enum %s".formatted(id, className));
        }
        return result;
    }
}
