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

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.remoting.RemotingDescription;
import com.gridnine.platform.elsa.gradle.meta.remoting.RemotingMetaRegistry;

import java.io.File;
import java.util.Locale;
import java.util.Set;

public class JavaRemotingConstantsCodeGenerator {
    public static void generate(RemotingMetaRegistry registry, File destDir, String constantsClassName, Set<File> generatedFiles) throws Exception {
        RemotingDescription remoting = registry.getRemotings().values().iterator().next();
        var gen = new JavaCodeGenerator();
        gen.setPackageName(JavaCodeGeneratorUtils.getPackage(constantsClassName));
        gen.wrapWithBlock("public final class %s".formatted(JavaCodeGeneratorUtils.getSimpleName(constantsClassName)), () -> remoting.getGroups().values().forEach(group -> {
            group.getServices().keySet().forEach(sc -> {
                gen.blankLine();
                gen.printLine("public static final String %s = \"%s\";".formatted(getFieldName(remoting.getId(), group.getId(), sc),
                        getFieldValue(remoting.getId(), group.getId(), sc)));
            });
            group.getSubscriptions().keySet().forEach(sc -> {
                gen.blankLine();
                gen.printLine("public static final String %s = \"%s\";".formatted(getFieldName(remoting.getId(), group.getId(), sc),
                        getFieldValue(remoting.getId(), group.getId(), sc)));
            });

        }));
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), constantsClassName + ".java", destDir);
        generatedFiles.add(file);
    }

    private static String getFieldName(String remId, String groupId, String methodName) {
        return "%s_%s_%s".formatted(remId, groupId, methodName).replace(":", "_").replace("-", "_").replace(".", "_").toUpperCase(Locale.ROOT);
    }

    private static String getFieldValue(String remId, String groupId, String methodName) {
        return "%s:%s:%s".formatted(remId, groupId, methodName);
    }
}
