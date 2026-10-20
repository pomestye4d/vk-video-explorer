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
import com.gridnine.platform.elsa.gradle.meta.webApp.BaseWebElementDescription;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

public class WebCommonHelper {
    public static String getServerCommandImport(BaseWebElementDescription descr, String commonPackageName, String moduleName) throws Exception {
        if(descr.getCommandsFromServer().isEmpty()){
            return "";
        }
        var bldr = new StringBuilder();
        for(var cmd: descr.getCommandsFromServer().values()){
            if(cmd.getProperties().isEmpty() && cmd.getCollections().isEmpty()){
                continue;
            }
            bldr.append("\n\r");
            var actionClassName = "%s%sAction".formatted(descr.getClassName(),BuildTextUtils.capitalize(cmd.getId()));
            var cls = JavaCodeGeneratorUtils.getSimpleName(actionClassName);
            var imp = WebCodeGeneratorUtils.getImportName(actionClassName,commonPackageName, moduleName);
            bldr.append("import { %s } from '%s'".formatted(cls, imp));
        }
        return bldr.toString();
    }
    public static String getServerCommandBlock(BaseWebElementDescription descr) throws Exception {
        if(descr.getCommandsFromServer().isEmpty()){
            return "";
        }
        var bldr = new StringBuilder();
        for(var cmd: descr.getCommandsFromServer().values()){
            if(!bldr.isEmpty()){
                bldr.append("\r\n");
            }
            if(cmd.getProperties().isEmpty() && cmd.getCollections().isEmpty()){
                bldr.append("""
                        process%s(){
                        }""".formatted(BuildTextUtils.capitalize(cmd.getId())));
            } else {
                bldr.append("""
                        process%s(_value: %s%sAction){
                        }""".formatted(BuildTextUtils.capitalize(cmd.getId())
                        , JavaCodeGeneratorUtils.getSimpleName(descr.getClassName()),BuildTextUtils.capitalize(cmd.getId())));
            }
        }
        return bldr.toString();
    }
}
