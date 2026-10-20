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

package com.gridnine.platform.elsa.core.storage.database.jdbc.model;

import com.gridnine.platform.elsa.common.core.l10n.SupportedLocalesProvider;
import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.model.domain.BaseAsset;
import com.gridnine.platform.elsa.common.core.model.domain.BaseSearchableProjection;
import com.gridnine.platform.elsa.common.core.model.domain.BaseVirtualAsset;
import com.gridnine.platform.elsa.common.core.model.domain.VersionInfo;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.serialization.JsonMarshaller;
import com.gridnine.platform.elsa.common.core.serialization.JsonUnmarshaller;
import com.gridnine.platform.elsa.common.meta.common.EntityDescription;
import com.gridnine.platform.elsa.common.meta.custom.CustomMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.BaseSearchableDescription;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.core.storage.database.DatabaseObjectData;
import com.gridnine.platform.elsa.core.storage.database.DatabaseSearchableProjectionWrapper;
import com.gridnine.platform.elsa.core.storage.database.jdbc.handlers.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class JdbcDatabaseMetadataProvider {

    private DomainMetaRegistry metaRegistry;
    private SupportedLocalesProvider supportedLocalesProvider;

    private final Map<String, JdbcTableDescription> descriptionsMap = new LinkedHashMap<>();

    private final Map<String, JdbcSequenceDescription> sequencesMap = new LinkedHashMap<>();

    @Autowired
    private CustomMetaRegistry customMetaRegistry;

    @Autowired
    private ReflectionFactory reflectionFactory;

    @Autowired
    private ListableBeanFactory beanFactory;

    @Value("${elsa.storage.tsLocales:english,russian}")
    private String tsLocalesRaw;

    private List<String> tsLocales;

    public final static String OBJECT_ID_COLUMN = "objectId";

    @Autowired
    public void setMetaRegistry(DomainMetaRegistry metaRegistry) {
        this.metaRegistry = metaRegistry;
    }

    @Autowired
    public void setSupportedLocalesProvider(SupportedLocalesProvider supportedLocalesProvider) {
        this.supportedLocalesProvider = supportedLocalesProvider;
    }


    public Map<String, JdbcTableDescription> getDescriptions() {
        return descriptionsMap;
    }

    public Map<String, JdbcSequenceDescription> getSequencesMap() {
        return sequencesMap;
    }

    @PostConstruct
    public void init() {
        tsLocales = Arrays.stream(tsLocalesRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        sequencesMap.put("intid", new JdbcSequenceDescription("intid", JdbcSequenceType.INT));
        {
            var classMapping = new JdbcTableDescription("identifiers");
            classMapping.getFields().put("id", new JdbcIdFieldHandler(true));
            descriptionsMap.put(classMapping.getName(), classMapping);
        }
        {
            var classMapping = new JdbcTableDescription("classmapping");
            classMapping.getFields().put("id", new JdbcIdFieldHandler(false));
            classMapping.getFields().put("classname", new JdbcStringFieldHandler("classname", true));
            descriptionsMap.put(classMapping.getName(), classMapping);
        }
        {
            var enumTable = new JdbcTableDescription("enummapping");
            enumTable.getFields().put("id", new JdbcIdFieldHandler(false));
            enumTable.getFields().put("classname", new JdbcStringFieldHandler("classname", false));
            enumTable.getFields().put("enumconstant", new JdbcStringFieldHandler("enumconstant", false));
            supportedLocalesProvider.getSupportedLocales().forEach(loc -> {
                var id = "%sName".formatted(loc.getLanguage());
                enumTable.getFields().put(id, new JdbcStringFieldHandler(id, true));
            });
            descriptionsMap.put(enumTable.getName(), enumTable);
        }
        {
            var structureUpdatesTable = new JdbcTableDescription("structureupdates");
            structureUpdatesTable.getFields().put("id", new JdbcStringFieldHandler("id", true));
            structureUpdatesTable.getFields().put("date", new JdbcInstantFieldHandler("date", false));
            descriptionsMap.put(structureUpdatesTable.getName(), structureUpdatesTable);
        }
        metaRegistry.getDocuments().values().forEach(doc -> {
            if (!doc.isAbstract()) {
                {
                    var docTable = new JdbcTableDescription(JdbcUtils.getTableName(doc.getId()));
                    docTable.getFields().put(BaseIdentity.Fields.idName, new JdbcIdFieldHandler(true));
                    docTable.getFields().put(DatabaseObjectData.Fields.data, new JdbcBlobFieldHandler(DatabaseObjectData.Fields.data));
                    docTable.getFields().put(VersionInfo.Fields.revision, new JdbcIntFieldHandler(VersionInfo.Fields.revision, false));
                    docTable.getFields().put(VersionInfo.Fields.modifiedBy, new JdbcStringFieldHandler(VersionInfo.Fields.modifiedBy, false));
                    docTable.getFields().put(VersionInfo.Fields.modified, new JdbcInstantFieldHandler(VersionInfo.Fields.modified, false));
                    docTable.getFields().put(VersionInfo.Fields.comment, new JdbcStringFieldHandler(VersionInfo.Fields.comment, false));
                    docTable.getFields().put(VersionInfo.Fields.versionNumber, new JdbcIntFieldHandler(VersionInfo.Fields.versionNumber, false));
                    descriptionsMap.put(docTable.getName(), docTable);
                }
                {
                    var versionTable = createVersionTable(doc.getId());
                    descriptionsMap.put(versionTable.getName(), versionTable);
                }
                {
                    var captionTable = createCaptionTable(doc.getId(), doc.getLocalizableCaptionExpression() != null);
                    descriptionsMap.put(captionTable.getName(), captionTable);
                }
            }
        });
        metaRegistry.getAssets().values().forEach(asset -> {
            if (!asset.isAbstract()) {
                {
                    var assetTable = new JdbcTableDescription(JdbcUtils.getTableName(asset.getId()));
                    assetTable.getFields().put(BaseIdentity.Fields.idName, new JdbcIdFieldHandler(true));
                    assetTable.getFields().put(VersionInfo.Fields.revision, new JdbcIntFieldHandler(VersionInfo.Fields.revision, false));
                    assetTable.getFields().put(VersionInfo.Fields.modifiedBy, new JdbcStringFieldHandler(VersionInfo.Fields.modifiedBy, false));
                    assetTable.getFields().put(VersionInfo.Fields.modified, new JdbcInstantFieldHandler(VersionInfo.Fields.modified, false));
                    assetTable.getFields().put(VersionInfo.Fields.comment, new JdbcStringFieldHandler(VersionInfo.Fields.comment, false));
                    assetTable.getFields().put(VersionInfo.Fields.versionNumber, new JdbcIntFieldHandler(VersionInfo.Fields.versionNumber, false));
                    var parent = asset.getExtendsId();
                    while (parent != null) {
                        var parentDescr = metaRegistry.getAssets().get(parent);
                        if (parentDescr != null) {
                            fillBaseSearchableFields(assetTable, parentDescr);
                            parent = parentDescr.getExtendsId();
                            continue;
                        }
                        var customEntityParentDescr = customMetaRegistry.getEntities().get(parent);
                        if (customEntityParentDescr != null) {
                            fillBaseSearchableFields(assetTable, customEntityParentDescr);
                            parent = customEntityParentDescr.getExtendsId();
                            continue;
                        }
                        throw Xeption.forDeveloper("unknown object type %s".formatted(parent));
                    }
                    fillBaseSearchableFields(assetTable, asset);
                    descriptionsMap.put(assetTable.getName(), assetTable);
                }
                {
                    var versionTable = createVersionTable(asset.getId());
                    descriptionsMap.put(versionTable.getName(), versionTable);
                }
                {
                    var captionTable = createCaptionTable(asset.getId(), asset.getLocalizableCaptionExpression() != null);
                    descriptionsMap.put(captionTable.getName(), captionTable);
                }
            }
        });
        metaRegistry.getVirtualAssets().values().forEach(va -> {
            var asset = JdbcUtils.createAssetDescription(va, metaRegistry);
            var assetTable = new JdbcTableDescription(JdbcUtils.getTableName(asset.getId()));
            assetTable.setVirtual(true);
            assetTable.getFields().put(BaseIdentity.Fields.idName, new JdbcIdFieldHandler(true));
            if (JdbcUtils.isFieldIncluded(BaseVirtualAsset.Fields.versionInfo, va.getIncludedFields(), va.getExcludedFields())) {
                assetTable.getFields().put(VersionInfo.Fields.revision, new JdbcIntFieldHandler(VersionInfo.Fields.revision, false));
                assetTable.getFields().put(VersionInfo.Fields.modifiedBy, new JdbcStringFieldHandler(VersionInfo.Fields.modifiedBy, false));
                assetTable.getFields().put(VersionInfo.Fields.modified, new JdbcInstantFieldHandler(VersionInfo.Fields.modified, false));
                assetTable.getFields().put(VersionInfo.Fields.comment, new JdbcStringFieldHandler(VersionInfo.Fields.comment, false));
                assetTable.getFields().put(VersionInfo.Fields.versionNumber, new JdbcIntFieldHandler(VersionInfo.Fields.versionNumber, false));
            }
            fillBaseSearchableFields(assetTable, asset);
            descriptionsMap.put(assetTable.getName(), assetTable);
        });
        metaRegistry.getSearchableProjections().values().forEach(proj -> {
            {
                var projTable = new JdbcTableDescription(JdbcUtils.getTableName(proj.getId()));
                projTable.getFields().put(BaseSearchableProjection.Fields.navigationKey, new JdbcIntFieldHandler("navigationkey", false));

                projTable.getFields().put(BaseSearchableProjection.Fields.document, new JdbcEntityReferenceFieldHandler(BaseSearchableProjection.Fields.document, true, reflectionFactory.getClass(proj.getDocument()),
                        isAbstract(proj.getDocument()), isNotCachedCaption(proj.getDocument())));
                var parent = proj.getExtendsId();
                while (parent != null) {
                    var parentDescr = metaRegistry.getAssets().get(parent);
                    if (parentDescr != null) {
                        fillBaseSearchableFields(projTable, parentDescr);
                        parent = parentDescr.getExtendsId();
                        continue;
                    }
                    var customEntityParentDescr = customMetaRegistry.getEntities().get(parent);
                    if (customEntityParentDescr != null) {
                        fillBaseSearchableFields(projTable, customEntityParentDescr);
                        parent = customEntityParentDescr.getExtendsId();
                        continue;
                    }
                    throw Xeption.forDeveloper("unknown object type %s".formatted(parent));
                }
                fillBaseSearchableFields(projTable, proj);
                descriptionsMap.put(projTable.getName(), projTable);
            }
        });

    }

    private void fillBaseSearchableFields(JdbcTableDescription assetTable, EntityDescription customEntityParentDescr) {
        customEntityParentDescr.getProperties().values().forEach(prop -> {
            var propId = prop.getId();
            if (BaseAsset.Fields.versionInfo.equals(propId)) {
                return;
            }
            if (BaseIdentity.Fields.idName.equals(propId)) {
                return;
            }
            var handler = switch (prop.getType()) {
                case UUID -> new JdbcUUIDFieldHandler(propId, true);
                case STRING -> new JdbcStringFieldHandler(propId, true);
                case LOCAL_DATE -> new JdbcLocalDateFieldHandler(propId, true);
                case LOCAL_DATE_TIME -> new JdbcLocalDateTimeFieldHandler(propId, true);
                case INSTANT -> new JdbcInstantFieldHandler(propId, true);
                case ENUM -> new JdbcEnumFieldHandler(propId, reflectionFactory.getClass(prop.getClassName()), true);
                case ENTITY -> new JdbcEntityFieldHandler(propId, beanFactory, reflectionFactory.getClass(prop.getClassName()));
                case BOOLEAN -> new JdbcBooleanFieldHandler(propId, true);
                case ENTITY_REFERENCE ->
                        new JdbcEntityReferenceFieldHandler(propId, true, reflectionFactory.getClass(prop.getClassName()),
                                isAbstract(prop.getClassName()), isNotCachedCaption(prop.getClassName()));
                case LONG -> new JdbcLongFieldHandler(propId, true);
                case INT -> new JdbcIntFieldHandler(propId, true);
                case BIG_DECIMAL -> new JdbcBigDecimalFieldHandler(propId, true);

                default -> throw new IllegalStateException("Unexpected value: " + prop.getType());
            };
            assetTable.getFields().put(propId, handler);
        });
        customEntityParentDescr.getCollections().values().forEach(coll -> {
            var collId = coll.getId();
            var handler = switch (coll.getElementType()) {
                case STRING -> new JdbcStringCollectionFieldHandler(collId, true);
                case ENUM ->
                        new JdbcEnumCollectionFieldHandler(collId, reflectionFactory.getClass(coll.getElementClassName()), true);
                case UUID -> new JdbcUUIDCollectionFieldHandler(collId, true);
                case ENTITY_REFERENCE ->
                        new JdbcEntityReferenceCollectionFieldHandler(collId, true, reflectionFactory.getClass(coll.getElementClassName()),
                                isAbstract(coll.getElementClassName()), isNotCachedCaption(coll.getElementClassName()));
                case ENTITY -> new JdbcEntityCollectionFieldHandler(collId, beanFactory, reflectionFactory.getClass(coll.getElementClassName()));
                default -> throw new IllegalStateException("Unexpected value: " + coll.getElementType());
            };
            assetTable.getFields().put(collId, handler);
        });
    }

    private boolean isAbstract(String document) {
        var docDescr = metaRegistry.getDocuments().get(document);
        if (docDescr != null) {
            return docDescr.isAbstract();
        }
        var assetDescr = metaRegistry.getAssets().get(document);
        if (assetDescr != null) {
            return assetDescr.isAbstract();
        }
        throw Xeption.forDeveloper("unknown object type %s".formatted(document));
    }

    private boolean isNotCachedCaption(String document) {
        var docDescr = metaRegistry.getDocuments().get(document);
        if (docDescr != null) {
            return !docDescr.isCacheCaption() && !docDescr.isCacheResolve();
        }
        var assetDescr = metaRegistry.getAssets().get(document);
        if (assetDescr != null) {
            return !assetDescr.isCacheCaption() && !assetDescr.isCacheResolve();
        }
        throw Xeption.forDeveloper("unknown object type %s".formatted(document));
    }

    private JdbcTableDescription createCaptionTable(String docId, boolean localizable) {
        var captionTable = new JdbcTableDescription(JdbcUtils.getCaptionTableName(docId));
        captionTable.getFields().put(BaseIdentity.Fields.idName, new JdbcIdFieldHandler(true));
        if (localizable) {
            supportedLocalesProvider.getSupportedLocales().forEach(loc -> {
                var id = "%sCaption".formatted(loc.getLanguage());
                captionTable.getFields().put(id, new JdbcStringFieldHandler(id, true));
            });
        } else {
            var id = "caption";
            captionTable.getFields().put(id, new JdbcStringFieldHandler(id, true));
        }
        return captionTable;
    }

    private JdbcTableDescription createVersionTable(String id) {
        var versionTable = new JdbcTableDescription(JdbcUtils.getVersionTableName(id));
        versionTable.getFields().put(OBJECT_ID_COLUMN, new JdbcUUIDFieldHandler(OBJECT_ID_COLUMN, true));
        versionTable.getFields().put(VersionInfo.Fields.versionNumber, new JdbcIntFieldHandler(VersionInfo.Fields.versionNumber, true));
        versionTable.getFields().put(DatabaseObjectData.Fields.data, new JdbcBlobFieldHandler(DatabaseObjectData.Fields.data));
        versionTable.getFields().put(VersionInfo.Fields.modifiedBy, new JdbcStringFieldHandler(VersionInfo.Fields.modifiedBy, false));
        versionTable.getFields().put(VersionInfo.Fields.modified, new JdbcInstantFieldHandler(VersionInfo.Fields.modified, false));
        versionTable.getFields().put(VersionInfo.Fields.comment, new JdbcStringFieldHandler(VersionInfo.Fields.comment, false));
        return versionTable;
    }

    private void fillBaseSearchableFields(JdbcTableDescription table, BaseSearchableDescription search) {
        table.getFields().put(DatabaseSearchableProjectionWrapper.Fields.aggregatedData, new JdbcTextFieldHandler(DatabaseSearchableProjectionWrapper.Fields.aggregatedData, true));
        search.getProperties().values().forEach(prop -> {
            var propId = prop.getId();
            if (BaseAsset.Fields.versionInfo.equals(propId)) {
                return;
            }
            var handler = switch (prop.getType()) {
                case UUID -> new JdbcUUIDFieldHandler(propId, true);
                case STRING -> new JdbcStringFieldHandler(propId, true);
                case TEXT -> new JdbcTextFieldHandler(propId, true);
                case LOCAL_DATE -> new JdbcLocalDateFieldHandler(propId, true);
                case LOCAL_DATE_TIME -> new JdbcLocalDateTimeFieldHandler(propId, true);
                case INSTANT -> new JdbcInstantFieldHandler(propId, true);
                case ENUM -> new JdbcEnumFieldHandler(propId, reflectionFactory.getClass(prop.getClassName()), true);
                case BOOLEAN -> new JdbcBooleanFieldHandler(propId, true);
                case ENTITY_REFERENCE ->
                        new JdbcEntityReferenceFieldHandler(propId, true, reflectionFactory.getClass(prop.getClassName()),
                                isAbstract(prop.getClassName()), isNotCachedCaption(prop.getClassName()));
                case LONG -> new JdbcLongFieldHandler(propId, true);
                case INT -> new JdbcIntFieldHandler(propId, true);
                case BIG_DECIMAL -> new JdbcBigDecimalFieldHandler(propId, true);
                case ENTITY -> new JdbcEntityFieldHandler(propId, beanFactory, reflectionFactory.getClass(prop.getClassName()));
                case TEXT_SEARCH -> new JdbcTextSearchFieldHandler(propId, true, tsLocales);
            };
            table.getFields().put(propId, handler);
        });
        search.getCollections().values().forEach(coll -> {
            var collId = coll.getId();
            var handler = switch (coll.getElementType()) {
                case STRING -> new JdbcStringCollectionFieldHandler(collId, true);
                case ENUM ->
                        new JdbcEnumCollectionFieldHandler(collId, reflectionFactory.getClass(coll.getElementClassName()), true);
                case UUID -> new JdbcUUIDCollectionFieldHandler(collId, true);
                case ENTITY -> new JdbcEntityCollectionFieldHandler(collId, beanFactory, reflectionFactory.getClass(coll.getElementClassName()));
                case ENTITY_REFERENCE ->
                        new JdbcEntityReferenceCollectionFieldHandler(collId, true, reflectionFactory.getClass(coll.getElementClassName()),
                                isAbstract(coll.getElementClassName()), isNotCachedCaption(coll.getElementClassName()));
            };
            table.getFields().put(collId, handler);
        });
    }
}
