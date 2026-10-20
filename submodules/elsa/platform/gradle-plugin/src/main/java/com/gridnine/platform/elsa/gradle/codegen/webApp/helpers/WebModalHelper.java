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
import com.gridnine.platform.elsa.gradle.meta.webApp.ModalWebElementDescription;
import com.gridnine.platform.elsa.gradle.parser.webApp.WebAppMetadataHelper;

import java.io.File;

public class WebModalHelper {
    public static void generateModal(ModalWebElementDescription descr, File destDir, String commonPackageName, String moduleName) throws Exception {
        var ett = WebAppMetadataHelper.toCustomEntity(descr);
        var basicName = JavaCodeGeneratorUtils.getSimpleName(descr.getClassName());
        var skeletonName = "%sSkeleton".formatted(basicName);
        var skeletonImport = WebCodeGeneratorUtils.getImportName(descr.getClassName()+"Skeleton", commonPackageName, moduleName);
        var functionalComponentName = "%sFC".formatted(basicName);
        var componentName = "%sComponent".formatted(basicName);
        var result = """
        import { BaseReactUiElement, initStateSetters } from '%s/src/common/component';
        import { %s } from '%s';
        import { useState } from 'react';
        import { NotificationType } from '%s/src-gen/common/NotificationType'; %s
        import { NotificationFC } from '%s/src/common/notification';
        import { Modal } from '%s/src/common/modal';
        
                function %s(props: { element: %s }) {
                    initStateSetters(props.element);
                     const [isNotificationActive, setNotificationActive] = useState(false);
                     const [notificationMessage, setNotificationMessage] = useState('');
                     const [notificationType, setNotificationType] = useState<NotificationType>('INFO');
                     const showNotification = (message: string, type: NotificationType) => {
                            setNotificationMessage(message);
                            setNotificationType(type);
                            setNotificationActive(true);
                     };
                     props.element.processNotify = (notification) =>
                            showNotification(notification.message, notification.type);
        
                    return (
                        <>
                                                                  <NotificationFC
                                                                      message={notificationMessage}
                                                                      onClose={() => setNotificationActive(false)}
                                                                      isOpen={isNotificationActive}
                                                                      type={notificationType}
                                                                      duration={props.element.getNotificationDuration() ?? 1000}
                                                                  />
                                                                  <Modal
                                                                      isOpen={props.element.getVisible()}
                                                                      onClose={() => {
                                                                          props.element.sendClose();
                                                                      }}
                                                                      title={props.element.getTitle()}
                                                                      content={
                                                                          props.element.findByTag('content')?.children?.[0] as
                                                                              | BaseReactUiElement
                                                                              | undefined
                                                                      }
                                                                      buttons={props.element.findByTag('buttons')?.children as any}
                                                                  />
                                                              </>
                    );
                }
                export class %s extends %s {
                    functionalComponent = %s;
                    %s
                }
        """.formatted(moduleName, moduleName, moduleName, moduleName, skeletonName, skeletonImport,WebCommonHelper.getServerCommandImport(ett, commonPackageName, moduleName), functionalComponentName, componentName, componentName, skeletonName, functionalComponentName, WebCommonHelper.getServerCommandBlock(ett));
        var file = WebCodeGeneratorUtils.getFile(descr.getClassName() + ".tsx", destDir);
        if(!file.exists()) {
            WebCodeGeneratorUtils.saveIfDiffers(result, file);
        }
    }
}
