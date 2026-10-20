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

import com.gridnine.platform.elsa.gradle.utils.BuildRunnableWithException;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.util.HashSet;
import java.util.Set;

public class TypeScriptCodeGenerator {
    private final StringBuffer buf = new StringBuffer();

    private int indent;

    private final Set<String> imports = new HashSet<>();

    public void print(String line) {
        indent(buf);
        buf.append(line);
    }

    public void printLine(String line) {
        if (!buf.isEmpty()) {
            buf.append("\n");
        }
        indent(buf);
        buf.append(line);
    }

    public void addImport(String importName) {
        imports.add(importName);
    }
    public void wrapWithBlock(String name, BuildRunnableWithException runnable) throws Exception {
        printLine(name == null ? "{" : "%s{".formatted(name));
        indent++;
        try {
            runnable.run();
        } finally {
            indent--;
            printLine("}");
        }
    }

    private void indent(StringBuffer buf) {
        buf.append("  ".repeat(Math.max(0, indent)));
    }

    public void blankLine() {
        buf.append("\n");
    }

    public String toString() {
        String sb = """
                /* ****************************************************************
                 * This is generated code, don't modify it manually
                 **************************************************************** */
                 
                 %s
                 """.formatted(imports.isEmpty()? "": "\n%s\n".formatted(BuildTextUtils.joinToString(imports.stream().map("import %s;\n"::formatted).sorted().toList(), "")));
        return sb + buf;
    }
}
