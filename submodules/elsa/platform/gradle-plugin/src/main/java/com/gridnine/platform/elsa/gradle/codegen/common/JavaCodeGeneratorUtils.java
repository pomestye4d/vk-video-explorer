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

package com.gridnine.platform.elsa.gradle.codegen.common;

import com.gridnine.platform.elsa.gradle.meta.common.*;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class JavaCodeGeneratorUtils {

    public static String getPackage(String className) {
        var idx = className.lastIndexOf(".");
        return idx == -1? "java.lang": className.substring(0, idx);
    }

    public static String getSimpleName(String className) {
        if(className.contains("<")){
            int idx = className.indexOf("<");
            var cl1 = className.substring(0, idx);
            var cl2 = className.substring(idx + 1, className.length()-1);
            if(cl2.contains("<")){
                var idx2 = cl2.indexOf("<");
                var cl22 = cl2.substring(0, idx2);
                var cl3 = cl2.substring(idx2 + 1, cl2.length()-1);
                return  "%s<%s<%s>>".formatted(getSimpleName(cl1), cl22, cl3);
            }
            return "%s<%s>".formatted(getSimpleName(cl1), cl2);
        }
        var idx = className.lastIndexOf(".");
        return idx == -1? className: className.substring(idx + 1);
    }

    public static File saveIfDiffers(String content, String fileName, File destDir) throws IOException {
        var parts = fileName.split("\\.");
        var currentFile = destDir;
        var length = parts.length;
        for (int n = 0; n < length - 2; n++) {
            currentFile = new File(currentFile, parts[n] + "/");
            assert currentFile.exists() || currentFile.mkdirs();
        }
        currentFile = new File(currentFile, parts[parts.length - 2] + "." + parts[parts.length - 1]);
        if (currentFile.exists()) {
            var currentContent = Files.readString(currentFile.toPath(), StandardCharsets.UTF_8);
            if (currentContent.equals(content)) {
                return currentFile;
            }
        }
        while (!currentFile.getParentFile().exists()) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //noops
            }
            //noinspection ResultOfMethodCallIgnored
            currentFile.getParentFile().mkdirs();
        }
        Files.writeString(currentFile.toPath(), content);
        return currentFile;
    }

    public static void generateJavaEntityConfiguratorCode(EntityDescription ed, JavaCodeGenerator gen) throws Exception {
        gen.addImport("com.gridnine.platform.elsa.common.meta.common.EntityDescription");
        gen.wrapWithBlock(null, () -> {
            gen.printLine("var entityDescription = registry.getEntities().computeIfAbsent(\"%s\",EntityDescription::new);".formatted(ed.getId()));
            generateJavaEntityConfiguratorCode("entityDescription", ed, gen);
        });
    }

    public static void generateJavaEnumConfiguratorCode(EnumDescription ed, JavaCodeGenerator gen) throws Exception {
        gen.addImport("com.gridnine.platform.elsa.common.meta.common.EnumDescription");
        gen.wrapWithBlock(null, () -> {
            gen.printLine("var enumDescription = registry.getEnums().computeIfAbsent(\"%s\", EnumDescription::new);".formatted(ed.getId()));
            for (EnumItemDescription eid : ed.getItems().values()) {
                gen.addImport("com.gridnine.platform.elsa.common.meta.common.EnumItemDescription");
                gen.wrapWithBlock(null, () -> {
                    gen.printLine("var enumItemDescription = new EnumItemDescription(\"%s\");".formatted(eid.getId()));
                    for (Map.Entry<Locale, String> entry : eid.getDisplayNames().entrySet()) {
                        gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                        gen.printLine("enumItemDescription.getDisplayNames().put(LocaleUtils.getLocale(\"%s\",\"%s\"), \"%s\");".formatted(entry.getKey().getLanguage(), entry.getKey().getCountry(), entry.getValue()));
                    }
                    gen.printLine("enumDescription.getItems().put(enumItemDescription.getId(), enumItemDescription);");
                });
            }
        });
    }

    public static void generateJavaEntityConfiguratorCode(String descriptionName, EntityDescription ed, JavaCodeGenerator gen) throws Exception {
        if (ed.isAbstract()) {
            gen.printLine("%s.setAbstract(true);".formatted(descriptionName));
        }
        if (ed.getExtendsId() != null) {
            gen.printLine("%s.setExtendsId(\"%s\");".formatted(descriptionName, ed.getExtendsId()));
        }
        ed.getParameters().forEach((k,v) ->{
            gen.printLine("%s.getParameters().put(\"%s\", \"%s\");".formatted(descriptionName, k, v));
        });
        for (StandardPropertyDescription pd : ed.getProperties().values()) {
            gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardPropertyDescription");
            gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
            gen.wrapWithBlock(null, () -> {
                gen.printLine("var propertyDescription = new StandardPropertyDescription(\"%s\");".formatted(pd.getId()));
                gen.printLine("propertyDescription.setType(StandardValueType.%s);".formatted(pd.getType().name()));
                if (pd.getClassName() != null) {
                    gen.printLine("propertyDescription.setClassName(\"%s\");".formatted(pd.getClassName()));
                }
                if (pd.isNonNullable()) {
                    gen.printLine("propertyDescription.setNonNullable(true);");
                }
                gen.printLine("%s.getProperties().put(propertyDescription.getId(), propertyDescription);".formatted(descriptionName));
            });
        }
        for (StandardCollectionDescription cd : ed.getCollections().values()) {
            gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardCollectionDescription");
            gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
            gen.wrapWithBlock(null, () -> {
                gen.printLine("var collectionDescription = new StandardCollectionDescription(\"%s\");".formatted(cd.getId()));
                gen.printLine("collectionDescription.setElementType(StandardValueType.%s);".formatted(cd.getElementType().name()));
                if (cd.getElementClassName() != null) {
                    gen.printLine("collectionDescription.setElementClassName(\"%s\");".formatted(cd.getElementClassName()));
                }
                if (cd.isUnique()) {
                    gen.printLine("collectionDescription.setUnique(true);");
                }
                gen.printLine("%s.getCollections().put(collectionDescription.getId(), collectionDescription);".formatted(descriptionName));
            });
        }
        for (StandardMapDescription md : ed.getMaps().values()) {
            gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardMapDescription");
            gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
            gen.wrapWithBlock(null, () -> {
                gen.printLine("var mapDescription = new StandardMapDescription(\"%s\");".formatted(md.getId()));
                gen.printLine("mapDescription.setKeyType(StandardValueType.%s);".formatted(md.getKeyType().name()));
                gen.printLine("mapDescription.setValueType(StandardValueType.%s);".formatted(md.getValueType().name()));
                if (md.getKeyClassName() != null) {
                    gen.printLine("mapDescription.setKeyClassName(\"%s\");".formatted(md.getKeyClassName()));
                }
                if (md.getValueClassName() != null) {
                    gen.printLine("mapDescription.setValueClassName(\"%s\");".formatted(md.getValueClassName()));
                }
                gen.printLine("%s.getMaps().put(mapDescription.getId(), mapDescription);".formatted(descriptionName));
            });
        }
    }
    public static void generateJavaEnumCode(EnumDescription ed, File destDir, Set<File> generatedFiles) throws Exception {
        generateJavaEnumCode(ed, false, destDir, generatedFiles);
    }
    public static void generateJavaEnumCode(EnumDescription ed, boolean genToStringLocalization, File destDir,  Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        gen.setPackageName(JavaCodeGeneratorUtils.getPackage(ed.getId()));
        gen.wrapWithBlock("public enum %s".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getId())), () -> {
            var items = new ArrayList<>(ed.getItems().values());
            int size = items.size();
            for (int n = 0; n < size; n++) {
                gen.blankLine();
                var item = items.get(n);
                if(genToStringLocalization){
                    gen.wrapWithBlock(item.getId(), "{", "}%s".formatted(n == size - 1 ? "" : ","), ()->{
                        gen.blankLine();
                        gen.printLine("@Override");
                        gen.wrapWithBlock("public String toString()", ()->{
                            gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                            gen.printLine("return LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, name());"
                                    .formatted(BuildTextUtils.joinToString(item.getDisplayNames().keySet().stream().map("\"%s\""::formatted).toList(),
                                                    ","),
                                            BuildTextUtils.joinToString(item.getDisplayNames().values().stream().map("\"%s\""::formatted).toList(),
                                                    ", ")));
                        });
                    });

                    continue;
                }
                gen.printLine("%s%s".formatted(item.getId(), n == size - 1 ? "" : ","));
            }
        });
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), ed.getId() + ".java", destDir);
        generatedFiles.add(file);
    }

    public static void generateJavaEntityCode(EntityDescription ed, File destDir, String defaultExtendsId, boolean sealable, Set<File> generatedFiles) throws Exception {
        var ged = new GenEntityDescription();
        ged.setAbstract(ed.isAbstract());
        String extendsId = ed.getExtendsId();
        if(extendsId == null){
            extendsId = defaultExtendsId;
        }
        ged.setExtendsId(extendsId);
        ged.setId(ed.getId());
        ged.getProperties().putAll(ed.getProperties());
        ged.getCollections().putAll(ed.getCollections());
        ged.getMaps().putAll(ed.getMaps());
        ged.setSealable(sealable);
        generateJavaEntityCode(ged, destDir, generatedFiles);
    }

    public static void generateJavaEntityCode(GenEntityDescription ed, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        var packageName = JavaCodeGeneratorUtils.getPackage(ed.getId());
        gen.setPackageName(packageName);
        var extendsId = ed.getExtendsId();
        if (extendsId == null || "Object".equals(extendsId)) {
            extendsId = "com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject";
        }
        var extId1 = extendsId;
        var extId2 = extendsId;
        var index = extendsId.indexOf("<");
        if (index != -1) {
            extId1 = extendsId.substring(0, index);
            extId2 = extendsId.substring(index + 1, extendsId.length() - 1);
        }
        if (!packageName.equals(getPackage(extId1))) {
            gen.addImport(extId1);
        }
        if (!packageName.equals(getPackage(extId2))) {
            gen.addImport(extId2);
        }
        String implementsId = null;
        if (ed.getToLocalizableStringExpression() != null) {
            implementsId = "com.gridnine.platform.elsa.common.core.model.common.Localizable";
            gen.addImport("com.gridnine.platform.elsa.common.core.model.common.L10nMessage");
        }
        if (implementsId != null && !implementsId.equals(getPackage(extendsId))) {
            gen.addImport(implementsId);
        }
        if(ed.isSealable()){
            gen.addImport("com.gridnine.platform.elsa.common.core.model.domain.Sealable");
            gen.addImport("com.gridnine.platform.elsa.common.core.model.common.Xeption");
        }
        var cnsb = new StringBuilder();
        cnsb.append("public");
        if (ed.isAbstract()) {
            cnsb.append(" abstract");
        }
        cnsb.append(" class %s extends %s".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getId()),
                !extId1.equals(extId2) ? "%s<%s>".formatted(JavaCodeGeneratorUtils.getSimpleName(extId1),
                        JavaCodeGeneratorUtils.getSimpleName(extId2))
                        : JavaCodeGeneratorUtils.getSimpleName(extendsId)));
        if (implementsId != null) {
            cnsb.append(" implements %s".formatted(JavaCodeGeneratorUtils.getSimpleName(implementsId)));
        }
        if(ed.isSealable()){
            if (implementsId != null){
                cnsb.append(", Sealable");
            } else {
                cnsb.append(" implements Sealable");
            }
        }
        gen.wrapWithBlock(cnsb.toString(), () -> {
            for (StandardPropertyDescription pd : ed.getProperties().values()) {
                gen.blankLine();
                String className = getPropertyType(pd.getType(), pd.getClassName(), pd.isNonNullable(), gen);
                gen.printLine("private %s %s;".formatted(className, pd.getId()));
            }
            for (StandardCollectionDescription cd : ed.getCollections().values()) {
                gen.blankLine();
                String className = getPropertyType(cd.getElementType(), cd.getElementClassName(), false, gen);
                gen.addImport("java.util.*");
                gen.printLine("private final %s<%s> %s = new %s<>();".formatted(cd.isUnique() ? "Set" : "List", className, cd.getId(), cd.isUnique() ? "HashSet" : "ArrayList"));
            }
            for (StandardMapDescription md : ed.getMaps().values()) {
                gen.blankLine();
                gen.addImport("java.util.*");
                String keyClassName = getPropertyType(md.getKeyType(), md.getKeyClassName(), false, gen);
                String valueClassName = getPropertyType(md.getValueType(), md.getValueClassName(), false, gen);
                gen.printLine("private final Map<%s,%s> %s = new HashMap<>();".formatted(keyClassName, valueClassName, md.getId()));
            }
            if(ed.isSealable()){
                for (StandardPropertyDescription pd : ed.getProperties().values()) {
                    gen.blankLine();
                    String className = getPropertyType(pd.getType(), pd.getClassName(), pd.isNonNullable(), gen);
                    gen.wrapWithBlock("public %s %s%s()".formatted(className, className.equals("boolean") ? "is" : "get", BuildTextUtils.capitalize(pd.getId())), () -> gen.printLine("return this.%s;".formatted(pd.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%s(%s value)".formatted(BuildTextUtils.capitalize(pd.getId()), className), () -> {
                        gen.wrapWithBlock("if(isSealed())", () -> gen.printLine("throw Xeption.forDeveloper(\"Object is sealed\");"));
                        gen.printLine("this.%s = value;".formatted(pd.getId()));
                    });
                }
                for (StandardCollectionDescription cd : ed.getCollections().values()) {
                    gen.blankLine();
                    gen.addImport("java.util.*");
                    String className = getPropertyType(cd.getElementType(), cd.getElementClassName(), false, gen);
                    gen.wrapWithBlock("public %s<%s> get%s()".formatted(cd.isUnique() ? "Set" : "List", className, BuildTextUtils.capitalize(cd.getId())), () -> gen.printLine("return isSealed()? Collections.unmodifiable%s(this.%s): this.%s;".formatted(cd.isUnique()? "Set": "List", cd.getId(), cd.getId())));
                }
                for (StandardMapDescription md : ed.getMaps().values()) {
                    gen.blankLine();
                    gen.addImport("java.util.*");
                    String keyClassName = getPropertyType(md.getKeyType(), md.getKeyClassName(), false, gen);
                    String valueClassName = getPropertyType(md.getValueType(), md.getValueClassName(), false, gen);
                    gen.wrapWithBlock("public Map<%s,%s> get%s()".formatted(keyClassName,valueClassName, BuildTextUtils.capitalize(md.getId())), () -> gen.printLine("return isSealed()? Collections.unmodifiableMap(this.%s): this.%s;".formatted(md.getId(), md.getId())));
                }

            }  else {
                for (StandardPropertyDescription pd : ed.getProperties().values()) {
                    gen.blankLine();
                    String className = getPropertyType(pd.getType(), pd.getClassName(), pd.isNonNullable(), gen);
                    gen.wrapWithBlock("public %s %s%s()".formatted(className, className.equals("boolean") ? "is" : "get", BuildTextUtils.capitalize(pd.getId())), () -> gen.printLine("return %s;".formatted(pd.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%s(%s value)".formatted(BuildTextUtils.capitalize(pd.getId()), className), () -> gen.printLine("this.%s = value;".formatted(pd.getId())));
                }
                for (StandardCollectionDescription cd : ed.getCollections().values()) {
                    gen.blankLine();
                    gen.addImport("java.util.*");
                    String className = getPropertyType(cd.getElementType(), cd.getElementClassName(), false, gen);
                    gen.wrapWithBlock("public %s<%s> get%s()".formatted(cd.isUnique() ? "Set" : "List", className, BuildTextUtils.capitalize(cd.getId())), () -> gen.printLine("return %s;".formatted(cd.getId())));
                }
                for (StandardMapDescription md : ed.getMaps().values()) {
                    gen.blankLine();
                    gen.addImport("java.util.*");
                    String keyClassName = getPropertyType(md.getKeyType(), md.getKeyClassName(), false, gen);
                    String valueClassName = getPropertyType(md.getValueType(), md.getValueClassName(), false, gen);
                    gen.wrapWithBlock("public Map<%s,%s> get%s()".formatted(keyClassName, valueClassName, BuildTextUtils.capitalize(md.getId())), () -> gen.printLine("return %s;".formatted(md.getId())));
                }
            }
            if (!ed.getProperties().isEmpty()) {
                gen.blankLine();
                gen.printLine("@Override");
                gen.wrapWithBlock("public Object getValue(String propertyName)", () -> {
                    for (StandardPropertyDescription pd : ed.getProperties().values()) {
                        gen.blankLine();
                        gen.wrapWithBlock("if(\"%s\".equals(propertyName))".formatted(pd.getId()), () -> gen.printLine("return this.%s;".formatted(pd.getId())));
                    }
                    gen.blankLine();
                    gen.printLine("return super.getValue(propertyName);");
                });
                gen.blankLine();
                gen.printLine("@Override");
                gen.wrapWithBlock("public void setValue(String propertyName, Object value)", () -> {
                    gen.blankLine();
                    if(ed.isSealable()) {
                        gen.wrapWithBlock("if(isSealed())", () -> gen.printLine("throw Xeption.forDeveloper(\"Object is sealed\");"));
                    }
                    for (StandardPropertyDescription pd : ed.getProperties().values()) {
                        gen.blankLine();
                        gen.wrapWithBlock("if(\"%s\".equals(propertyName))".formatted(pd.getId()), () -> {
                            if (pd.getType() == StandardValueType.ENTITY_REFERENCE) {
                                gen.printLine("//noinspection unchecked");
                            }
                            gen.printLine("this.%s = (%s) value;".formatted(pd.getId(), getPropertyType(pd.getType(), pd.getClassName(), pd.isNonNullable(), gen)));
                            gen.printLine("return;");
                        });
                    }
                    gen.blankLine();
                    gen.printLine("super.setValue(propertyName, value);");
                });
            }
            if (!ed.getCollections().isEmpty()) {
                gen.blankLine();
                gen.printLine("@Override");
                gen.wrapWithBlock("public Collection<?> getCollection(String collectionName)", () -> {
                    for (StandardCollectionDescription cd : ed.getCollections().values()) {
                        gen.blankLine();
                        gen.wrapWithBlock("if(\"%s\".equals(collectionName))".formatted(cd.getId()), () -> gen.printLine("return this.%s;".formatted(cd.getId())));
                    }
                    gen.blankLine();
                    gen.printLine("return super.getCollection(collectionName);");
                });
            }
            if (!ed.getMaps().isEmpty()) {
                gen.blankLine();
                gen.printLine("@Override");
                gen.wrapWithBlock("public Map<?,?> getMap(String mapName)", () -> {
                    for (StandardMapDescription md : ed.getMaps().values()) {
                        gen.blankLine();
                        gen.wrapWithBlock("if(\"%s\".equals(mapName))".formatted(md.getId()), () -> gen.printLine("return %s;".formatted(md.getId())));
                    }
                    gen.blankLine();
                    gen.printLine("return super.getMap(mapName);");
                });
            }
            if (ed.getToLocalizableStringExpression() != null) {
                gen.blankLine();
                gen.printLine("@Override");
                gen.addImport("java.util.*");
                gen.wrapWithBlock("public String toString(Locale locale)", () -> gen.printLine("return %s;".formatted(ed.getToLocalizableStringExpression())));
            }
            if (ed.getToStringExpression() != null) {
                gen.blankLine();
                gen.printLine("@Override");
                gen.wrapWithBlock("public String toString()", () -> gen.printLine("return %s;".formatted(ed.getToStringExpression())));
            }
            if(ed.isSealable()){
                gen.blankLine();
                gen.printLine("@Override");
                gen.wrapWithBlock("public void seal()", () -> {
                    gen.printLine("super.seal();");
                    for (StandardPropertyDescription pd : ed.getProperties().values()) {
                        if(pd.getType() == StandardValueType.ENTITY || pd.getType() == StandardValueType.ENTITY_REFERENCE){
                            gen.blankLine();
                            gen.wrapWithBlock("if(%s instanceof Sealable slb)".formatted(pd.getId()), ()-> gen.printLine("slb.seal();"));
                        }
                    }
                    for (StandardCollectionDescription cd : ed.getCollections().values()) {
                        gen.blankLine();
                        if(cd.getElementType() == StandardValueType.ENTITY_REFERENCE || cd.getElementType() == StandardValueType.ENTITY){
                            gen.printLine("""
                                    %s.forEach(it -> {
                                     if(it instanceof Sealable slb){
                                        slb.seal();
                                     }
                                    });""".formatted(cd.getId()));
                        }
                    }
                    for (StandardMapDescription md : ed.getMaps().values()) {
                        gen.blankLine();
                        if(md.getKeyType() == StandardValueType.ENTITY_REFERENCE || md.getValueType() == StandardValueType.ENTITY_REFERENCE ||
                                md.getKeyType() == StandardValueType.ENTITY|| md.getValueType() == StandardValueType.ENTITY){
                            gen.wrapWithBlock("%s.forEach".formatted(md.getId()), "(", ")", ()-> gen.wrapWithBlock("(k,v)->", () ->{
                                if(md.getKeyType() == StandardValueType.ENTITY_REFERENCE || md.getKeyType() == StandardValueType.ENTITY){
                                    gen.printLine("k.seal();");
                                }
                                if(md.getValueType() == StandardValueType.ENTITY_REFERENCE || md.getValueType() == StandardValueType.ENTITY){
                                    gen.printLine("v.seal();");
                                }
                            }));
                        }
                    }

                } );
            }
            if(!ed.isAbstract() && ed.isGenerateToReference()){
                gen.blankLine();
                gen.addImport("com.gridnine.platform.elsa.common.core.model.domain.EntityReference");
                gen.wrapWithBlock("public EntityReference<%s> toReference()".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getId())), () -> {
                    gen.printLine("return new EntityReference<>(this);");
                });
            }
        });

        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), ed.getId() + ".java", destDir);
        generatedFiles.add(file);
    }

    public static String getPropertyType(StandardValueType type, String className, boolean noNullable, JavaCodeGenerator gen) {
        return switch (type) {
            case STRING, CLASS -> "String";
            case LOCAL_DATE -> {
                gen.addImport(LocalDate.class.getName());
                yield LocalDate.class.getSimpleName();
            }
            case LOCAL_DATE_TIME -> {
                gen.addImport(LocalDateTime.class.getName());
                yield LocalDateTime.class.getSimpleName();
            }
            case INSTANT -> {
                gen.addImport(Instant.class.getName());
                yield Instant.class.getSimpleName();
            }
            case BOOLEAN -> noNullable ? "boolean" : "Boolean";
            case BYTE_ARRAY -> "byte[]";
            case ENUM, ENTITY -> {
                if(className.contains("<")){
                    int idx = className.indexOf("<");
                    var cl1 = className.substring(0, idx);
                    gen.addImport(cl1);
                    var cl2 = className.substring(idx + 1, className.length()-1);
                    if(cl2.contains("<")){
                        var idx2 = cl2.indexOf("<");
                        var cl22 = cl2.substring(0, idx2);
                        cl22 = addImport(cl22, gen);
                        var cl3 = cl2.substring(idx2 + 1, cl2.length()-1);
                        if(cl3.contains(".")) {
                            cl3 = addImport(cl3, gen);
                        }
                        yield "%s<%s<%s>>".formatted(getSimpleName(cl1), cl22, cl3);
                    } else if(cl2.contains(".")) {
                        cl2 = addImport(cl2, gen);
                    }
                    yield "%s<%s>".formatted(getSimpleName(cl1), cl2);
                }
                var pk = getPackage(className);
                if (!gen.getPackageName().equals(pk) && !"Object".equals(className)) {
                    gen.addImport(className);
                }
                yield getSimpleName(className);
            }

            case ENTITY_REFERENCE -> {
                var pk = getPackage(className);
                if (!gen.getPackageName().equals(pk)) {
                    gen.addImport(className);
                }
                gen.addImport("com.gridnine.platform.elsa.common.core.model.domain.EntityReference");
                yield "EntityReference<%s>".formatted(JavaCodeGeneratorUtils.getSimpleName(className));
            }
            case LONG -> noNullable ? "long" : "Long";
            case UUID -> {
                gen.addImport("java.util.UUID");
                yield "UUID";
            }
            case INT -> noNullable ? "int" : "Integer";
            case BIG_DECIMAL -> {
                gen.addImport(BigDecimal.class.getName());
                yield BigDecimal.class.getSimpleName();
            }
            case TEXT_SEARCH -> {
                gen.addImport("com.gridnine.platform.elsa.common.core.model.domain.TextSearchValue");
                yield "TextSearchValue";
            }
        };
    }

    private static String addImport(String cl22, JavaCodeGenerator gen) {
        if(!cl22.contains(",")){
            gen.addImport(cl22.trim());
            return getSimpleName(cl22);
        }
        var result = new ArrayList<String>();
        Arrays.stream(cl22.split(",")).forEach(cl ->{
            gen.addImport(cl.trim());
            result.add(getSimpleName(cl));
        });
        return BuildTextUtils.joinToString(result, ", ");
    }

    public static String toCamelCased(String name) {
        var str = name.replace(":", "_").replace("-", "_");
        var result = new StringBuilder();
        boolean underscore = false;
        for (int n = 0; n < str.length(); n++) {
            var letter = str.substring(n, n + 1);
            if (letter.equals("_")) {
                underscore = true;
                continue;
            }
            result.append(underscore ? letter.toUpperCase(Locale.ROOT) : letter);
            underscore = false;
        }
        return result.toString();
    }
}
