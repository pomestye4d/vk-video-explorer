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

import com.gridnine.platform.elsa.gradle.codegen.adminUi.JavaAdminUiCodeGenRecord;
import com.gridnine.platform.elsa.gradle.codegen.common.HasCodeGenType;
import com.gridnine.platform.elsa.gradle.codegen.custom.JavaCustomCodeGenRecord;
import com.gridnine.platform.elsa.gradle.codegen.domain.JavaDomainCodeGenRecord;
import com.gridnine.platform.elsa.gradle.codegen.l10n.JavaL10nCodeGenRecord;
import com.gridnine.platform.elsa.gradle.codegen.remoting.JavaRemotingCodeGenRecord;
import com.gridnine.platform.elsa.gradle.codegen.remoting.OpenApiCodeGenRecord;
import com.gridnine.platform.elsa.gradle.codegen.webApp.JavaWebAppCodeGenRecord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ElsaJavaCodeGenExtension {

    private String artefactId;

    public String getArtefactId() {
        return artefactId;
    }

    public void setArtefactId(String artefactId) {
        this.artefactId = artefactId;
    }

    private final List<HasCodeGenType> codegenRecords;

    private final ElsaGlobalData globalData;

    private final File projectDir;


    public ElsaJavaCodeGenExtension(List<HasCodeGenType> codegenRecords, File projectDir, ElsaGlobalData globalData) {
        this.codegenRecords = codegenRecords;
        this.projectDir = projectDir;
        this.globalData = globalData;
    }

    public void domain(String destDir, String configurator, List<String> sourcesFileNames) throws IOException {
        var record = new JavaDomainCodeGenRecord();
        record.setRegistryConfigurator(configurator);
        sourcesFileNames.forEach(it -> record.getSources().add(new File(projectDir, it)));
        record.setDestinationDir(new File(projectDir, destDir));
        globalData.getDomainRecords().put(record.getDestinationDir().getCanonicalPath(), record);
        globalData.setCurrentDomainRecord(record);
        codegenRecords.add(record);
    }

    public void domainInjection(String destDir,  List<String> sourcesFileNames) throws Exception {
        List<File> files = sourcesFileNames.stream().map(it -> new File(projectDir, it)).toList();
        if(globalData.getCurrentDomainRecord() != null) {
            globalData.getCurrentDomainRecord().getLocalInjections().addAll(files);
        }
        globalData.getDomainInjections().put(new File(projectDir, destDir).getCanonicalPath(), files);
    }

    public void custom(String destDir, String configurator, List<String> sourcesFileNames) {
        var record = new JavaCustomCodeGenRecord();
        record.setRegistryConfigurator(configurator);
        sourcesFileNames.forEach(it -> record.getSources().add(new File(projectDir, it)));
        record.setDestinationDir(new File(projectDir, destDir));
        codegenRecords.add(record);
    }

    public void remoting(String destDir, String configurator, List<String> sourcesFileNames) throws IOException {
        var record = new JavaRemotingCodeGenRecord();
        record.setRegistryConfigurator(configurator);
        sourcesFileNames.forEach(it -> record.getSources().add(new File(projectDir, it)));
        record.setDestinationDir(new File(projectDir, destDir));
        globalData.getRemotingRecords().computeIfAbsent(record.getDestinationDir().getCanonicalPath(), (i) -> new ArrayList<>()).add(record);
        globalData.setCurrentRemotingRecord(record);
        codegenRecords.add(record);
    }

    public void adminUi(String destDir, String configurator, List<String> sourcesFileNames) throws IOException {
        var record = new JavaAdminUiCodeGenRecord();
        record.setRegistryConfigurator(configurator);
        sourcesFileNames.forEach(it -> record.getSources().add(new File(projectDir, it)));
        record.setDestinationDir(new File(projectDir, destDir));
        codegenRecords.add(record);
    }

    public void remotingInjection(String destDir,  List<String> sourcesFileNames) throws IOException {
        List<File> files = sourcesFileNames.stream().map(it -> new File(projectDir, it)).toList();
        if(globalData.getCurrentRemotingRecord() != null) {
            globalData.getCurrentRemotingRecord().getLocalInjections().addAll(files);
        }
        globalData.getDomainInjections().put(new File(projectDir, destDir).getCanonicalPath(), files);
    }

    public void remoting(String destDir, String configurator,  String constants, boolean noModelClasses, List<String> sourcesFileNames) throws IOException {
        var record = new JavaRemotingCodeGenRecord();
        record.setRegistryConfigurator(configurator);
        record.setConstants(constants);
        record.setNoModelClasses(noModelClasses);
        sourcesFileNames.forEach(it -> record.getSources().add(new File(projectDir, it)));
        record.setDestinationDir(new File(projectDir, destDir));
        globalData.getRemotingRecords().computeIfAbsent(record.getDestinationDir().getCanonicalPath(), (i) -> new ArrayList<>()).add(record);
        globalData.setCurrentRemotingRecord(record);
        codegenRecords.add(record);
    }

    public void webApp(String destDir, String sourceDir, String configurator,  boolean skipCommonClasses, List<String> sourcesFileNames) throws IOException {
        var record = new JavaWebAppCodeGenRecord();
        record.setRegistryConfigurator(configurator);
        record.setSkipCommonClasses(skipCommonClasses);
        sourcesFileNames.forEach(it -> record.getSources().add(new File(projectDir, it)));
        record.setDestinationDir(new File(projectDir, destDir));
        record.setSourceDir(new File(projectDir, sourceDir));
        codegenRecords.add(record);
    }

    public void openApi(String yaml, String... restIds) throws IOException {
        var file = new File(projectDir, yaml);
        globalData.getOpenApiRecords().computeIfAbsent(file.getCanonicalPath(), (i)-> {
            OpenApiCodeGenRecord record = new OpenApiCodeGenRecord();
            record.setYamlFile(file);
            codegenRecords.add(record);
            return record;
        }).getRestIds().addAll(List.of(restIds));
    }

    public void openApiInjection(String yaml, String... restIds) throws IOException {
         openApi(yaml, restIds);
    }

    public void l10n(String destDir, String configurator, String factory, List<String> sourcesFileNames) {
        var record = new JavaL10nCodeGenRecord();
        record.setRegistryConfigurator(configurator);
        sourcesFileNames.forEach(it -> record.getSources().add(new File(projectDir, it)));
        record.setFactory(factory);
        record.setDestinationDir(new File(projectDir, destDir));
        codegenRecords.add(record);
    }

}
