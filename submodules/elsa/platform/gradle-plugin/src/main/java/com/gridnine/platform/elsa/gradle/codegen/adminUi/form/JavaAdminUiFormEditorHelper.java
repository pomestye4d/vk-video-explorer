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

package com.gridnine.platform.elsa.gradle.codegen.adminUi.form;

import com.gridnine.platform.elsa.gradle.codegen.adminUi.div.JavaAdminUiDivEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.RootDivDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.form.*;
import com.gridnine.platform.elsa.gradle.meta.adminUi.grid.RootGridDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.RootTabsDescription;
import com.gridnine.platform.elsa.gradle.meta.common.StandardValueType;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaAdminUiFormEditorHelper {


    public static void generateEditor(RootFormDescription fd, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        var packageName = JavaCodeGeneratorUtils.getPackage(fd.getClassName());
        gen.setPackageName(packageName);
        gen.addImport("com.gridnine.platform.elsa.admin.web.form.FormContainer");
        var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(fd.getClassName());
        var allComps = new LinkedHashMap<String, String>();
        collectFields(fd, allComps);
        gen.wrapWithBlock("public class %s extends FormContainer".formatted(simpleClassName), () -> {
            for (var entry : allComps.entrySet()) {
                gen.blankLine();
                String className = entry.getValue();
                var scn = JavaCodeGeneratorUtils.getPropertyType(StandardValueType.ENTITY, className, false, gen);
                gen.printLine("private final %s %s;".formatted(scn, entry.getKey()));
            }
            gen.blankLine();
            gen.addImport("com.gridnine.webpeer.core.ui.OperationUiContext");
            gen.wrapWithBlock("public %s(String tag, OperationUiContext context, String width)".formatted(simpleClassName), () -> {
                gen.blankLine();
                gen.printLine("super(tag, context);");
                gen.printLine("this.setWidth(width, context);");
                if(!fd.getTitle().isEmpty()){
                    gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                    gen.printLine("this.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, null), context);"
                            .formatted(BuildTextUtils.joinToString(fd.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                                    BuildTextUtils.joinToString(fd.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", ")));
                }
                generateFormComponentsInit(fd.getComponents(), gen, destDir, generatedFiles, "addChild");
            });
            for (var entry : allComps.entrySet()) {
                gen.blankLine();
                String className = entry.getValue();
                gen.addImport(className.contains("<") ? className.substring(0, className.indexOf("<")) : className);
                gen.wrapWithBlock("public %s get%s()".formatted(JavaCodeGeneratorUtils.getSimpleName(className), BuildTextUtils.capitalize(entry.getKey())),
                        () -> gen.printLine("return %s;".formatted(entry.getKey())));
            }
        });

        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), fd.getClassName() + ".java", destDir);
        generatedFiles.add(file);
    }

    /**
     * Generates initialization code for a set of form components.
     * For regular components: init + addChildMethod(context, comp, idx).
     *
     * @param components      the components map (top-level, not flattened)
     * @param gen             the code generator
     * @param destDir         destination directory
     * @param generatedFiles  set of generated files
     * @param addChildMethod  "addChild" for root (form/div), or e.g. "this.myTab.addChild" for tab
     */
    public static void generateFormComponentsInit(Map<String, BaseAdminUiFormComponentDescription> components,
                                                  JavaCodeGenerator gen,
                                                  File destDir,
                                                  Set<File> generatedFiles,
                                                  String addChildMethod) {
        final var idx = new AtomicInteger(-1);
        for (var pd : components.values()) {
            gen.blankLine();
            gen.wrapWithBlock("", () -> {
                initSingleComponent(pd, gen);
                gen.printLine("%s(context, this.%s, %s);".formatted(addChildMethod, pd.getId(), idx.incrementAndGet()));
            });
        }
    }

    /**
     * Generates initialization code for a single (non-div) component, without any addChild call.
     */
    private static void initSingleComponent(BaseAdminUiFormComponentDescription pd, JavaCodeGenerator gen) {
        if (pd instanceof FormDivDescription fdd) {
            JavaAdminUiDivEditorHelper.generateNestedComponent(fdd.getContent(), pd.getId() + "Div", gen);
            return;
        }
        var type = getType(pd);
        var index = type.indexOf("<");
        var config = "%sConfiguration".formatted(index == -1 ? type : type.substring(0, index));
        gen.addImport(config);
        if (pd instanceof FormCustomElementDescription cd) {
            gen.printLine("this.%s = new %s(\"%s\", context);".formatted(pd.getId(), JavaCodeGeneratorUtils.getSimpleName(type), pd.getId()));
            if(!cd.getTitle().isEmpty()){
                gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                gen.printLine("this.%s.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, \"%s\"), context);"
                        .formatted(pd.getId(), BuildTextUtils.joinToString(pd.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                                BuildTextUtils.joinToString(pd.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", "),
                                pd.getId()));
            }
        } else {
            gen.printLine("var config = new %s();".formatted(JavaCodeGeneratorUtils.getSimpleName(config)));
            gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
            if (!(pd instanceof FormEntitiesListDescription) && !(pd instanceof FormReferencesListDescription) && !(pd instanceof FormFilesFieldDescription) && !(pd instanceof FormHiddenFieldDescription) && !(pd instanceof FormLabelWithToolsDescription)) {
                gen.printLine("config.setDeferred(true);");
            }
            if (!(pd instanceof FormHiddenFieldDescription)) {
                gen.printLine("config.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, \"%s\"));"
                        .formatted(BuildTextUtils.joinToString(pd.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                                BuildTextUtils.joinToString(pd.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", "),
                                pd.getId()));
            }
            if (pd instanceof FormRemoteMultiSelectDescription rms && !BuildTextUtils.isBlank(rms.getClassName())) {
                gen.addImport("com.gridnine.platform.elsa.admin.web.common.AutocompleteUtils");
                gen.addImport(rms.getClassName());
                gen.printLine("config.setAutocompleteServiceHandler(AutocompleteUtils.createMultiSelectAutocomplete(%s.class));".formatted(JavaCodeGeneratorUtils.getSimpleName(rms.getClassName())));
                gen.printLine("config.setAllowAddNewValue(%s);".formatted(rms.isAllowAddNewValue() ? "true" : "false"));
                gen.printLine("config.setOrderEditable(%s);".formatted(rms.isOrderEditable() ? "true" : "false"));
            }
            if (pd instanceof FormRemoteSelectDescription rms && !BuildTextUtils.isBlank(rms.getClassName())) {
                gen.addImport("com.gridnine.platform.elsa.admin.web.common.AutocompleteUtils");
                gen.addImport(rms.getClassName());
                gen.printLine("config.setAllowAddNewValue(%s);".formatted(rms.isAllowAddNewValue() ? "true" : "false"));
                gen.printLine("config.setAutocompleteServiceHandler(AutocompleteUtils.createSelectAutocomplete(%s.class));".formatted(JavaCodeGeneratorUtils.getSimpleName(rms.getClassName())));
            }
            if (pd instanceof FormEnumSelectDescription ed && !BuildTextUtils.isBlank(ed.getClassName())) {
                gen.addImport("com.gridnine.platform.elsa.admin.web.common.AutocompleteUtils");
                gen.addImport(ed.getClassName());
                gen.printLine("config.getOptions().addAll(AutocompleteUtils.getOptions(%s.class));".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getClassName())));
            }
            if (pd instanceof FormEnumMultiSelectDescription ed && !BuildTextUtils.isBlank(ed.getClassName())) {
                gen.addImport("com.gridnine.platform.elsa.admin.web.common.AutocompleteUtils");
                gen.addImport(ed.getClassName());
                gen.printLine("config.getOptions().addAll(AutocompleteUtils.getOptions(%s.class));".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getClassName())));
            }
            if (pd instanceof FormBigDecimalFieldDescription bd && bd.getPrecision() != null) {
                gen.printLine("config.setPrecision(%s);".formatted(bd.getPrecision()));
            }
            if (pd instanceof FormLabelWithToolsDescription ld && ld.getStyle() != null) {
                gen.printLine("config.setStyle(\"%s\");".formatted(ld.getStyle()));
            }
            var scn = JavaCodeGeneratorUtils.getPropertyType(StandardValueType.ENTITY, type, false, gen);
            gen.printLine("this.%s = new %s(\"%s\", config, context);".formatted(pd.getId(), scn, pd.getId()));
        }
    }

    public static String getFieldType(BaseAdminUiFormComponentDescription pd) {
        return getType(pd);
    }

    public static String getType(BaseAdminUiFormComponentDescription pd) {
        return switch (pd.getType()) {
            case FORM_TEXT_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormTextField";
            case FORM_HIDDEN_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormHiddenField";
            case FORM_INT_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormIntField";
            case FORM_BIG_DECIMAL_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormBigDecimalField";
            case FORM_ENUM_SELECT -> "com.gridnine.platform.elsa.admin.web.form.FormSelect";
            case FORM_REFERENCES_LIST -> "com.gridnine.platform.elsa.admin.web.form.FormReferencesList";
            case FORM_LINK_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormLinkField";
            case FORM_BOOLEAN_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormBooleanField";
            case FORM_DATE_INTERVAL_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormDateIntervalField";
            case FORM_LOCAL_DATE_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormLocalDateField";
            case FORM_LOCAL_TIME_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormLocalTimeField";
            case FORM_LOCAL_PERIOD_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormLocalPeriodField";
            case FORM_TEXT_AREA -> "com.gridnine.platform.elsa.admin.web.form.FormTextArea";
            case FORM_REMOTE_SELECT -> "com.gridnine.platform.elsa.admin.web.form.FormRemoteSelect";
            case FORM_REMOTE_MULTI_SELECT -> "com.gridnine.platform.elsa.admin.web.form.FormRemoteMultiSelect";
            case FORM_SELECT -> "com.gridnine.platform.elsa.admin.web.form.FormSelect";
            case FORM_MULTI_SELECT -> "com.gridnine.platform.elsa.admin.web.form.FormMultiSelect";
            case FORM_ENUM_MULTI_SELECT -> "com.gridnine.platform.elsa.admin.web.form.FormMultiSelect";
            case FORM_FILES_FIELD -> "com.gridnine.platform.elsa.admin.web.form.FormFilesField";
            case FORM_ENTITIES_LIST -> {
                var entityEditor = ((FormEntitiesListDescription) pd).getEntityEditor();
                if (entityEditor instanceof RootFormDescription fd) {
                    yield "com.gridnine.platform.elsa.admin.web.form.FormEntitiesList<%s>".formatted(fd.getClassName());
                }
                if (entityEditor instanceof RootGridDescription gd) {
                    yield "com.gridnine.platform.elsa.admin.web.form.FormEntitiesList<%s>".formatted(gd.getClassName());
                }
                if (entityEditor instanceof RootDivDescription gd) {
                    yield "com.gridnine.platform.elsa.admin.web.form.FormEntitiesList<%s>".formatted(gd.getClassName());
                }
                if (entityEditor instanceof RootTabsDescription gd) {
                    yield "com.gridnine.platform.elsa.admin.web.form.FormEntitiesList<%s>".formatted(gd.getClassName());
                }
                throw new IllegalArgumentException("unsupported type " + pd.getType());
            }
            case FORM_CUSTOM -> ((FormCustomElementDescription) pd).getClassName();
            case FORM_RICH_TEXT_EDITOR -> "com.gridnine.platform.elsa.admin.web.form.FormRichTextEditor";
            case FORM_LABEL_WITH_TOOLS -> "com.gridnine.platform.elsa.admin.web.form.FormLabelWithTools";
            case FORM_DIV -> "com.gridnine.platform.elsa.admin.web.common.Div";
            default ->  throw new IllegalArgumentException("unsupported type " + pd.getType());
        };
    }

    public static void collectFields(NestedFormDescription cd, Map<String, String> fields) {
        cd.getComponents().forEach((id, comp) -> {
            if (comp instanceof FormDivDescription fdd) {
                JavaAdminUiDivEditorHelper.collectFields(fdd.getContent(), fields);
            } else {
                fields.put(id, getType(comp));
            }
        });
    }

    public static void generateNestedComponent(NestedFormDescription cd, String containerName, JavaCodeGenerator gen) {
        gen.addImport("com.gridnine.platform.elsa.admin.web.form.FormContainer");
        gen.printLine("var %s = new FormContainer(\"form\", context);".formatted(containerName));
        if(!cd.getTitle().isEmpty()){
            gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
            gen.printLine("%s.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, null), context);"
                    .formatted(containerName, BuildTextUtils.joinToString(cd.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                            BuildTextUtils.joinToString(cd.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", ")));
        }
        var idx = new AtomicInteger(0);
        for (var pd : cd.getComponents().values()) {
            gen.blankLine();
            gen.wrapWithBlock("", () -> {
                if (pd instanceof FormDivDescription fdd) {
                    JavaAdminUiDivEditorHelper.generateNestedComponent(fdd.getContent(), pd.getId() + "Div", gen);
                    gen.printLine("%s.addChild(context, %s, %s);".formatted(containerName, pd.getId(), idx.getAndIncrement()));
                    return;
                }
                var type = getType(pd);
                var index = type.indexOf("<");
                var config = "%sConfiguration".formatted(index == -1 ? type : type.substring(0, index));
                gen.addImport(config);
                if (pd instanceof FormCustomElementDescription cd1) {
                    gen.printLine("%s = new %s(\"%s\", context);".formatted(pd.getId(), JavaCodeGeneratorUtils.getSimpleName(type), pd.getId()));
                    if(!cd1.getTitle().isEmpty()){
                        gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                        gen.printLine("%s.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, \"%s\"), context);"
                                .formatted(pd.getId(), BuildTextUtils.joinToString(pd.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                                        BuildTextUtils.joinToString(pd.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", "),
                                        pd.getId()));
                    }
                } else {
                    gen.printLine("var config = new %s();".formatted(JavaCodeGeneratorUtils.getSimpleName(config)));
                    gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                    if (!(pd instanceof FormEntitiesListDescription) && !(pd instanceof FormReferencesListDescription) && !(pd instanceof FormFilesFieldDescription) && !(pd instanceof FormHiddenFieldDescription) && !(pd instanceof FormLabelWithToolsDescription)) {
                        gen.printLine("config.setDeferred(true);");
                    }
                    if (pd instanceof FormRemoteSelectDescription rd) {
                        if(rd.isAllowAddNewValue()){
                            gen.printLine("config.setAllowAddNewValue(true);");
                        }
                    }
                    if (pd instanceof FormRemoteMultiSelectDescription rd) {
                        if(rd.isAllowAddNewValue()){
                            gen.printLine("config.setAllowAddNewValue(true);");
                        }
                        if(rd.isOrderEditable()){
                            gen.printLine("config.setOrderEditable(true);");
                        }
                    }
                    if (!(pd instanceof FormHiddenFieldDescription)) {
                        gen.printLine("config.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, \"%s\"));"
                                .formatted(BuildTextUtils.joinToString(pd.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                                        BuildTextUtils.joinToString(pd.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", "),
                                        pd.getId()));
                    }
                    if (pd instanceof FormRemoteMultiSelectDescription rms && !BuildTextUtils.isBlank(rms.getClassName())) {
                        gen.addImport("com.gridnine.platform.elsa.admin.web.common.AutocompleteUtils");
                        gen.addImport(rms.getClassName());
                        gen.printLine("config.setAutocompleteServiceHandler(AutocompleteUtils.createMultiSelectAutocomplete(%s.class));".formatted(JavaCodeGeneratorUtils.getSimpleName(rms.getClassName())));
                        gen.printLine("config.setOrderEditable(%s);".formatted(rms.isOrderEditable()));
                    }
                    if (pd instanceof FormRemoteSelectDescription rms && !BuildTextUtils.isBlank(rms.getClassName())) {
                        gen.addImport("com.gridnine.platform.elsa.admin.web.common.AutocompleteUtils");
                        gen.addImport(rms.getClassName());
                        gen.printLine("config.setAutocompleteServiceHandler(AutocompleteUtils.createSelectAutocomplete(%s.class));".formatted(JavaCodeGeneratorUtils.getSimpleName(rms.getClassName())));
                    }
                    if (pd instanceof FormEnumSelectDescription ed && !BuildTextUtils.isBlank(ed.getClassName())) {
                        gen.addImport("com.gridnine.platform.elsa.admin.web.common.AutocompleteUtils");
                        gen.addImport(ed.getClassName());
                        gen.printLine("config.getOptions().addAll(AutocompleteUtils.getOptions(%s.class));".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getClassName())));
                    }
                    if (pd instanceof FormEnumMultiSelectDescription ed && !BuildTextUtils.isBlank(ed.getClassName())) {
                        gen.addImport("com.gridnine.platform.elsa.admin.web.common.AutocompleteUtils");
                        gen.addImport(ed.getClassName());
                        gen.printLine("config.getOptions().addAll(AutocompleteUtils.getOptions(%s.class));".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getClassName())));
                    }
                    if (pd instanceof FormBigDecimalFieldDescription bd && bd.getPrecision() != null) {
                        gen.printLine("config.setPrecision(%s);".formatted(bd.getPrecision()));
                    }
                    if (pd instanceof FormLabelWithToolsDescription ld && ld.getStyle() != null) {
                        gen.printLine("config.setStyle(\"%s\");".formatted(ld.getStyle()));
                    }
                    var scn = JavaCodeGeneratorUtils.getPropertyType(StandardValueType.ENTITY, type, false, gen);
                    gen.printLine("%s = new %s(\"%s\", config, context);".formatted(pd.getId(), scn, pd.getId()));
                }
                gen.printLine("%s.addChild(context, %s, %s);".formatted(containerName, pd.getId(), idx.getAndIncrement()));
            });
        }
    }
}
