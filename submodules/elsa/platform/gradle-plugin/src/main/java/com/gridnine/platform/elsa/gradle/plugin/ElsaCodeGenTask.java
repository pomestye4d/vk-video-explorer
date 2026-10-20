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

import com.gridnine.platform.elsa.gradle.codegen.adminUi.JavaAdminUiCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.BaseCodeGenRecord;
import com.gridnine.platform.elsa.gradle.codegen.common.CodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.GeneratorType;
import com.gridnine.platform.elsa.gradle.codegen.common.HasCodeGenType;
import com.gridnine.platform.elsa.gradle.codegen.custom.JavaCustomCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.domain.JavaDomainCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.l10n.JavaL10nCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.l10n.WebL10nCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.remoting.JavaRemotingCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.remoting.OpenApiCodeGenRecord;
import com.gridnine.platform.elsa.gradle.codegen.remoting.OpenapiRemotingCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.remoting.WebRemotingCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.webApp.JavaWebAppCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.webApp.WebWebAppCodeGenerator;
import com.gridnine.platform.elsa.gradle.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.domain.DomainMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ElsaCodeGenTask extends DefaultTask {
    public ElsaCodeGenTask() {
        setGroup("elsa");
    }

    @TaskAction
    public void generate() {
        var records = new ArrayList<HasCodeGenType>();
        var jext = getProject().getExtensions().findByType(ElsaJavaExtension.class);
        var javaProject = false;
        if (jext != null) {
            records.addAll(jext.getCodegenRecords());
            javaProject = true;
        } else {
            var wext = getProject().getExtensions().getByType(ElsaWebExtension.class);
            records.addAll(wext.getCodegenRecords());
        }
        Map<Object, Object> context = new HashMap<>();
        context.put("project", getProject());
        context.put("records", records);
        if(jext != null) {
            context.put("elsa-global-data", jext.getGlobalData());
        }
        Set<File> generatedFiles = new HashSet<>();
        var generators = new LinkedHashMap<GeneratorType, Map<File, List<HasCodeGenType>>>();
        records.forEach(it -> {
            if(it instanceof BaseCodeGenRecord bcr) {
                var dests = generators
                        .computeIfAbsent(it.getGeneratorType(), t -> new LinkedHashMap<>());
                dests.computeIfAbsent(bcr.getDestinationDir(), (i)-> new ArrayList<>()).add(bcr);
            }
            if(it instanceof OpenApiCodeGenRecord ocr){
                var recs = generators
                        .computeIfAbsent(ocr.getGeneratorType(), t -> new LinkedHashMap<>()).computeIfAbsent(ocr.getYamlFile(), (i) -> new ArrayList<>());
                var rec = new OpenApiCodeGenRecord();
                recs.add(rec);
                rec.setYamlFile(ocr.getYamlFile());
                rec.getRestIds().addAll(ocr.getRestIds());
            }
        });
        if (javaProject) {
            var domainMetaRegistry = new DomainMetaRegistry();
            context.put("domain-meta-registry", domainMetaRegistry);
            var parser = new DomainMetaRegistryParser();
            var projects = new ArrayList<Project>();
            var allProjects = getProject().getRootProject().getAllprojects();
            getProject().getConfigurations().getAt("implementation").getAllDependencies().withType(ProjectDependency.class).forEach((proj) -> {
                String path = proj.getPath();
                allProjects.stream().filter(it -> it.getPath().equals(path)).findFirst().ifPresent(projects::add);
            });
            projects.add(getProject());
            projects.forEach(project -> {
                try {
                    var extension = project.getExtensions().findByType(ElsaJavaExtension.class);
                    if (extension != null) {
                        var records2 = (List<?>) extension.getClass().getMethod("getCodegenRecords").invoke(extension);
                        for (Object record : records2) {
                            var type = (Enum<?>) record.getClass().getMethod("getGeneratorType").invoke(record);
                            if ("JAVA_DOMAIN".equals(type.name())) {
                                @SuppressWarnings("unchecked") var sources = (List<File>) record.getClass().getMethod("getSources").invoke(record);
                                parser.updateMetaRegistry(domainMetaRegistry, sources);
                                @SuppressWarnings("unchecked") var localInjections = (List<File>) record.getClass().getMethod("getLocalInjections").invoke(record);
                                parser.updateMetaRegistry(domainMetaRegistry, localInjections);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        var gens = new ArrayList<>(generators.entrySet());
        gens.sort(Comparator.comparing(it -> switch (it.getKey()) {
            case JAVA_REMOTING -> 2;
            case WEB_REMOTING -> 4;
            default -> 0;
        }));
        gens.forEach(entry -> {
            var codeGen = (CodeGenerator) switch (entry.getKey()) {
                case JAVA_ADMIN_UI -> new JavaAdminUiCodeGenerator();
                case JAVA_WEB_APP -> new JavaWebAppCodeGenerator();
                case JAVA_DOMAIN -> new JavaDomainCodeGenerator();
                case JAVA_CUSTOM -> new JavaCustomCodeGenerator();
                case JAVA_REMOTING -> new JavaRemotingCodeGenerator();
                case WEB_WEB_APP -> new WebWebAppCodeGenerator();
                case OPENAPI_REMOTING -> new OpenapiRemotingCodeGenerator();
                case JAVA_L10N -> new JavaL10nCodeGenerator();
                case WEB_REMOTING -> new WebRemotingCodeGenerator();
                case WEB_L10N -> new WebL10nCodeGenerator();
            };
            entry.getValue().forEach((key1, value1) -> value1.forEach(value2 ->BuildExceptionUtils.wrapException(() ->codeGen.generate(value2, key1, generatedFiles, context))));
        });
        Set<File> destDirs = new HashSet<>();
        records.stream().filter(it -> it instanceof BaseCodeGenRecord).forEach(it -> destDirs.add(((BaseCodeGenRecord) it).getDestinationDir()));
        cleanupDirs(destDirs, generatedFiles);
    }

    private boolean cleanupDirs(Collection<File> destDirs, Set<File> generatedFiles) {
        var result = new AtomicBoolean(false);
        destDirs.forEach(fileOrDir -> {
            if (!fileOrDir.exists()) {
                return;
            }
            if (fileOrDir.isFile()) {
                if (!generatedFiles.contains(fileOrDir)) {
                    fileOrDir.delete();
                } else {
                    result.set(true);
                }
                return;
            }
            var subRes = cleanupDirs(Arrays.asList(Objects.requireNonNull(fileOrDir.listFiles())), generatedFiles);
            if (subRes) {
                result.set(true);
                return;
            }
            fileOrDir.delete();
        });
        return result.get();
    }
}
