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

import com.gridnine.platform.elsa.gradle.codegen.common.GenEntityDescription;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.common.*;
import com.gridnine.platform.elsa.gradle.meta.domain.*;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class JavaDomainEntitiesCodeGenerator {
    public static void generate(DomainMetaRegistry registry, File destDir, Set<File> generatedFiles, Map<Object, Object> context) throws Exception {
        for (EnumDescription ed : registry.getEnums().values()) {
            JavaCodeGeneratorUtils.generateJavaEnumCode(ed, true, destDir, generatedFiles);
        }
        for (EntityDescription ed : registry.getEntities().values()) {
            JavaCodeGeneratorUtils.generateJavaEntityCode(ed, destDir, "com.gridnine.platform.elsa.common.core.model.domain.BaseDomainEntity", true, generatedFiles);
        }
        for (DocumentDescription dd : registry.getDocuments().values()) {
            var ed = new GenEntityDescription();
            ed.setId(dd.getId());
            ed.setAbstract(dd.isAbstract());
            ed.setExtendsId(dd.getExtendsId() == null ? "com.gridnine.platform.elsa.common.core.model.domain.BaseDocument" : dd.getExtendsId());
            ed.getMaps().putAll(dd.getMaps());
            ed.getProperties().putAll(dd.getProperties());
            ed.getCollections().putAll(dd.getCollections());
            ed.setToLocalizableStringExpression(dd.getLocalizableCaptionExpression());
            ed.setSealable(true);
            ed.setGenerateToReference(true);
            ed.setToStringExpression(dd.getCaptionExpression());
            JavaCodeGeneratorUtils.generateJavaEntityCode(ed, destDir, generatedFiles);
        }

        for (SearchableProjectionDescription spd : registry.getSearchableProjections().values()) {
            var ed = createSearchableDescription(spd);
            ed.setExtendsId(ed.getExtendsId() != null ? ed.getExtendsId() :"com.gridnine.platform.elsa.common.core.model.domain.BaseSearchableProjection<%s>".formatted(spd.getDocument()));
            JavaCodeGeneratorUtils.generateJavaEntityCode(ed, destDir, generatedFiles);
        }
        for (AssetDescription ad : registry.getAssets().values()) {
            var ed = createSearchableDescription(ad);
            ed.setExtendsId(ad.getExtendsId() != null ? ad.getExtendsId() : "com.gridnine.platform.elsa.common.core.model.domain.BaseAsset");
            ed.setAbstract(ad.isAbstract());
            ed.setSealable(true);
            ed.setToStringExpression(ad.getCaptionExpression());
            ed.setToLocalizableStringExpression(ad.getLocalizableCaptionExpression());
            ed.setGenerateToReference(true);
            JavaCodeGeneratorUtils.generateJavaEntityCode(ed, destDir, generatedFiles);
        }
        for (VirtualAssetDescription ad : registry.getVirtualAssets().values()) {
            var ed = createSearchableDescription(JavaDomainFieldsClassCodeGenerator.createAssetDescription(ad, registry, context));
            ed.setExtendsId("com.gridnine.platform.elsa.common.core.model.domain.BaseVirtualAsset");
            if(!JavaDomainFieldsClassCodeGenerator.isFieldIncluded("versionInfo", ad.getIncludedFields(), ad.getExcludedFields())){
                ed.getProperties().remove("versionInfo");
            }
            ed.setSealable(true);
            JavaCodeGeneratorUtils.generateJavaEntityCode(ed, destDir, generatedFiles);
        }
    }

    private static GenEntityDescription createSearchableDescription(BaseSearchableDescription spd) {
        var ed = new GenEntityDescription();
        ed.setId(spd.getId());
        ed.setExtendsId(spd.getExtendsId());
        for (DatabasePropertyDescription prop : spd.getProperties().values()) {
            var sp = new StandardPropertyDescription();
            sp.setId(prop.getId());
            sp.setType(getStandardValueType(prop.getType()));
            sp.setNonNullable(prop.isNonNullable());
            sp.setClassName(prop.getClassName());
            ed.getProperties().put(prop.getId(), sp);
        }
        for (DatabaseCollectionDescription coll : spd.getCollections().values()) {
            var sc = new StandardCollectionDescription();
            sc.setId(coll.getId());
            sc.setElementType(getStandardValueType(coll.getElementType()));
            sc.setUnique(coll.isUnique());
            sc.setElementClassName(coll.getElementClassName());
            ed.getCollections().put(coll.getId(), sc);
        }
        return ed;
    }


    static StandardValueType getStandardValueType(DatabasePropertyType type) {
        return switch (type) {
            case STRING, TEXT->StandardValueType.STRING;
            case LOCAL_DATE -> StandardValueType.LOCAL_DATE;
            case LOCAL_DATE_TIME -> StandardValueType.LOCAL_DATE_TIME;
            case INSTANT -> StandardValueType.INSTANT;
            case ENUM -> StandardValueType.ENUM;
            case BOOLEAN -> StandardValueType.BOOLEAN;
            case ENTITY_REFERENCE -> StandardValueType.ENTITY_REFERENCE;
            case LONG -> StandardValueType.LONG;
            case INT -> StandardValueType.INT;
            case BIG_DECIMAL -> StandardValueType.BIG_DECIMAL;
            case UUID -> StandardValueType.UUID;
            case ENTITY -> StandardValueType.ENTITY;
            case TEXT_SEARCH -> StandardValueType.TEXT_SEARCH;
        };
    }

    static StandardValueType getStandardValueType(DatabaseCollectionType type) {
        return switch (type) {
            case STRING -> StandardValueType.STRING;
            case ENUM -> StandardValueType.ENUM;
            case ENTITY -> StandardValueType.ENTITY;
            case ENTITY_REFERENCE -> StandardValueType.ENTITY_REFERENCE;
            case UUID -> StandardValueType.UUID;
        };
    }
}
