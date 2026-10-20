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

package com.gridnine.platform.elsa.gradle.codegen.common;

import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;
import com.gridnine.platform.elsa.gradle.utils.BuildRunnableWithException;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.util.HashSet;
import java.util.Set;

public class JavaCodeGenerator {
    private final StringBuffer buf = new StringBuffer();

    private final Set<String> imports = new HashSet<>();

    private String packageName;

    private int indent;

    public void printLine(String line) {
        if (!buf.isEmpty()) {
            buf.append("\n");
        }
        indent(buf);
        buf.append(line);
    }

    public void wrapWithBlock(String name, BuildRunnableWithException runnable) {
        wrapWithBlock(name, "{", "}", runnable);
    }

    public void wrapWithBlock(String name, String blockStartCharacter, String blockEndCharacter, BuildRunnableWithException runnable) {
        printLine(name == null ? blockStartCharacter : "%s%s".formatted(name, blockStartCharacter));
        indent++;
        try {
            BuildExceptionUtils.wrapException(runnable);
        } finally {
            indent--;
            printLine(blockEndCharacter);
        }
    }

    private void indent(StringBuffer buf) {
        buf.append("\t".repeat(Math.max(0, indent)));
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void blankLine() {
        buf.append("\n");
    }

    public void addImport(String className) {
        imports.add(className);
    }

    public String toString() {
        var sb = new StringBuilder();
        sb.append("""
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
                 *
                 *****************************************************************
                 * This is generated code, don't modify it manually
                 *****************************************************************/
                 
                """);
        sb.append("package %s;\n\n".formatted(packageName));
        if (!imports.isEmpty()) {
            sb.append(BuildTextUtils.joinToString(imports.stream().map("import %s;\n"::formatted).sorted().toList(), ""));
            sb.append("\n");
        }
        return sb + buf.toString();
    }
}
