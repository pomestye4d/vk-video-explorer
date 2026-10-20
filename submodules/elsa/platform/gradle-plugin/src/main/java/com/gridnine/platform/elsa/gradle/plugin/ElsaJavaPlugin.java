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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.LinkedHashMap;
import java.util.Map;

public class ElsaJavaPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        var globalPropertyName = "elsa-global-data";
        var props = target.getRootProject().getExtensions().getExtraProperties();
        Map<String, Object> map;
        if(props.has(globalPropertyName)){
            map = (Map<String, Object>) props.get(globalPropertyName);
        } else {
            map = new LinkedHashMap<>();
            props.set(globalPropertyName, map);
        }
        var globalData = new ElsaGlobalData(map);
        var ext = target.getExtensions().create("elsa", ElsaJavaExtension.class);
        ext.setProjectDir(target.getProjectDir());
        ext.setGlobalData(globalData);
        target.getTasks().create("eCodeGen", ElsaCodeGenTask.class);
    }
}
