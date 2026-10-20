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

package com.gridnine.platform.elsa.gradle.plugin;

import com.gridnine.platform.elsa.gradle.codegen.common.HasCodeGenType;
import org.gradle.api.Action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ElsaJavaExtension {

    private final List<HasCodeGenType> codegenRecords = new ArrayList<>();

    private File projectDir;

    private ElsaGlobalData globalData;

    public void codegen(Action<ElsaJavaCodeGenExtension> action) {
        var ext = new ElsaJavaCodeGenExtension(codegenRecords, projectDir, globalData);
        action.execute(ext);
    }

    public List<HasCodeGenType> getCodegenRecords() {
        return codegenRecords;
    }

    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }

    public void setGlobalData(ElsaGlobalData globalData) {
        this.globalData = globalData;
    }

    public ElsaGlobalData getGlobalData() {
        return globalData;
    }
}
