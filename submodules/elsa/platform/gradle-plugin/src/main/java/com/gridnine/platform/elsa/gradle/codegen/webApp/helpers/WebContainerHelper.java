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
import com.gridnine.platform.elsa.gradle.meta.webApp.ContainerWebElementDescription;

import java.io.File;
import java.io.IOException;
public class WebContainerHelper {
    public static void generateContainer(ContainerWebElementDescription descr, File destDir, String commonPackageName, String moduleName) throws Exception {
        var basicName = JavaCodeGeneratorUtils.getSimpleName(descr.getClassName());
        var skeletonName = "%sSkeleton".formatted(basicName);
        var skeletonImport = WebCodeGeneratorUtils.getImportName(descr.getClassName()+"Skeleton", commonPackageName, moduleName);
        var functionalComponentName = "%sFC".formatted(basicName);
        var componentName = "%sComponent".formatted(basicName);
        var result = """
                import { WebComponentWrapper } from '%s/src/common/wrapper';
                import { BaseReactUiElement, initStateSetters } from '%s/src/common/component';
                import { %s } from '%s';
                
                function %s(props: { element: %s }) {
                    initStateSetters(props.element);
                    return (
                        <WebComponentWrapper element={props.element}>
                            <div
                                className="webpeer-container-content"
                                key="content"
                                style={{
                                    display: 'flex',
                                    flexDirection:
                                        props.element.getFlexDirection() === 'ROW' ? 'row' : 'column',
                                }}
                            >
                                {(props.element.children || []).map((it) => {
                                    if (props.element.getProcessedChildren().indexOf(it.id) === -1) {
                                        return (it as BaseReactUiElement).createReactElement();
                                    }
                                    return null;
                                })}
                            </div>
                        </WebComponentWrapper>
                    );
                }
                
                export class %s extends %s {
                    functionalComponent = %s;
                    getProcessedChildren(): string[] {
                        return [];
                    }
                    %s
                }
                """.formatted(moduleName,moduleName,skeletonName, skeletonImport, functionalComponentName, componentName, componentName, skeletonName, functionalComponentName, WebCommonHelper.getServerCommandBlock(descr));
        var file = WebCodeGeneratorUtils.getFile(descr.getClassName() + ".tsx", destDir);
        if(!file.exists()) {
            WebCodeGeneratorUtils.saveIfDiffers(result, file);
        }
    }
}
