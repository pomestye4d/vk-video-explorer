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

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.common.EntityDescription;
import com.gridnine.platform.elsa.gradle.meta.common.EnumDescription;
import com.gridnine.platform.elsa.gradle.meta.domain.*;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JavaDomainConfiguratorCodeGenerator {
    public static void generate(DomainMetaRegistry registry, String configurator, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        gen.setPackageName(JavaCodeGeneratorUtils.getPackage(configurator));
        gen.addImport("com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistryConfigurator");
        gen.addImport("com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry");
        gen.wrapWithBlock("public class %s implements DomainMetaRegistryConfigurator".formatted(JavaCodeGeneratorUtils.getSimpleName(configurator)), () -> {
            gen.blankLine();
            gen.printLine("@Override");
            gen.wrapWithBlock("public void updateMetaRegistry(DomainMetaRegistry registry)", () -> {
                for (EnumDescription ed : registry.getEnums().values()) {
                    JavaCodeGeneratorUtils.generateJavaEnumConfiguratorCode(ed, gen);
                }
                for (EntityDescription ed : registry.getEntities().values()) {
                    JavaCodeGeneratorUtils.generateJavaEntityConfiguratorCode(ed, gen);
                }
                for (DocumentDescription dd : registry.getDocuments().values()) {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.domain.DocumentDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var documentDescription = registry.getDocuments().computeIfAbsent(\"%s\", DocumentDescription::new);".formatted(dd.getId()));
                        if (dd.isCacheCaption()) {
                            gen.printLine("documentDescription.setCacheCaption(true);");
                        }
                        if (dd.isCacheResolve()) {
                            gen.printLine("documentDescription.setCacheResolve(true);");
                        }
                        if(dd.getLocalizableCaptionExpression() != null){
                            gen.printLine("documentDescription.setLocalizableCaptionExpression(\"%s\");".formatted(dd.getLocalizableCaptionExpression().replace("\"","\\\"")));
                        } else if(dd.getCaptionExpression() != null) {
                            gen.printLine("documentDescription.setCaptionExpression(\"%s\");".formatted(dd.getCaptionExpression().replace("\"","\\\"")));
                        }
                        JavaCodeGeneratorUtils.generateJavaEntityConfiguratorCode("documentDescription", dd, gen);
                    });
                }
                for (SearchableProjectionDescription pd : registry.getSearchableProjections().values()) {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.domain.SearchableProjectionDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var projectionDescription = registry.getSearchableProjections().computeIfAbsent(\"%s\", SearchableProjectionDescription::new);".formatted(pd.getId()));
                        gen.printLine("projectionDescription.setDocument(\"%s\");".formatted(pd.getDocument()));
                        generateJavaSearchableCode("projectionDescription", pd, gen);
                    });
                }
                for (AssetDescription ad : registry.getAssets().values()) {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.domain.AssetDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var assetDescription = registry.getAssets().computeIfAbsent(\"%s\", AssetDescription::new);".formatted(ad.getId()));
                        if(ad.getLocalizableCaptionExpression() != null){
                            gen.printLine("assetDescription.setLocalizableCaptionExpression(\"%s\");".formatted(ad.getLocalizableCaptionExpression().replace("\"","\\\"")));
                        } else if(ad.getCaptionExpression() != null){
                            gen.printLine("assetDescription.setCaptionExpression(\"%s\");".formatted(ad.getCaptionExpression().replace("\"","\\\"")));
                        }
                        generateJavaSearchableCode("assetDescription", ad, gen);
                    });
                }
                for (VirtualAssetDescription ad : registry.getVirtualAssets().values()) {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.domain.VirtualAssetDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var virtualAssetDescription = registry.getVirtualAssets().computeIfAbsent(\"%s\", VirtualAssetDescription::new);".formatted(ad.getId()));
                        gen.printLine("virtualAssetDescription.setBaseAsset(\"%s\");".formatted(ad.getBaseAsset()));
                        if(!BuildTextUtils.isBlank(ad.getIncludedFields())) {
                            gen.printLine("virtualAssetDescription.setIncludedFields(\"%s\");".formatted(ad.getIncludedFields()));
                        }
                        if(!BuildTextUtils.isBlank(ad.getExcludedFields())) {
                            gen.printLine("virtualAssetDescription.setExcludedFields(\"%s\");".formatted(ad.getExcludedFields()));
                        }
                        for(JoinDescription jd : ad.getJoins()){
                            gen.wrapWithBlock(null, ()->{
                                gen.addImport("com.gridnine.platform.elsa.common.meta.domain.JoinDescription");
                                gen.printLine("var joinDescription = new JoinDescription();");
                                gen.printLine("virtualAssetDescription.getJoins().add(joinDescription);");
                                gen.printLine("joinDescription.setJoinedEntity(\"%s\");".formatted(jd.getJoinedEntity()));
                                gen.printLine("joinDescription.setPrimaryKey(\"%s\");".formatted(jd.getPrimaryKey()));
                                gen.printLine("joinDescription.setForeignKey(\"%s\");".formatted(jd.getForeignKey()));
                                if(!BuildTextUtils.isBlank(jd.getIncludedFields())) {
                                    gen.printLine("joinDescription.setIncludedFields(\"%s\");".formatted(jd.getIncludedFields()));
                                }
                                if(!BuildTextUtils.isBlank(jd.getExcludedFields())) {
                                    gen.printLine("joinDescription.setExcludedFields(\"%s\");".formatted(jd.getExcludedFields()));
                                }
                            });
                        }
                    });
                }
            });

        });
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), configurator + ".java", destDir);
        generatedFiles.add(file);
    }

    private static void generateJavaSearchableCode(String descriptionName, BaseSearchableDescription sd, JavaCodeGenerator gen) throws Exception {
        if (sd.isHidden()) {
            gen.printLine("%s.setHidden(true);".formatted(descriptionName));
        }
        if (sd.getExtendsId() != null)  {
            gen.printLine("%s.setExtendsId(\"%s\");".formatted(descriptionName, sd.getExtendsId()));
        }
        sd.getParameters().forEach((k,v) -> gen.printLine("%s.getParameters().put(\"%s\", \"%s\");".formatted(descriptionName, k, v)));
        if(sd instanceof AssetDescription ad){
            if(ad.isCacheCaption()){
                gen.printLine("%s.setCacheCaption(true);".formatted(descriptionName));
            }
            if(ad.isCacheResolve()){
                gen.printLine("%s.setCacheResolve(true);".formatted(descriptionName));
            }
        }
        for (Map.Entry<Locale, String> entry : sd.getDisplayNames().entrySet()) {
            gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
            gen.printLine("%s.getDisplayNames().put(LocaleUtils.getLocale(\"%s\",\"%s\"), \"%s\");".formatted(descriptionName, entry.getKey().getLanguage(), entry.getKey().getCountry(), entry.getValue()));
        }
        for (DatabasePropertyDescription pd : sd.getProperties().values()) {
            gen.addImport("com.gridnine.platform.elsa.common.meta.domain.DatabasePropertyDescription");
            gen.addImport("com.gridnine.platform.elsa.common.meta.domain.DatabasePropertyType");
            gen.wrapWithBlock(null, () -> {
                gen.printLine("var propertyDescription = new DatabasePropertyDescription(\"%s\");".formatted(pd.getId()));
                gen.printLine("propertyDescription.setType(DatabasePropertyType.%s);".formatted(pd.getType().name()));
                pd.getParameters().forEach((k,v) -> gen.printLine("propertyDescription.getParameters().put(\"%s\", \"%s\");".formatted(k, v)));
                if (pd.getClassName() != null) {
                    gen.printLine("propertyDescription.setClassName(\"%s\");".formatted(pd.getClassName()));
                }
                if (pd.isCacheFind()) {
                    gen.printLine("propertyDescription.setCacheFind(true);");
                }
                if (pd.isCacheGetAll()) {
                    gen.printLine("propertyDescription.setCacheGetAll(true);");
                }
                if (pd.isUseInTextSearch()) {
                    gen.printLine("propertyDescription.setUseInTextSearch(true);");
                }
                if (pd.isNonNullable()) {
                    gen.printLine("propertyDescription.setNonNullable(true);");
                }
                for (Map.Entry<Locale, String> entry : pd.getDisplayNames().entrySet()) {
                    gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                    gen.printLine("propertyDescription.getDisplayNames().put(LocaleUtils.getLocale(\"%s\",\"%s\"), \"%s\");".formatted(entry.getKey().getLanguage(), entry.getKey().getCountry(), entry.getValue()));
                }
                gen.printLine("%s.getProperties().put(propertyDescription.getId(), propertyDescription);".formatted(descriptionName));
            });
        }
        for (DatabaseCollectionDescription cd : sd.getCollections().values()) {
            gen.addImport("com.gridnine.platform.elsa.common.meta.domain.DatabaseCollectionDescription");
            gen.addImport("com.gridnine.platform.elsa.common.meta.domain.DatabaseCollectionType");
            gen.wrapWithBlock(null, () -> {
                gen.printLine("var collectionDescription = new DatabaseCollectionDescription(\"%s\");".formatted(cd.getId()));
                gen.printLine("collectionDescription.setElementType(DatabaseCollectionType.%s);".formatted(cd.getElementType().name()));
                if (cd.getElementClassName() != null) {
                    gen.printLine("collectionDescription.setElementClassName(\"%s\");".formatted(cd.getElementClassName()));
                }
                if (cd.isUnique()) {
                    gen.printLine("collectionDescription.setUnique(true);");
                }
                if (cd.isUseInTextSearch()) {
                    gen.printLine("collectionDescription.setUseInTextSearch(true);");
                }
                for (Map.Entry<Locale, String> entry : cd.getDisplayNames().entrySet()) {
                    gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                    gen.printLine("collectionDescription.getDisplayNames().put(LocaleUtils.getLocale(\"%s\",\"%s\"), \"%s\");".formatted(entry.getKey().getLanguage(), entry.getKey().getCountry(), entry.getValue()));
                }
                gen.printLine("%s.getCollections().put(collectionDescription.getId(), collectionDescription);".formatted(descriptionName));
            });
        }
    }
}
