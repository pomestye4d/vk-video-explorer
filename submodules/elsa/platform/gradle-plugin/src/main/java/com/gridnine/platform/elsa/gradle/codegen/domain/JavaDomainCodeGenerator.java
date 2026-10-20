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

package com.gridnine.platform.elsa.gradle.codegen.domain;

import com.gridnine.platform.elsa.gradle.codegen.common.CodeGenerator;
import com.gridnine.platform.elsa.gradle.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.domain.DomainMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.plugin.ElsaGlobalData;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class JavaDomainCodeGenerator implements CodeGenerator<JavaDomainCodeGenRecord> {
    @Override
    public void generate(JavaDomainCodeGenRecord record, File destDir, Set<File> generatedFiles, Map<Object, Object> context) throws IOException {
        var parser = new DomainMetaRegistryParser();
        String dir = record.getDestinationDir().getCanonicalPath();
        ElsaGlobalData elsaGlobalData = (ElsaGlobalData) context.get("elsa-global-data");
        if(!record.getSources().isEmpty()){
            var coll = new ArrayList<>(record.getSources());
            if(elsaGlobalData.getDomainInjections().containsKey(dir)){
                coll.addAll(elsaGlobalData.getDomainInjections().get(dir));
            }
            var metaRegistry = new DomainMetaRegistry();
            parser.updateMetaRegistry(metaRegistry, coll);
            BuildExceptionUtils.wrapException(() -> JavaDomainEntitiesCodeGenerator.generate(metaRegistry, destDir, generatedFiles, context));
            BuildExceptionUtils.wrapException(() -> JavaDomainFieldsClassCodeGenerator.generate(metaRegistry, destDir, generatedFiles, context));
        }
        {
            var coll = new ArrayList<>(record.getSources());
            coll.addAll(record.getLocalInjections());
            var metaRegistry = new DomainMetaRegistry();
            parser.updateMetaRegistry(metaRegistry, coll);
            BuildExceptionUtils.wrapException(() -> JavaDomainConfiguratorCodeGenerator.generate(metaRegistry, record.getRegistryConfigurator(), destDir, generatedFiles));
        }
    }
}
