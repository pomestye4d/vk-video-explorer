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

import com.gridnine.platform.elsa.gradle.codegen.adminUi.common.JavaAdminUiCodeGenUtils;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.div.JavaAdminUiDivConfiguratorHelper;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.meta.adminUi.form.*;
import com.gridnine.platform.elsa.gradle.utils.BuildRunnableWithException;

import java.util.Map;

public class JavaAdminUiFormConfiguratorHelper {

    public static void generateComponentsIntoContainer(Map<String, ? extends BaseAdminUiFormComponentDescription> components, String containerDescriptionName, String compName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        for (var comp : components.values()) {
            switch (comp.getType()) {
                case FORM_TEXT_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormTextFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormTextFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_HIDDEN_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormHiddenFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormHiddenFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_INT_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormIntFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormIntFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_BIG_DECIMAL_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormBigDecimalFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormBigDecimalFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        var bd = (FormBigDecimalFieldDescription) comp;
                        if (bd.getPrecision() != null) {
                            gen.printLine("%s.setPrecision(%s);".formatted(compName, bd.getPrecision()));
                        }
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_ENUM_SELECT -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormEnumSelectDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormEnumSelectDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        var ed = (FormEnumSelectDescription) comp;
                        if (ed.getClassName() != null) {
                            gen.printLine("%s.setClassName(\"%s\");".formatted(compName, ed.getClassName()));
                        }
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_FILES_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormFilesFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormFilesFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_REFERENCES_LIST -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormReferencesListDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormReferencesListDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_ENTITIES_LIST -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormEntitiesListDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormEntitiesListDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        var eld = (FormEntitiesListDescription) comp;
                        gen.wrapWithBlock("", () -> {
                            JavaAdminUiCodeGenUtils.generateDescription(eld.getEntityEditor(), "nestedEditor", nestedCount + 1, gen);
                            gen.printLine("%s.setEntityEditor(nestedEditor);".formatted(compName));
                        });
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_LINK_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormLinkFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormLinkFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_BOOLEAN_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormBooleanFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormBooleanFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_TEXT_AREA -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormTextAreaDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormTextAreaDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_DATE_INTERVAL_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormDateIntervalFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormDateIntervalFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_LOCAL_DATE_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormLocalDateFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormLocalDateFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_LOCAL_TIME_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormLocalTimeFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormLocalTimeFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_LOCAL_PERIOD_FIELD -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormLocalPeriodFieldDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormLocalPeriodFieldDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_REMOTE_SELECT -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormRemoteSelectDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormRemoteSelectDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        gen.printLine("%s.setClassName(\"%s\");".formatted(compName, ((FormRemoteSelectDescription) comp).getClassName()));
                        gen.printLine("%s.setAllowAddNewValue(%s);".formatted(compName, ((FormRemoteSelectDescription) comp).isAllowAddNewValue() ? "true" : "false"));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_REMOTE_MULTI_SELECT -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormRemoteMultiSelectDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormRemoteMultiSelectDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        gen.printLine("%s.setClassName(\"%s\");".formatted(compName, ((FormRemoteMultiSelectDescription) comp).getClassName()));
                        gen.printLine("%s.setAllowAddNewValue(%s);".formatted(compName, ((FormRemoteMultiSelectDescription) comp).isAllowAddNewValue() ? "true" : "false"));
                        gen.printLine("%s.setOrderEditable(%s);".formatted(compName, ((FormRemoteMultiSelectDescription) comp).isOrderEditable() ? "true" : "false"));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_SELECT -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormSelectDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormSelectDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_MULTI_SELECT -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormMultiSelectDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormMultiSelectDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_ENUM_MULTI_SELECT -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormEnumMultiSelectDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormEnumMultiSelectDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        var ed = (FormEnumMultiSelectDescription) comp;
                        if (ed.getClassName() != null) {
                            gen.printLine("%s.setClassName(\"%s\");".formatted(compName, ed.getClassName()));
                        }
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_CUSTOM -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormCustomElementDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormCustomElementDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.setClassName(\"%s\");".formatted(compName, ((FormCustomElementDescription) comp).getClassName()));
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_RICH_TEXT_EDITOR -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormRichTextEditorDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormRichTextEditorDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_LABEL_WITH_TOOLS -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormLabelWithToolsDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormLabelWithToolsDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        var l = (FormLabelWithToolsDescription)comp;
                        if(l.getStyle() != null){
                            gen.printLine("%s.setStyle(\"%s\");".formatted(compName, l.getStyle()));
                        }
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }
                case FORM_DIV -> {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.FormDivDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var %s = new FormDivDescription();".formatted(compName));
                        gen.printLine("%s.setId(\"%s\");".formatted(compName, comp.getId()));
                        JavaAdminUiCodeGenUtils.updateTitle(comp.getTitle(), compName, gen);
                        var fdd = (FormDivDescription) comp;
                        gen.wrapWithBlock("", () -> {
                            JavaAdminUiDivConfiguratorHelper.generateDescription(fdd.getContent(), "divContent", nestedCount + 1, gen);
                            gen.printLine("%s.setContent(divContent);".formatted(compName));
                        });
                        gen.printLine("%s.getComponents().put(%s.getId(), %s);".formatted(containerDescriptionName, compName, compName));
                    });
                }

            }
        }
    }

    public static void generateDescription(NestedFormDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {

        var runnable = new BuildRunnableWithException(){

            @Override
            public void run() throws Exception {
                    if(nestedCount == 0) {
                        gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.RootFormDescription");
                        gen.printLine("""
                        var %s = (RootFormDescription) registry.getContainers().computeIfAbsent("%s", className->{
                                        var ctr = new RootFormDescription();
                                        ctr.setClassName(className);
                                        return ctr;
                                    });""".formatted(containerDescriptionName, ((RootFormDescription) cd).getClassName()));
                    } else {
                        gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.form.NestedFormDescription");
                        gen.printLine("var %s = new NestedFormDescription();".formatted(containerDescriptionName));
                    }
                    JavaAdminUiCodeGenUtils.updateTitle(cd.getTitle(),containerDescriptionName, gen);
                    var compName = "comp"+nestedCount;
                    generateComponentsIntoContainer(cd.getComponents(), containerDescriptionName, compName, nestedCount, gen);
            }
        };
        if(nestedCount == 0) {
            gen.wrapWithBlock(null,runnable);
        } else {
            runnable.run();
        }
    }
}
