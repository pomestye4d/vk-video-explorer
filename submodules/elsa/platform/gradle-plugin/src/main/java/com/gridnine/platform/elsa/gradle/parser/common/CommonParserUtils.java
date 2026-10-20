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

package com.gridnine.platform.elsa.gradle.parser.common;

import com.gridnine.platform.elsa.gradle.meta.common.*;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public final class CommonParserUtils {

    private static final Pattern languagePattern = Pattern.compile(".*_([a-z]+)\\.properties");

    private static Locale ruLocale = Locale.forLanguageTag("ru");

    public static String getIdAttribute(XmlNode node) {
        var id = node.getAttribute("id");
        if (BuildTextUtils.isBlank(id)) {
            throw new IllegalArgumentException("id attribute of element %s".formatted(node.getName()));
        }
        return id;
    }

    public static MetaDataParsingResult parse(File file) throws Exception {
        var content = Files.readAllBytes(file.toPath());
        var node = XmlUtils.parseXml(content);
        var baseName = file.getName().substring(0, file.getName().lastIndexOf("."));
        var dir = new File(file.getParentFile(), "l10n");
        var localizations = new LinkedHashMap<String, Map<Locale, String>>();
        if (dir.exists()) {
            for (File lf : Objects.requireNonNull(dir.listFiles())) {
                if (lf.getName().contains(baseName)) {
                    readLocalizations(lf, localizations);
                }
            }
        }
        return new MetaDataParsingResult(node, localizations);
    }

    private static void readLocalizations(File file, Map<String, Map<Locale, String>> localizations) throws IOException {
        var m = languagePattern.matcher(file.getName());
        if (!m.find()) {
            throw new IllegalArgumentException("unable to detect language from filename %s".formatted(file.getName()));
        }
        var localeStr = m.group(1);
        var locale = new Locale(localeStr);
        var content = Files.readAllBytes(file.toPath());
        var props = new Properties();
        props.load(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8));
        props.forEach((key, value) -> localizations.computeIfAbsent((String) key,
                k -> new LinkedHashMap<>()).put(locale, (String) value));
    }

    public static <T extends BaseModelElementDescription> void updateLocalizationsOfChild(T description, Map<String, Map<Locale, String>> localizations, String parentId) {
        var id = parentId == null ? "%s.name".formatted(description.getId()) : "%s.%s.name".formatted(parentId, description.getId());
        updateLocalizations(description, localizations, id);
    }

    public static <T extends BaseModelElementDescription> void updateLocalizations(Map<Locale, String> displayNames, Map<String, Map<Locale, String>> localizations, String id) {
        var locs = localizations.get(id);
        if (locs == null) {
            return;
        }
        displayNames.putAll(locs);
    }

    private static <T extends BaseModelElementDescription> void updateLocalizations(T description, Map<String, Map<Locale, String>> localizations, String id) {
        updateLocalizations(description.getDisplayNames(), localizations, id);
    }


    public static void fillEntityDescription(XmlNode elm, EntityDescription description) {
        description.setAbstract("true".equals(elm.getAttribute("abstract")));
        description.setExtendsId(elm.getAttribute("extends"));
        elm.getChildren("property").forEach(prop -> {
            var pd = description.getProperties().computeIfAbsent(getIdAttribute(prop), StandardPropertyDescription::new);
            pd.setClassName(prop.getAttribute("class-name"));
            pd.setNonNullable("true".equals(prop.getAttribute("non-nullable")));
            pd.setType(StandardValueType.valueOf(prop.getAttribute("type")));
        });
        elm.getChildren("collection").forEach(coll -> {
            var cd = description.getCollections().computeIfAbsent(getIdAttribute(coll), StandardCollectionDescription::new);
            cd.setElementClassName(coll.getAttribute("element-class-name"));
            cd.setElementType(StandardValueType.valueOf(coll.getAttribute("element-type")));
            cd.setUnique("true".equals(coll.getAttribute("unique")));
        });
        elm.getChildren("map").forEach(map -> {
            var md = description.getMaps().computeIfAbsent(getIdAttribute(map), StandardMapDescription::new);
            md.setKeyClassName(map.getAttribute("key-class-name"));
            md.setKeyType(StandardValueType.valueOf(map.getAttribute("key-type")));
            md.setValueClassName(map.getAttribute("value-class-name"));
            md.setValueType(StandardValueType.valueOf(map.getAttribute("value-type")));
        });
        updateParameters(elm, description);

    }

    public static EntityDescription updateEntity(Map<String, EntityDescription> entities, XmlNode node) {
        var entityDescr = entities.computeIfAbsent(getIdAttribute(node), EntityDescription::new);
        fillEntityDescription(node, entityDescr);
        return entityDescr;
    }

    public static void updateEnum(Map<String, EnumDescription> enums, XmlNode node, Map<String, Map<Locale, String>> localizations) {
        var ed = enums.computeIfAbsent(getIdAttribute(node), EnumDescription::new);
        node.getChildren("enum-item").forEach(item -> {
            var id = getIdAttribute(item);
            var ei = ed.getItems().computeIfAbsent(id, EnumItemDescription::new);
            if (localizations != null) {
                updateLocalizationsOfChild(ei, localizations, ed.getId());
            }
            updateDisplayName(item, ei.getDisplayNames());
        });
    }

    public static void updateParameters(XmlNode node, BaseModelElementDescription elm) {
        node.getChildren("parameter").forEach(child -> elm.getParameters().put(child.getAttribute("name"), child.getAttribute("value")));
    }
    public static void updateLocalizationFromAttribute(XmlNode node, String attrName, Map<Locale, String> titleMap) {
        String title = node.getAttribute(attrName);
        if(BuildTextUtils.isBlank(title)){
            return;
        }
        for(var item: title.split("\\|")){
            var t = item.trim();
            if(BuildTextUtils.isBlank(item)){
                continue;
            }
            if(t.startsWith("en:")){
                titleMap.put(Locale.ENGLISH, t.substring(3));
            } else if (t.startsWith("ru:")){
                titleMap.put(ruLocale, t.substring(3));
            } else {
                titleMap.put(Locale.ENGLISH, t);
            }
        }
    }
    public static void updateDisplayName(XmlNode node, Map<Locale, String> titleMap) {
        updateLocalizationFromAttribute(node,"displayName", titleMap);
    }

}
