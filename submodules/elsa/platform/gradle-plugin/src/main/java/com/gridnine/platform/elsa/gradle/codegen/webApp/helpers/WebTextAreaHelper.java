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

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.codegen.common.WebCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.webApp.TextAreaWebElementDescription;

import java.io.File;
import java.io.IOException;

public class WebTextAreaHelper {
    public static void generateTextArea(TextAreaWebElementDescription descr, File destDir, String commonPackageName, String moduleName) throws IOException {
        var basicName = JavaCodeGeneratorUtils.getSimpleName(descr.getClassName());
        var skeletonName = "%sSkeleton".formatted(basicName);
        var skeletonImport = WebCodeGeneratorUtils.getImportName(descr.getClassName()+"Skeleton", commonPackageName, moduleName);
        var functionalComponentName = "%sFC".formatted(basicName);
        var componentName = "%sComponent".formatted(basicName);
        var result = """
                import { WebComponentWrapper } from '%s/src/common/wrapper';
                import { initStateSetters } from '%s/src/common/component';
                import { useEditor } from %s/src/common/editor";
                import { %s } from '%s';
                
                function %s(props: { element: %s }) {
                    initStateSetters(props.element);
                    const editor = useEditor();
                    return (
                        <WebComponentWrapper element={props.element}>
                            <textarea
                               value={props.element.getValue()?.value??''}
                               className={`webpeer-text-area${props.element.getValidationMessage() ? ' has-error' : ''}`}
                               onChange={(e) => {
                                        props.element.setValidationMessage(undefined);
                                        if(editor) {
                                           editor.setHasChanges(true);
                                        }
                                        props.element.setValue({
                                          value: e.target.value
                                        });
                                       }}
                            />
                        </WebComponentWrapper>
                    );
                }
                export class %s extends %s {
                    functionalComponent = %s;
                    setValidationMessage(value?: string) {
                        this.stateSetters.get('validationMessage')!(value);
                    }
                }
                """.formatted(moduleName, moduleName, moduleName, skeletonName, skeletonImport, functionalComponentName, componentName, componentName, skeletonName, functionalComponentName);
        var file = WebCodeGeneratorUtils.getFile(descr.getClassName() + ".tsx", destDir);
        if(!file.exists()) {
            WebCodeGeneratorUtils.saveIfDiffers(result, file);
        }
    }
}
