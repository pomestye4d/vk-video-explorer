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

import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.meta.common.BaseElementWithId;
import com.gridnine.platform.elsa.common.meta.custom.CustomMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class JdbcClassMapperImpl implements ClassMapper {
    private final Map<String, Integer> name2Id = new HashMap<>();
    private final Map<Integer, String> id2name = new HashMap<>();


    public JdbcClassMapperImpl(DomainMetaRegistry metaRegistry, CustomMetaRegistry customMetaRegistry, JdbcTemplate template, JdbcDialect dialect) {
        template.queryForList("select id, classname from classmapping").forEach(map -> {
            var id = ((Number) map.get("id")).intValue();
            var className = (String) map.get("classname");
            name2Id.put(className, id);
            id2name.put(id, className);
        });
        metaRegistry.getEnums().values().forEach(it -> check(template, dialect, it.getId()));
        metaRegistry.getDocuments().values().forEach(it -> {
            if (!it.isAbstract()) {
                check(template, dialect, it.getId());
            }
        });
        metaRegistry.getEntities().values().forEach(it -> {
            if (!it.isAbstract()) {
                check(template, dialect, it.getId());
            }
        });
        metaRegistry.getAssets().values().forEach(it -> {
            if (!it.isAbstract()) {
                check(template, dialect, it.getId());
            }
        });
        metaRegistry.getSearchableProjections().values().forEach(it -> check(template, dialect, it.getId()));
        var transientClasses = customMetaRegistry.getEntities().values().stream()
                .filter(it -> !it.isAbstract()).map(BaseElementWithId::getId).collect(Collectors.toCollection(HashSet::new));
        transientClasses.addAll(customMetaRegistry.getEntities().keySet());
        var res = transientClasses.stream().sorted().toList();
        for (var n = 0; n < res.size(); n++) {
            var id = 1000000 + n;
            var className = res.get(n);
            id2name.put(id, className);
            name2Id.put(className, id);
        }
    }

    @Override
    public int getId(String name) {
        if (!name2Id.containsKey(name)) {
            throw Xeption.forDeveloper("mapping for class %s was not found".formatted(name));
        }
        return name2Id.get(name);
    }

    @Override
    public String getName(int id) {
        if (!id2name.containsKey(id)) {
            throw Xeption.forDeveloper("mapping for id %s was not found".formatted(id));
        }
        return id2name.get(id);
    }

    private void check(JdbcTemplate template, JdbcDialect dialect, String className) {
        if (!name2Id.containsKey(className)) {
            Integer id = null;
            while (id == null || id2name.containsKey(id)) {
                id = template.query(dialect.getSequenceNextValueSql("intid"), rs -> {
                    rs.next();
                    return rs.getInt(1);
                });
            }
            var fId = id;
            template.execute("insert into classmapping(id, classname) values (?, ?)", (PreparedStatementCallback<Void>) ps -> {
                ps.setInt(1, fId);
                ps.setString(2, className);
                ps.execute();
                return null;
            });
            id2name.put(id, className);
            name2Id.put(className, id);
        }
    }
}
