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

package com.gridnine.platform.elsa.gradle.codegen.webApp.helpers;

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.common.*;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.List;
import java.util.Set;

public class JavaWebAppEntityHelper {
    public static void generateJavaEntityCode(EntityDescription ed, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        var packageName = JavaCodeGeneratorUtils.getPackage(ed.getId());
        gen.setPackageName(packageName);
        var extendsId = ed.getExtendsId();
        var noSerialization = "true".equals(ed.getParameters().get("no-serialization"));
        String implementsId = null;
        if (extendsId == null || "Object".equals(extendsId)) {
            if(!noSerialization){
                gen.addImport("com.gridnine.webpeer.core.utils.GsonSerializable");
                gen.addImport("com.gridnine.platform.elsa.webApp.GsonDeserializable");
            }
            extendsId = null;
            implementsId = noSerialization? null: "GsonSerializable, GsonDeserializable";
        } else {
            gen.addImport(extendsId);
        }
        var cnsb = new StringBuilder();
        cnsb.append("public");
        if (extendsId != null) {
            cnsb.append(" class %s extends %s".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getId()),
                    JavaCodeGeneratorUtils.getSimpleName(extendsId)));
        } else if(implementsId != null){
            cnsb.append(" class %s implements %s".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getId()),
                    implementsId));
        } else {
            cnsb.append(" class %s".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getId())));
        }
        gen.wrapWithBlock(cnsb.toString(), () -> {
            for (StandardPropertyDescription pd : ed.getProperties().values()) {
                gen.blankLine();
                String className = JavaCodeGeneratorUtils.getPropertyType(pd.getType(), pd.getClassName(), pd.isNonNullable(), gen);
                gen.printLine("private %s %s;".formatted(className, pd.getId()));
            }
            for (StandardCollectionDescription cd : ed.getCollections().values()) {
                gen.blankLine();
                String className = JavaCodeGeneratorUtils.getPropertyType(cd.getElementType(), cd.getElementClassName(), false, gen);
                gen.addImport("java.util.*");
                gen.printLine("private final %s<%s> %s = new %s<>();".formatted(cd.isUnique() ? "Set" : "List", className, cd.getId(), cd.isUnique() ? "HashSet" : "ArrayList"));
            }
            for (StandardMapDescription md : ed.getMaps().values()) {
                gen.blankLine();
                gen.addImport("java.util.Map");
                gen.addImport("java.util.HashMap");
                String keyClassName = JavaCodeGeneratorUtils.getPropertyType(md.getKeyType(), md.getKeyClassName(), false, gen);
                String valueClassName = JavaCodeGeneratorUtils.getPropertyType(md.getValueType(), md.getValueClassName(), false, gen);
                gen.printLine("private final Map<%s,%s> %s = new HashMap<>();".formatted(keyClassName, valueClassName, md.getId()));
            }
            {
                for (StandardPropertyDescription pd : ed.getProperties().values()) {
                    gen.blankLine();
                    String className = JavaCodeGeneratorUtils.getPropertyType(pd.getType(), pd.getClassName(), pd.isNonNullable(), gen);
                    gen.wrapWithBlock("public %s %s%s()".formatted(className, pd.getType() == StandardValueType.BOOLEAN ? "is" : "get", BuildTextUtils.capitalize(pd.getId())), () -> gen.printLine("return %s;".formatted(pd.getId())));
                    gen.blankLine();
                    gen.wrapWithBlock("public void set%s(%s value)".formatted(BuildTextUtils.capitalize(pd.getId()), className), () -> gen.printLine("this.%s = value;".formatted(pd.getId())));
                }
                for (StandardCollectionDescription cd : ed.getCollections().values()) {
                    gen.blankLine();
                    gen.addImport("java.util.*");
                    String className = JavaCodeGeneratorUtils.getPropertyType(cd.getElementType(), cd.getElementClassName(), false, gen);
                    gen.wrapWithBlock("public %s<%s> get%s()".formatted(cd.isUnique() ? "Set" : "List", className, BuildTextUtils.capitalize(cd.getId())), () -> gen.printLine("return %s;".formatted(cd.getId())));
                }
                for (StandardMapDescription md : ed.getMaps().values()) {
                    gen.blankLine();
                    gen.addImport("java.util.Map");
                    gen.addImport("java.util.HashMap");
                    String keyClassName = JavaCodeGeneratorUtils.getPropertyType(md.getKeyType(), md.getKeyClassName(), false, gen);
                    String valueClassName = JavaCodeGeneratorUtils.getPropertyType(md.getValueType(), md.getValueClassName(), false, gen);
                    gen.wrapWithBlock("public Map<%s,%s> get%s()".formatted(keyClassName, valueClassName, BuildTextUtils.capitalize(md.getId())), () -> gen.printLine("return %s;".formatted(md.getId())));
                }
            }
            if(!"true".equals(ed.getParameters().get("no-equals"))) {
                {

                    gen.blankLine();
                    gen.printLine("@Override");
                    gen.wrapWithBlock("public boolean equals(Object other)", () -> {
                        gen.wrapWithBlock("if(!(other instanceof %s))".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getId())), () -> {
                            gen.printLine("return false;");
                        });
                        gen.printLine("var casted = (%s) other;".formatted(JavaCodeGeneratorUtils.getSimpleName(ed.getId())));
                        for (StandardPropertyDescription pd : ed.getProperties().values()) {
                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                            gen.wrapWithBlock("if(!WebAppUtils.equals(this.%s, casted.%s))".formatted(pd.getId(), pd.getId()), () -> {
                                gen.printLine("return false;");
                            });
                        }
                        for (var coll : ed.getCollections().values()) {
                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                            gen.wrapWithBlock("if(!WebAppUtils.equals(this.%s, casted.%s))".formatted(coll.getId(), coll.getId()), () -> {
                                gen.printLine("return false;");
                            });
                        }
                        for (var map : ed.getMaps().values()) {
                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                            gen.wrapWithBlock("if(!WebAppUtils.equals(this.%s, casted.%s))".formatted(map.getId(), map.getId()), () -> {
                                gen.printLine("return false;");
                            });
                        }
                        gen.printLine("return true;");
                    });
                }
                {
                    gen.blankLine();
                    gen.printLine("@Override");
                    gen.wrapWithBlock("public int hashCode()", () -> gen.printLine("return super.hashCode();"));
                }
            }
            if(!noSerialization) {
                {
                    gen.blankLine();
                    gen.printLine("@Override");
                    gen.addImport("com.google.gson.JsonElement");
                    gen.addImport("com.google.gson.JsonObject");
                    gen.wrapWithBlock("public JsonElement serialize() throws Exception", () -> {
                        gen.printLine("var obj = new JsonObject();");
                        for (StandardPropertyDescription pd : ed.getProperties().values()) {
                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                            if (pd.isNonNullable()) {
                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                gen.printLine("obj.add(\"%s\", WebAppUtils.toJsonValue(%s, StandardValueType.%s));".formatted(pd.getId(), pd.getId(), pd.getType().name()));
                            } else {
                                gen.wrapWithBlock("if(%s != null)".formatted(pd.getId()), () -> {
                                    gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                    gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                    gen.printLine("obj.add(\"%s\", WebAppUtils.toJsonValue(%s, StandardValueType.%s));".formatted(pd.getId(), pd.getId(), pd.getType().name()));
                                });
                            }
                        }
                        for (StandardCollectionDescription cd : ed.getCollections().values()) {
                            gen.wrapWithBlock("if(!%s.isEmpty())".formatted(cd.getId()), () -> {
                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                gen.addImport("com.google.gson.JsonArray");
                                gen.printLine("var coll = new JsonArray();");
                                gen.wrapWithBlock("for(var elm: %s)".formatted(cd.getId()), () -> {
                                    gen.printLine("coll.add(WebAppUtils.toJsonValue(elm,  StandardValueType.%s));".formatted(cd.getElementType().name()));
                                });
                                gen.printLine("obj.add(\"%s\", coll);".formatted(cd.getId()));
                            });
                        }
                        for (var md : ed.getMaps().values()) {
                            gen.wrapWithBlock("if(!%s.isEmpty())".formatted(md.getId()), () -> {
                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                gen.printLine("obj.add(\"%s\", WebAppUtils.toJsonValue(%s, StandardValueType.%s));".formatted(md.getId(), md.getId(), md.getValueType().name()));
                            });
                        }
                        gen.printLine("return obj;");
                    });
                    {
                        gen.blankLine();
                        gen.printLine("@Override");
                        gen.addImport("com.google.gson.JsonElement");
                        gen.addImport("com.google.gson.JsonObject");
                        gen.wrapWithBlock("public void deserialize(JsonElement element) throws Exception", () -> {
                            gen.printLine("var obj = element.getAsJsonObject();");
                            for (StandardPropertyDescription pd : ed.getProperties().values()) {
                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                gen.printLine("%s = WebAppUtils.fromJsonValue(obj.has(\"%s\")? obj.get(\"%s\"): null, StandardValueType.%s, %s.class);".formatted(pd.getId(), pd.getId(), pd.getId(), pd.getType().name(), JavaCodeGeneratorUtils.getPropertyType(pd.getType(), pd.getClassName(), false, gen)));
                            }
                            for (StandardCollectionDescription cd : ed.getCollections().values()) {
                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                gen.printLine("%s.clear();".formatted(cd.getId()));
                                gen.wrapWithBlock("if(obj.has(\"%s\"))".formatted(cd.getId()), () -> {
                                    gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                    gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                    gen.wrapWithBlock("for(var elm : obj.get(\"%s\").getAsJsonArray())".formatted(cd.getId()), () -> {
                                        gen.printLine("%s.add(WebAppUtils.fromJsonValue(elm, StandardValueType.%s, %s.class));".formatted(cd.getId(), cd.getElementType().name(), JavaCodeGeneratorUtils.getPropertyType(cd.getElementType(), cd.getElementClassName(), false, gen)));
                                    });
                                });
                            }
                        });
                    }
                }
            }
        });
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), ed.getId() + ".java", destDir);
        generatedFiles.add(file);
    }
}


