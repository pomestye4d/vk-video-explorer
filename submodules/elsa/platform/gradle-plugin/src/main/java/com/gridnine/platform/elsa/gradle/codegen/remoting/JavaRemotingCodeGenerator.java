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

package com.gridnine.platform.elsa.gradle.codegen.remoting;

import com.gridnine.platform.elsa.gradle.codegen.common.CodeGenerator;
import com.gridnine.platform.elsa.gradle.meta.remoting.RemotingMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.remoting.RemotingMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.plugin.ElsaGlobalData;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class JavaRemotingCodeGenerator implements CodeGenerator<JavaRemotingCodeGenRecord> {
    @Override
    public void generate(JavaRemotingCodeGenRecord record, File destDir, Set<File> generatedFiles, Map<Object, Object> context) throws IOException {
        var parser = new RemotingMetaRegistryParser();
        String dir = record.getDestinationDir().getCanonicalPath();
        ElsaGlobalData elsaGlobalData = (ElsaGlobalData) context.get("elsa-global-data");
        {
            var coll = new ArrayList<>(record.getSources());
            if(elsaGlobalData.getRemotingInjections().containsKey(dir)){
                coll.addAll(elsaGlobalData.getRemotingInjections().get(dir));
            }
            var metaRegistry = new RemotingMetaRegistry();
            parser.updateMetaRegistry(metaRegistry, coll);
            if (!record.isNoModelClasses()) {
                BuildExceptionUtils.wrapException(() -> JavaRemotingEntitiesCodeGenerator.generate(metaRegistry, destDir, generatedFiles));
            }
        }
        {
            var coll = new ArrayList<>(record.getSources());
            coll.addAll(record.getLocalInjections());
            var metaRegistry = new RemotingMetaRegistry();
            parser.updateMetaRegistry(metaRegistry, coll);
            if (record.getRegistryConfigurator() != null) {
                BuildExceptionUtils.wrapException(() -> JavaRemotingConfiguratorCodeGenerator.generate(metaRegistry, record.getRegistryConfigurator(), destDir, generatedFiles));
            }
            if (record.getConstants() != null) {
                BuildExceptionUtils.wrapException(() -> JavaRemotingConstantsCodeGenerator.generate(metaRegistry, destDir, record.getConstants(), generatedFiles));
            }
        }
    }
}
