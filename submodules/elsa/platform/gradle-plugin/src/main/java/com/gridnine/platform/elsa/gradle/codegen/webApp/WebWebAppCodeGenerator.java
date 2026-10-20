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

package com.gridnine.platform.elsa.gradle.codegen.webApp;

import com.gridnine.platform.elsa.gradle.codegen.common.CodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.TypeScriptCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.WebCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.codegen.webApp.helpers.*;
import com.gridnine.platform.elsa.gradle.meta.common.EntityDescription;
import com.gridnine.platform.elsa.gradle.meta.webApp.CustomWebElementDescription;
import com.gridnine.platform.elsa.gradle.meta.webApp.TableWebElementDescription;
import com.gridnine.platform.elsa.gradle.meta.webApp.WebAppMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.webApp.WebAppMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.parser.webApp.WebAppMetadataHelper;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class WebWebAppCodeGenerator implements CodeGenerator<WebWebAppCodeGenRecord> {

    @Override
    public void generate(WebWebAppCodeGenRecord record, File destDir, Set<File> generatedFiles, Map<Object, Object> context) throws Exception {
        var parser = new WebAppMetaRegistryParser();
        var coll = new ArrayList<>(record.getSources());
        var metaRegistry = new WebAppMetaRegistry();
        parser.updateMetaRegistry(metaRegistry, record.isSkipCommonClasses(), coll);
        if(!record.isSkipCommonClasses()) {
            WebWebCommonClassesHelper.generate(record.getSourceDir());
        }
        for(var item: metaRegistry.getEnums().values()) {
            var gen = new TypeScriptCodeGenerator();
            WebCodeGeneratorUtils.generateWebEnumCode(item, gen);
            var file = WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), WebCodeGeneratorUtils.getFile(item.getId() + ".ts", destDir, record.getCommonPackageName()));
            generatedFiles.add(file);
        }
        for(var item: metaRegistry.getEntities().values()) {
            var gen = new TypeScriptCodeGenerator();
            WebCodeGeneratorUtils.generateWebEntityCode(item, null, gen, record.getCommonPackageName(), record.getModuleName());
            var file = WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), WebCodeGeneratorUtils.getFile(item.getId() + ".ts", destDir, record.getCommonPackageName()));
            generatedFiles.add(file);
        }
        for(var elm: metaRegistry.getElements().values()){
            CustomWebElementDescription ce = WebAppMetadataHelper.toCustomEntity(elm);
            if(ce.getInput() != null){
                var id = WebAppMetadataHelper.getInputValueDescription(ce);
                if(id.getProperties().size()+id.getCollections().size()>1) {
                    var gen = new TypeScriptCodeGenerator();
                    WebCodeGeneratorUtils.generateWebEntityCode(id, null, gen, record.getCommonPackageName(), record.getModuleName());
                    var file = WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), WebCodeGeneratorUtils.getFile(id.getId() + ".ts", destDir, record.getCommonPackageName()));
                    generatedFiles.add(file);
                }
            }
            for(var action: ce.getCommandsFromClient().values()){
                if(action.getProperties().size()+action.getCollections().size()>0){
                    var dd = new EntityDescription();
                    dd.setId("%s%sAction".formatted(elm.getClassName(), BuildTextUtils.capitalize(action.getId())));
                    dd.getProperties().putAll(action.getProperties());
                    dd.getCollections().putAll(action.getCollections());
                    var gen = new TypeScriptCodeGenerator();
                    WebCodeGeneratorUtils.generateWebEntityCode(dd, null, gen, record.getCommonPackageName(), record.getModuleName());
                    var file = WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), WebCodeGeneratorUtils.getFile(dd.getId() + ".ts", destDir, record.getCommonPackageName()));
                    generatedFiles.add(file);
                }
            }
            for(var action: ce.getCommandsFromServer().values()){
                if(action.getProperties().size()+action.getCollections().size()>0){
                    var dd = new EntityDescription();
                    dd.setId("%s%sAction".formatted(elm.getClassName(), BuildTextUtils.capitalize(action.getId())));
                    dd.getProperties().putAll(action.getProperties());
                    dd.getCollections().putAll(action.getCollections());
                    var gen = new TypeScriptCodeGenerator();
                    WebCodeGeneratorUtils.generateWebEntityCode(dd, null, gen, record.getCommonPackageName(), record.getModuleName());
                    var file = WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), WebCodeGeneratorUtils.getFile(dd.getId() + ".ts", destDir, record.getCommonPackageName()));
                    generatedFiles.add(file);
                }
            }
            for(var ett: WebAppMetadataHelper.getServicesClasses(elm)){
                var gen = new TypeScriptCodeGenerator();
                WebCodeGeneratorUtils.generateWebEntityCode(ett, null, gen, record.getCommonPackageName(), record.getModuleName());
                var file = WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), WebCodeGeneratorUtils.getFile(ett.getId() + ".ts", destDir, record.getCommonPackageName()));
                generatedFiles.add(file);
            }

            if(elm instanceof TableWebElementDescription td){
                var dd = new EntityDescription();
                dd.setId("%sRow".formatted(td.getClassName()));
                dd.getProperties().putAll(td.getRow().getProperties());
                dd.getCollections().putAll(td.getRow().getCollections());
                var gen = new TypeScriptCodeGenerator();
                WebCodeGeneratorUtils.generateWebEntityCode(dd, null, gen, record.getCommonPackageName(), record.getModuleName());
                var file = WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), WebCodeGeneratorUtils.getFile(dd.getId() + ".ts", destDir, record.getCommonPackageName()));
                generatedFiles.add(file);
            }
        }
        WebWebAppElementsHelper.generate(metaRegistry, destDir, record.getSourceDir(), record.getCommonPackageName(), record.getModuleName(), generatedFiles);
        WebWebRegistryHelper.generate(metaRegistry, destDir, generatedFiles, record.getConfigurator(), record.getCommonPackageName(),record.getModuleName());
    }
}
