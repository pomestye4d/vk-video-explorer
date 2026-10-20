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
import com.gridnine.platform.elsa.gradle.meta.webApp.RouterWebElementDescription;

import java.io.File;
import java.io.IOException;

public class WebRouterHelper {
    public static void generateRouter(RouterWebElementDescription descr, File destDir, String commonPackageName, String moduleName) throws IOException {
        var basicName = JavaCodeGeneratorUtils.getSimpleName(descr.getClassName());
        var skeletonName = "%sSkeleton".formatted(basicName);
        var skeletonImport = WebCodeGeneratorUtils.getImportName(descr.getClassName() + "Skeleton", commonPackageName, moduleName);
        var functionalComponentName = "%sFC".formatted(basicName);
        var componentName = "%sComponent".formatted(basicName);
        var result = """
                import { WebComponentWrapper } from '%s/src/common/wrapper';
                import { RouterContext } from '%s/src/common/router';
                import {useEffect, useState} from "react";
                import { %s } from '%s';
                
                function %s(props: { element: %s }) {
                    for (const prop of props.element.state.keys()) {
                        const [value, setValue] = useState(props.element.state.get(prop));
                        props.element.state.set(prop, value);
                         props.element.stateSetters.set(prop, setValue);
                    }
                    function handlePopEvent() {
                         const currentPath = props.element.state.get('path') as string;
                         const hasChanges = props.element.state.get('hasChanges') as boolean;
                         const message = props.element.state.get('confirmMessage') as string;
                         if (hasChanges) {
                            if (!window.confirm(message)) {
                                 window.history.pushState(null, '', currentPath);
                                 return;
                             }
                         }
                         props.element.navigate({ path: window.location.pathname, force: true });
                    }
                    useEffect(() => {
                        props.element.state.forEach((value, key) => {
                            props.element.stateSetters.get(key)?.(value);
                        });
                        window.addEventListener('popstate', handlePopEvent);
                        return () => {
                            window.removeEventListener('popstate', handlePopEvent);
                        };
                    }, [props.element]);
                    return (
                      <RouterContext.Provider
                                  value={{
                                      hasChanges: !!props.element.getHasChanges(),
                                      setHasChanges: (value) => props.element.setHasChanges(value),
                                      confirmMessage: props.element.getConfirmMessage(),
                                      setConfirmMessage: (message) =>
                                          props.element.setConfirmMessage(message),
                                  }}
                              >
                        <WebComponentWrapper element={props.element}>
                             <div className="webpeer-container-content"
                                 key="content"
                                 style={{
                                   display: 'flex',
                                   flexDirection: 'column'
                                 }}
                             >
                               {props.element.findByTag('content').createReactElement()}
                             </div>
                        </WebComponentWrapper>
                        </RouterContext.Provider>
                    );
                }
                
                export class %s extends %s {
                    functionalComponent = %s;
                
                    navigate(value: { path: string; force: boolean }) {
                        this.sendCommand('navigate', value, false);
                    }
                
                    protected updatePropertyValue(pn: string, pv: any) {
                        super.updatePropertyValue(pn, pv);
                        if (pn === 'path') {
                           window.history.pushState(null, '', pv);
                           return;
                       }
                    }
                
                    setHasChanges(value: boolean) {
                       this.state.set('hasChanges', value);
                       this.stateSetters.get('hasChanges')!(value);
                       this.sendPropertyChange('hasChanges', value, true);
                    }
                
                    setConfirmMessage(value?: string) {
                       this.state.set('confirmMessage', value);
                       this.stateSetters.get('confirmMessage')!(value);
                       this.sendPropertyChange('confirmMessage', value, true);
                    }
                }
                """.formatted(moduleName, moduleName, skeletonName, skeletonImport, functionalComponentName, componentName, componentName, skeletonName, functionalComponentName);
        var file = WebCodeGeneratorUtils.getFile(descr.getClassName() + ".tsx", destDir);
        if (!file.exists()) {
            WebCodeGeneratorUtils.saveIfDiffers(result, file);
        }
    }
}
