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

package com.gridnine.platform.elsa.gradle.codegen.adminUi.standalone;

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.adminUi.AdminUiMetaRegistry;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneButtonDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneEditableTableDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneRichTextEditorDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneTextFieldDescription;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.*;

public class JavaAdminUiStandaloneEditorHelper {

    public static void collectButtonFields(StandaloneButtonDescription cd, Map<String, String> fields) {
        fields.put(cd.getId(), "com.gridnine.platform.elsa.admin.web.common.Button");
    }

    public static void generateButtonEditor(StandaloneButtonDescription cd, String containerName, JavaCodeGenerator gen) {
        gen.addImport("com.gridnine.platform.elsa.admin.web.common.Button");
        gen.addImport("com.gridnine.platform.elsa.admin.web.common.ButtonConfiguration");
        var configName = "%sConfig".formatted(containerName);
        gen.printLine("var %s = new ButtonConfiguration();".formatted(configName));
        if (cd.getTooltip() != null) {
            gen.printLine("%s.setTooltip(%s);".formatted(configName, generateLocalizable(cd.getTooltip(), gen)));
        }
        if (cd.getButtonTitle() != null) {
            gen.printLine("%s.setTitle(%s);".formatted(configName, generateLocalizable(cd.getButtonTitle(), gen)));
        }
        if (cd.getIcon() != null) {
            gen.printLine("%s.setIcon(\"%s\");".formatted(configName, cd.getIcon()));
        }
        if (cd.getEnablingExpression() != null) {
            gen.printLine("%s.setEnablingExpression(\"%s\");".formatted(configName, cd.getEnablingExpression()));
        }
        gen.printLine("%s = new Button(\"%s\", %s, context);".formatted(cd.getId(), cd.getId(), configName));
        gen.printLine("var %s = %s;".formatted(containerName, cd.getId()));
    }

    private static String generateLocalizable(String buttonTitle, JavaCodeGenerator gen) {
        if(buttonTitle.contains("|")){
            var locales = new LinkedHashMap<String, String>();
            Arrays.stream(buttonTitle.split("\\|")).forEach(item ->{
                var idx = item.indexOf(":");
                var locale = item.substring(0, idx);
                locales.put(locale, item.substring(idx + 1));
            });
            gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
            return "LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, %s)".formatted(BuildTextUtils.joinToString(locales.keySet().stream().map("\"%s\""::formatted).toList(), ", "),
                    BuildTextUtils.joinToString(locales.values().stream().map("\"%s\""::formatted).toList(), ", "), "\"%s\"".formatted(buttonTitle));
        }
        return "\"%s\"".formatted(buttonTitle);
    }

    public static void generateEditor(StandaloneRichTextEditorDescription cd, String containerName, JavaCodeGenerator gen) {
        gen.addImport("com.gridnine.platform.elsa.admin.web.standalone.StandaloneRichTextEditor");
        gen.addImport("com.gridnine.platform.elsa.admin.web.standalone.StandaloneRichTextEditorConfiguration");
        var configName = "%sConfig".formatted(containerName);
        gen.printLine("var %s = new StandaloneRichTextEditorConfiguration();".formatted(configName));
        gen.printLine("%s.setDeferred(true);".formatted(configName));
        gen.printLine("%s.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, \"%s\"));"
                .formatted(configName, BuildTextUtils.joinToString(cd.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                        BuildTextUtils.joinToString(cd.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", "),
                        cd.getId()));
        if (cd.getStyle() != null) {
            gen.printLine("%s.setStyle(\"%s\");".formatted(configName, cd.getStyle()));
        }
        gen.printLine("%s = new StandaloneRichTextEditor(\"%s\", %s, context);".formatted(cd.getId(), cd.getId(), configName));
        gen.printLine("var %s = %s;".formatted(containerName, cd.getId()));
    }

    public static void collectTextFieldFields(StandaloneTextFieldDescription cd, Map<String, String> fields) {
        fields.put(cd.getId(), "com.gridnine.platform.elsa.admin.web.standalone.StandaloneTextField");
    }

    public static void generateTextFieldEditor(StandaloneTextFieldDescription cd, String containerName, JavaCodeGenerator gen) {
        gen.addImport("com.gridnine.platform.elsa.admin.web.standalone.StandaloneTextField");
        gen.addImport("com.gridnine.platform.elsa.admin.web.standalone.StandaloneTextFieldConfiguration");
        var configName = "%sConfig".formatted(containerName);
        gen.printLine("var %s = new StandaloneTextFieldConfiguration();".formatted(configName));
        if (Boolean.TRUE.equals(cd.getDeferred())) {
            gen.printLine("%s.setDeferred(true);".formatted(configName));
        }
        if (cd.getDebounceTime() != null) {
            gen.printLine("%s.setDebounceTime(%s);".formatted(configName, cd.getDebounceTime()));
        }
        if (cd.getEnablingExpression() != null) {
            gen.printLine("%s.setEnablingExpression(\"%s\");".formatted(configName, cd.getEnablingExpression()));
        }
        if (cd.getPlaceholder() != null) {
            gen.printLine("%s.setPlaceholder(\"%s\");".formatted(configName, cd.getPlaceholder()));
        }
        gen.printLine("%s = new StandaloneTextField(\"%s\", %s, context);".formatted(cd.getId(), cd.getId(), configName));
        if (cd.getStyle() != null) {
            gen.printLine("%s.setStyle(\"%s\", context);".formatted(cd.getId(), cd.getStyle()));
        }
        gen.printLine("var %s = %s;".formatted(containerName, cd.getId()));
    }

    public static void collectFields(StandaloneRichTextEditorDescription cd, Map<String, String> fields) {
        fields.put(cd.getId(), "com.gridnine.platform.elsa.admin.web.standalone.StandaloneRichTextEditor");
    }

    public static void generateEditableTableEditor(StandaloneEditableTableDescription cd, String containerName, JavaCodeGenerator gen) {
        gen.addImport("com.gridnine.platform.elsa.admin.web.standalone.EditableTable");
        gen.addImport("com.gridnine.platform.elsa.admin.web.standalone.EditableTableConfiguration");
        var configName = "%sConfig".formatted(containerName);
        gen.printLine("var %s = new EditableTableConfiguration();".formatted(configName));
        var typeParams = getTypeParams(cd).split(",");
        gen.addImport(typeParams[0].substring(1));
        gen.addImport(typeParams[1].substring(0, typeParams[1].length() - 1));
        gen.printLine("%s = new EditableTable<>(\"%s\", \"%s\", \"%s\", %s, context);".formatted(
                cd.getId(),  cd.getId(), cd.getContainerClassName(), cd.getId(), configName));
        gen.printLine("var %s = %s;".formatted(containerName, cd.getId()));
    }

    public static void collectEditableTableFields(StandaloneEditableTableDescription cd, Map<String, String> fields) {
        var typeParams = getTypeParams(cd);
        fields.put(cd.getId(), "com.gridnine.platform.elsa.admin.web.standalone.EditableTable" + typeParams);
    }

    private static String getTypeParams(StandaloneEditableTableDescription cd) {
        if (cd.getColumns().isEmpty()) {
            return "";
        }
        var rowClassName = getRowClassName(cd);
        var summaryClassName = cd.getSummaryDescription() != null ? getSummaryClassName(cd) : "com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject";
        return "<%s, %s>".formatted(rowClassName, summaryClassName);
    }

    static String getRowClassName(StandaloneEditableTableDescription cd) {
        var pkg = JavaCodeGeneratorUtils.getPackage(cd.getContainerClassName());
        var simpleName = JavaCodeGeneratorUtils.getSimpleName(cd.getContainerClassName());
        return "%s.%s%sRow".formatted(pkg, simpleName, BuildTextUtils.capitalize(cd.getId()));
    }

    static String getSummaryClassName(StandaloneEditableTableDescription cd) {
        var pkg = JavaCodeGeneratorUtils.getPackage(cd.getContainerClassName());
        var simpleName = JavaCodeGeneratorUtils.getSimpleName(cd.getContainerClassName());
        return "%s.%s%sSummary".formatted(pkg, simpleName, BuildTextUtils.capitalize(cd.getId()));
    }

    public static void generateDataClasses(AdminUiMetaRegistry metaRegistry, File destDir, Set<File> generatedFiles) throws Exception {
        for (var cd : metaRegistry.getEditableTables().values()) {
            if (!cd.getColumns().isEmpty()) {
                generateRowClass(cd, destDir, generatedFiles);
                if (cd.getSummaryDescription() != null) {
                    generateSummaryClass(cd, destDir, generatedFiles);
                }
            }
        }
    }

    private static void generateRowClass(StandaloneEditableTableDescription cd, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        var rowClassName = getRowClassName(cd);
        var packageName = JavaCodeGeneratorUtils.getPackage(rowClassName);
        var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(rowClassName);
        gen.setPackageName(packageName);
        gen.addImport("com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject");

        gen.wrapWithBlock("public class %s extends BaseIntrospectableObject".formatted(simpleClassName), () -> {
            gen.blankLine();
            gen.printLine("private String id;");
            for (var col : cd.getColumns()) {
                if ("BUTTON".equals(col.getType()) || "DROP_DOWN_BUTTON".equals(col.getType())) {
                    continue;
                }
                gen.blankLine();
                if ("MONEY".equals(col.getType())) {
                    gen.addImport("java.math.BigDecimal");
                    gen.printLine("private BigDecimal %sSum;".formatted(col.getId()));
                    gen.printLine("private String %sCurrencyCode;".formatted(col.getId()));
                }else if ("LINK".equals(col.getType())) {
                    gen.printLine("private String %sDisplayName;".formatted(col.getId()));
                    gen.printLine("private String %s;".formatted(col.getId()));
                } else if ("NUMBER".equals(col.getType())) {
                    gen.addImport("java.math.BigDecimal");
                    gen.printLine("private BigDecimal %s;".formatted(col.getId()));
                }else {
                    gen.printLine("private String %s;".formatted(col.getId()));
                }
            }
            gen.blankLine();
            gen.wrapWithBlock("public String getId()", () -> gen.printLine("return id;"));
            gen.blankLine();
            gen.wrapWithBlock("public void setId(String value)", () -> gen.printLine("this.id = value;"));
            for (var col : cd.getColumns()) {
                if ("BUTTON".equals(col.getType()) || "DROP_DOWN_BUTTON".equals(col.getType())) {
                    continue;
                }
                gen.blankLine();
                if ("MONEY".equals(col.getType())) {
                    var cap = BuildTextUtils.capitalize(col.getId());
                    gen.wrapWithBlock("public BigDecimal get%sSum()".formatted(cap), () ->
                            gen.printLine("return %sSum;".formatted(col.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%sSum(BigDecimal value)".formatted(cap), () ->
                            gen.printLine("this.%sSum = value;".formatted(col.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public String get%sCurrencyCode()".formatted(cap), () ->
                            gen.printLine("return %sCurrencyCode;".formatted(col.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%sCurrencyCode(String value)".formatted(cap), () ->
                            gen.printLine("this.%sCurrencyCode = value;".formatted(col.getId())));
                } else if ("LINK".equals(col.getType())) {
                    var cap = BuildTextUtils.capitalize(col.getId());
                    gen.wrapWithBlock("public String get%s()".formatted(cap), () ->
                            gen.printLine("return %s;".formatted(col.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%s(String value)".formatted(cap), () ->
                            gen.printLine("this.%s = value;".formatted(col.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public String get%sDisplayName()".formatted(cap), () ->
                            gen.printLine("return %sDisplayName;".formatted(col.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%sDisplayName(String value)".formatted(cap), () ->
                            gen.printLine("this.%sDisplayName = value;".formatted(col.getId())));
                } else if ("NUMBER".equals(col.getType())) {
                    var cap = BuildTextUtils.capitalize(col.getId());
                    gen.wrapWithBlock("public BigDecimal get%s()".formatted(cap), () ->
                            gen.printLine("return %s;".formatted(col.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%s(BigDecimal value)".formatted(cap), () ->
                            gen.printLine("this.%s = value;".formatted(col.getId())));
                } else {
                    var cap = BuildTextUtils.capitalize(col.getId());
                    gen.wrapWithBlock("public String get%s()".formatted(cap), () ->
                            gen.printLine("return %s;".formatted(col.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%s(String value)".formatted(cap), () ->
                            gen.printLine("this.%s = value;".formatted(col.getId())));
                }
            }
            gen.blankLine();
            gen.printLine("@Override");
            gen.wrapWithBlock("public Object getValue(String propertyName)", () -> {
                gen.printLine("if(\"id\".equals(propertyName)) return id;");
                for (var col : cd.getColumns()) {
                    if ("BUTTON".equals(col.getType()) || "DROP_DOWN_BUTTON".equals(col.getType())) {
                        continue;
                    }
                    if ("MONEY".equals(col.getType())) {
                        gen.printLine("if(\"%sSum\".equals(propertyName)) return %sSum;".formatted(col.getId(), col.getId()));
                        gen.printLine("if(\"%sCurrencyCode\".equals(propertyName)) return %sCurrencyCode;".formatted(col.getId(), col.getId()));
                    } else if ("LINK".equals(col.getType())) {
                        gen.printLine("if(\"%s\".equals(propertyName)) return %s;".formatted(col.getId(), col.getId()));
                        gen.printLine("if(\"%sDisplayName\".equals(propertyName)) return %sDisplayName;".formatted(col.getId(), col.getId()));
                    } else {
                        gen.printLine("if(\"%s\".equals(propertyName)) return %s;".formatted(col.getId(), col.getId()));
                    }
                }
                gen.printLine("return super.getValue(propertyName);");
            });
            gen.blankLine();
            gen.printLine("@Override");
            gen.wrapWithBlock("public void setValue(String propertyName, Object value)", () -> {
                gen.printLine("if(\"id\".equals(propertyName)) { this.id = (String) value; return; }");
                for (var col : cd.getColumns()) {
                    if ("BUTTON".equals(col.getType())) {
                        continue;
                    }
                    switch (col.getType()) {
                        case "MONEY" -> {
                            gen.printLine("if(\"%sSum\".equals(propertyName)) { this.%sSum = (BigDecimal) value; return; }".formatted(col.getId(), col.getId()));
                            gen.printLine("if(\"%sCurrencyCode\".equals(propertyName)) { this.%sCurrencyCode = (String) value; return; }".formatted(col.getId(), col.getId()));
                        }
                        case "BUTTON", "DROP_DOWN_BUTTON" -> {
                            // no cell data fields for button column types
                        }
                        case "LINK" -> {
                            gen.printLine("if(\"%s\".equals(propertyName)) { this.%s = (String) value; return; }".formatted(col.getId(), col.getId()));
                            gen.printLine("if(\"%sDisplayName\".equals(propertyName)) { this.%sDisplayName = (String) value; return; }".formatted(col.getId(), col.getId()));
                        }
                        case "NUMBER" ->
                                gen.printLine("if(\"%s\".equals(propertyName)) { this.%s = (BigDecimal) value; return; }".formatted(col.getId(), col.getId()));
                        case null, default ->
                                gen.printLine("if(\"%s\".equals(propertyName)) { this.%s = (String) value; return; }".formatted(col.getId(), col.getId()));
                    }
                }
                gen.printLine("super.setValue(propertyName, value);");
            });
        });

        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), rowClassName + ".java", destDir);
        generatedFiles.add(file);
    }

    private static void generateSummaryClass(StandaloneEditableTableDescription cd, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        var summaryClassName = getSummaryClassName(cd);
        var packageName = JavaCodeGeneratorUtils.getPackage(summaryClassName);
        var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(summaryClassName);
        gen.setPackageName(packageName);
        gen.addImport("com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject");

        // Collect unique non-LABEL fields: fieldName -> "NUMBER"|"MONEY_SUM"|"MONEY_CURRENCY"|"STRING"
        var summaryFields = new LinkedHashMap<String, String>();
        for (var row : cd.getSummaryDescription().getRows()) {
            for (var col : row.getColumns()) {
                if ("BUTTON".equals(col.getType()) || "DROP_DOWN_BUTTON".equals(col.getType())) {
                    continue;
                }
                if ("LABEL".equals(col.getType())) continue;
                if ("MONEY".equals(col.getType())) {
                    summaryFields.putIfAbsent(col.getId() + "Sum", "MONEY_SUM");
                    summaryFields.putIfAbsent(col.getId() + "CurrencyCode", "MONEY_CURRENCY");
                }  else if ("LINK".equals(col.getType())) {
                    summaryFields.putIfAbsent(col.getId(), "LINK");
                    summaryFields.putIfAbsent(col.getId() + "DisplayName", "LINK_DISPLAY_NAME");
                }else if ("NUMBER".equals(col.getType())) {
                    summaryFields.putIfAbsent(col.getId(), "NUMBER");
                } else {
                    summaryFields.putIfAbsent(col.getId(), "STRING");
                }
            }
        }

        gen.wrapWithBlock("public class %s extends BaseIntrospectableObject".formatted(simpleClassName), () -> {
            for (var entry : summaryFields.entrySet()) {
                gen.blankLine();
                if ("NUMBER".equals(entry.getValue()) || "MONEY_SUM".equals(entry.getValue())) {
                    gen.addImport("java.math.BigDecimal");
                    gen.printLine("private BigDecimal %s;".formatted(entry.getKey()));
                } else {
                    gen.printLine("private String %s;".formatted(entry.getKey()));
                }
            }
            for (var entry : summaryFields.entrySet()) {
                gen.blankLine();
                var cap = BuildTextUtils.capitalize(entry.getKey());
                if ("NUMBER".equals(entry.getValue()) || "MONEY_SUM".equals(entry.getValue())) {
                    gen.wrapWithBlock("public BigDecimal get%s()".formatted(cap), () ->
                            gen.printLine("return %s;".formatted(entry.getKey())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%s(BigDecimal value)".formatted(cap), () ->
                            gen.printLine("this.%s = value;".formatted(entry.getKey())));
                } else {
                    gen.wrapWithBlock("public String get%s()".formatted(cap), () ->
                            gen.printLine("return %s;".formatted(entry.getKey())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%s(String value)".formatted(cap), () ->
                            gen.printLine("this.%s = value;".formatted(entry.getKey())));
                }
            }
            gen.blankLine();
            gen.printLine("@Override");
            gen.wrapWithBlock("public Object getValue(String propertyName)", () -> {
                for (var entry : summaryFields.entrySet()) {
                    gen.printLine("if(\"%s\".equals(propertyName)) return %s;".formatted(entry.getKey(), entry.getKey()));
                }
                gen.printLine("return super.getValue(propertyName);");
            });
            gen.blankLine();
            gen.printLine("@Override");
            gen.wrapWithBlock("public void setValue(String propertyName, Object value)", () -> {
                for (var entry : summaryFields.entrySet()) {
                    if ("NUMBER".equals(entry.getValue()) || "MONEY_SUM".equals(entry.getValue())) {
                        gen.printLine("if(\"%s\".equals(propertyName)) { this.%s = (BigDecimal) value; return; }".formatted(entry.getKey(), entry.getKey()));
                    } else {
                        gen.printLine("if(\"%s\".equals(propertyName)) { this.%s = (String) value; return; }".formatted(entry.getKey(), entry.getKey()));
                    }
                }
                gen.printLine("super.setValue(propertyName, value);");
            });
        });

        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), summaryClassName + ".java", destDir);
        generatedFiles.add(file);
    }
}
