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
import com.gridnine.platform.elsa.gradle.meta.webApp.TableWebElementDescription;

import java.io.File;
import java.io.IOException;

public class WebTableHelper {
    public static void generateTable(TableWebElementDescription descr, File destDir, String commonPackageName, String moduleName) throws IOException {
        var basicName = JavaCodeGeneratorUtils.getSimpleName(descr.getClassName());
        var skeletonName = "%sSkeleton".formatted(basicName);
        var skeletonImport = WebCodeGeneratorUtils.getImportName(descr.getClassName()+"Skeleton", commonPackageName, moduleName);
        var functionalComponentName = "%sFC".formatted(basicName);
        var componentName = "%sComponent".formatted(basicName);
        var result = """
                import { WebComponentWrapper } from '%s/src/common/wrapper';
                import { initStateSetters } from '%s/src/common/component';
                import { EntityListColumnDescription } from '%s/src-gen/common/EntityListColumnDescription';
                import { Option } from '%s/src-gen/common/Option';
                import { %s } from '%s';
                
                function %s(props: { element: %s }) {
                    initStateSetters(props.element);
                    const getSortIcon = (columnKey: string) => {
                            if (props.element.getSort()?.field !== columnKey) {
                                return '↕️';
                            }
                            return props.element.getSort()?.sortOrder === 'ASC' ? '↑' : '↓';
                        };
                    const renderCellContent = (rowId: string, value: any, c: EntityListColumnDescription) => {
                            switch (c.columnType) {
                                case 'TEXT': {
                                    return value?.toString();
                                }
                                case 'OPTION': {
                                    return value?.toString();
                                }
                                case 'MENU': {
                                    const menu = value as Option[];
                                    return (
                                        <select
                                            value="_select"
                                            onChange={(ev) => {
                                                props.element.sendAction({
                                                    rowId,
                                                    actionId: ev.target.value,
                                                    columnId: c.id,
                                                });
                                            }}
                                        >
                                            <option key="select-action" value="_select">
                                                ...
                                            </option>
                                            {menu.map((m) => (
                                                <option key={m.id} value={m.id}>
                                                    {m.displayName}
                                                </option>
                                            ))}
                                        </select>
                                    );
                                }
                                case 'CUSTOM': {
                                    return value?.toString();
                                }
                            }
                        };
                    return (
                        <WebComponentWrapper element={props.element}>
                            <table className="webpeer-table">
                              <thead>
                                <tr>
                                   {props.element.getColumns().map((c) => (
                                      <th
                                        key={c.id}
                                        onClick={() => {
                                             if (!c.sortable) {
                                                return;
                                             }
                                             if (props.element.getSort()?.field === c.id) {
                                                  const newSort = { ...props.element.getSort()! };
                                                  newSort.sortOrder = newSort.sortOrder == 'ASC' ? 'DESC' : 'ASC';
                                                  props.element.sendSort({ sort: newSort });
                                                  return;
                                             }
                                             props.element.sendSort({
                                                   sort: {
                                                     field: c.id,
                                                     sortOrder: 'ASC',
                                                   }
                                             });
                                        }}
                                        >
                                           {c.title} {c.sortable ? getSortIcon(c.id) : ''}
                                            </th>
                                        ))}
                                         </tr>
                                        </thead>
                                             <tbody>
                                                {props.element.getLoading() ? (
                                                 <tr>
                                                      <td colSpan={props.element.getColumns().length}>Loading</td>
                                                      </tr>
                                                  ) : (
                                                      props.element.getData().map((entry) => (
                                                          <tr key={entry.id}>
                                                              {props.element.getColumns().map((c) => (
                                                                  <td key={c.id}>
                                                                      {renderCellContent(
                                                                          entry.id,
                                                                          (entry as any)[c.id],
                                                                          c
                                                                      )}
                                                                  </td>
                                                              ))}
                                                          </tr>
                                                      ))
                                                  )}
                                              </tbody>
                                          </table>
                        </WebComponentWrapper>
                    );
                }
                export class %s extends %s {
                    functionalComponent = %s;
                
                    processRefreshData(){
                       this.sendRefreshData();
                    }
                }
                """.formatted(moduleName, moduleName, moduleName, moduleName, skeletonName, skeletonImport, functionalComponentName, componentName, componentName, skeletonName, functionalComponentName);
        var file = WebCodeGeneratorUtils.getFile(descr.getClassName() + ".tsx", destDir);
        if(!file.exists()) {
            WebCodeGeneratorUtils.saveIfDiffers(result, file);
        }
    }
}
