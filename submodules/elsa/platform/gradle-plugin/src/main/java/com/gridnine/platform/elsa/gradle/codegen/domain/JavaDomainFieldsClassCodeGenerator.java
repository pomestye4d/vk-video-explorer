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
import com.gridnine.platform.elsa.gradle.meta.domain.*;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JavaDomainFieldsClassCodeGenerator {

    static boolean isFieldIncluded(String fieldId, String includedFields, String excludedFields){
        if(!BuildTextUtils.isBlank(excludedFields) && isFieldMatchesPattern(fieldId, excludedFields)){
            return false;
        }
        return BuildTextUtils.isBlank(includedFields) || isFieldMatchesPattern(fieldId, includedFields);
    }

    static AssetDescription createAssetDescription(VirtualAssetDescription va, DomainMetaRegistry registry, Map<Object, Object> context){
        var assetDescription = new AssetDescription(va.getId());
        DomainMetaRegistry fullRegistry = (DomainMetaRegistry) context.get("domain-meta-registry");
        var baseAsset = registry.getAssets().get(va.getBaseAsset());
        if(baseAsset == null){
            baseAsset = fullRegistry.getAssets().get(va.getBaseAsset());
        }
        baseAsset.getProperties().forEach((k, v) -> {
            if (isFieldIncluded(k, va.getIncludedFields(), va.getExcludedFields())) {
                assetDescription.getProperties().put(k, v);
            }
        });
        baseAsset.getCollections().forEach((k, v) -> {
            if (isFieldIncluded(k, va.getIncludedFields(), va.getExcludedFields())) {
                assetDescription.getCollections().put(k, v);
            }
        });
        va.getJoins().forEach(join -> {
            var joinedAsset = fullRegistry.getAssets().get(join.getJoinedEntity());
            joinedAsset.getProperties().forEach((k, v) -> {
                if (isFieldIncluded(k, join.getIncludedFields(), join.getExcludedFields())) {
                    assetDescription.getProperties().put(k, v);
                }
            });
            joinedAsset.getCollections().forEach((k, v) -> {
                if (isFieldIncluded(k, join.getIncludedFields(), join.getExcludedFields())) {
                    assetDescription.getCollections().put(k, v);
                }
            });
        });
        return assetDescription;
    }

    private static boolean isFieldMatchesPattern(String fieldId, String excludedFields) {
        for(String item: excludedFields.split(";")){
            if(!BuildTextUtils.isBlank(item) && fieldId.matches(item)){
                return true;
            }
        }
        return false;
    }

    public static void generate(DomainMetaRegistry registry, File destDir, Set<File> generatedFiles, Map<Object, Object> context) {
        registry.getSearchableProjections().values().forEach(it -> BuildExceptionUtils.wrapException(() -> generateFieldsClass(it, destDir, generatedFiles)));
        registry.getAssets().values().forEach(it -> BuildExceptionUtils.wrapException(() -> generateFieldsClass(it, destDir, generatedFiles)));
        registry.getVirtualAssets().values().forEach(it -> BuildExceptionUtils.wrapException(() -> generateFieldsClass(createAssetDescription(it, registry, context), destDir, generatedFiles)));
    }


    private static void generateFieldsClass(BaseSearchableDescription sd, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        var packageName = JavaCodeGeneratorUtils.getPackage(sd.getId());
        gen.setPackageName(packageName);
        gen.wrapWithBlock("public class %sFields".formatted(JavaCodeGeneratorUtils.getSimpleName(sd.getId())), () -> {
            for (DatabasePropertyDescription pd : sd.getProperties().values()) {
                gen.blankLine();
                gen.printLine("public final static _%1$sField %1$s = new _%1$sField();".formatted(pd.getId()));
            }
            for (DatabaseCollectionDescription cd : sd.getCollections().values()) {
                gen.blankLine();
                gen.printLine("public final static _%1$sField %1$s = new _%1$sField();".formatted(cd.getId()));
            }
            for (DatabasePropertyDescription pd : sd.getProperties().values()) {
                gen.blankLine();
                gen.addImport("com.gridnine.platform.elsa.common.core.search.FieldNameSupport");
                var sb = new StringBuilder("public static class _%sField extends FieldNameSupport".formatted(pd.getId()));
                switch (pd.getType()) {
                    case LONG -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.EqualitySupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ComparisonSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.NumberOperationsSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements EqualitySupport, ComparisonSupport, NumberOperationsSupport, SortSupport, ArgumentType<Long>");
                    }
                    case INT -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.EqualitySupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ComparisonSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.NumberOperationsSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements EqualitySupport, ComparisonSupport, NumberOperationsSupport, SortSupport, ArgumentType<Integer>");
                    }
                    case LOCAL_DATE_TIME -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ComparisonSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("java.time.LocalDateTime");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements ComparisonSupport, SortSupport, ArgumentType<LocalDateTime>");
                    }
                    case LOCAL_DATE -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ComparisonSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.EqualitySupport");
                        gen.addImport("java.time.LocalDate");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements ComparisonSupport, SortSupport, EqualitySupport,  ArgumentType<LocalDate>");
                    }
                    case ENTITY_REFERENCE -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.EqualitySupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        gen.addImport("com.gridnine.platform.elsa.common.core.model.domain.EntityReference");
                        gen.addImport(pd.getClassName());
                        sb.append(" implements SortSupport, EqualitySupport, ArgumentType<EntityReference<%s>>".formatted(JavaCodeGeneratorUtils.getSimpleName(pd.getClassName())));
                    }
                    case BIG_DECIMAL -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ComparisonSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.NumberOperationsSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        gen.addImport("java.math.BigDecimal");
                        sb.append(" implements ComparisonSupport, NumberOperationsSupport, SortSupport, ArgumentType<BigDecimal>");
                    }
                    case BOOLEAN -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.EqualitySupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements EqualitySupport, ArgumentType<Boolean>");
                    }
                    case STRING,TEXT -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.EqualitySupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.StringOperationsSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ComparisonSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements EqualitySupport, StringOperationsSupport, ComparisonSupport, SortSupport, ArgumentType<String>");
                    }
                    case UUID -> {
                        gen.addImport(UUID.class.getName());
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.EqualitySupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements EqualitySupport, ArgumentType<UUID>");
                    }
                    case ENUM -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.EqualitySupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        gen.addImport(pd.getClassName());
                        sb.append(" implements EqualitySupport, SortSupport, ArgumentType<%s>".formatted(JavaCodeGeneratorUtils.getSimpleName(pd.getClassName())));
                    }
                    case INSTANT -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ComparisonSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.SortSupport");
                        gen.addImport("java.time.Instant");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements ComparisonSupport, SortSupport, ArgumentType<Instant>");
                    }
                    case TEXT_SEARCH -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.FullTextSearchSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements FullTextSearchSupport, ArgumentType<String>");
                    }
                }
                gen.wrapWithBlock(sb.toString(), () -> {
                    gen.wrapWithBlock("_%sField()".formatted(pd.getId()), () -> gen.printLine("super(\"%s\");".formatted(pd.getId())));
                    gen.blankLine();
                    gen.printLine("@Override");
                    gen.wrapWithBlock("public String toString()", ()->{
                        gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                        gen.printLine("return LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, this.name);"
                                .formatted(BuildTextUtils.joinToString(pd.getDisplayNames().keySet().stream().map("\"%s\""::formatted).toList(),
                                        ","),
                                        BuildTextUtils.joinToString(pd.getDisplayNames().values().stream().map("\"%s\""::formatted).toList(),
                                                ", ")));
                    });
                });
            }

            for (DatabaseCollectionDescription cd : sd.getCollections().values()) {
                gen.blankLine();
                gen.addImport("com.gridnine.platform.elsa.common.core.search.FieldNameSupport");
                var sb = new StringBuilder("public static class _%sField extends FieldNameSupport".formatted(cd.getId()));
                switch (cd.getElementType()) {
                    case ENTITY_REFERENCE -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.model.domain.EntityReference");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.CollectionSupport");
                        gen.addImport(cd.getElementClassName());
                        sb.append(" implements CollectionSupport, ArgumentType<EntityReference<%s>>".formatted(JavaCodeGeneratorUtils.getSimpleName(cd.getElementClassName())));
                    }
                    case ENUM -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.model.domain.EntityReference");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.CollectionSupport");
                        gen.addImport(cd.getElementClassName());
                        sb.append(" implements CollectionSupport, ArgumentType<%s>".formatted(JavaCodeGeneratorUtils.getSimpleName(cd.getElementClassName())));
                    }
                    case UUID -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.CollectionSupport");
                        gen.addImport(UUID.class.getName());
                        sb.append(" implements CollectionSupport, ArgumentType<UUID>");
                    }
                    case STRING -> {
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.CollectionSupport");
                        gen.addImport("com.gridnine.platform.elsa.common.core.search.ArgumentType");
                        sb.append(" implements CollectionSupport, ArgumentType<String>");
                    }
                }
                gen.wrapWithBlock(sb.toString(), () -> {
                    gen.wrapWithBlock("_%sField()".formatted(cd.getId()), () -> gen.printLine("super(\"%s\");".formatted(cd.getId())));
                    gen.blankLine();
                    gen.printLine("@Override");
                    gen.wrapWithBlock("public String toString()", ()->{
                        gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                        gen.printLine("return LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, this.name);"
                                .formatted(BuildTextUtils.joinToString(cd.getDisplayNames().keySet().stream().map("\"%s\""::formatted).toList(),
                                                ","),
                                        BuildTextUtils.joinToString(cd.getDisplayNames().values().stream().map("\"%s\""::formatted).toList(),
                                                ", ")));
                    });
                });
            }
        });

        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), "%sFields.java"
                .formatted(sd.getId()), destDir);
        generatedFiles.add(file);
    }
}
