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

package com.gridnine.platform.elsa.core.storage.database.jdbc;

import com.gridnine.platform.elsa.common.core.model.common.*;
import com.gridnine.platform.elsa.common.core.model.domain.*;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.search.*;
import com.gridnine.platform.elsa.common.core.utils.*;
import com.gridnine.platform.elsa.common.meta.domain.*;
import com.gridnine.platform.elsa.core.storage.database.*;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.handlers.JdbcFieldHandler;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcDatabaseMetadataProvider;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcFieldType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcTableDescription;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcUtils;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.datasource.DataSourceUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


public class JdbcDatabase implements Database {
    public final static TypedParameterId<JdbcTemplate> JDBC_TEMPLATE_PARAM = () -> "JDBC_TEMPLATE_PARAM";
    public final static TypedParameterId<JdbcDialect> JDBC_DIALECT = () -> "JDBC_DIALECT";
    public final static TypedParameterId<EnumMapper> ENUM_MAPPER = () -> "ENUM_MAPPER";


    private final JdbcTemplate template;

    private final JdbcDatabaseMetadataProvider dbMetadataProvider;

    private final EnumMapper enumMapper;

    private final ClassMapper classMapper;

    private final JdbcDialect dialect;

    private final DomainMetaRegistry domainMetaRegistry;

    private final ReflectionFactory reflectionFactory;

    private final CaptionProvider captionProvider;
    private final JdbcDatabaseCustomizer customizer;

    private final Map<String, AssetDescription> virtualAssetsDescriptions = new ConcurrentHashMap<>();

    private AssetDescription getAssetDescription(String virtualAssetId){
        return virtualAssetsDescriptions.computeIfAbsent(virtualAssetId, id ->{
            var vad = domainMetaRegistry.getVirtualAssets().get(virtualAssetId);
            if(vad == null){
                throw Xeption.forDeveloper("unable to find virtual asset description with id " + virtualAssetId);
            }
            return JdbcUtils.createAssetDescription(vad, domainMetaRegistry);
        });
    }

    public JdbcDatabase(JdbcTemplate template, JdbcDatabaseMetadataProvider dbMetadataProvider, EnumMapper enumMapper, ClassMapper classMapper, CaptionProvider captionProvider,
                        JdbcDialect dialect, DomainMetaRegistry domainMetaRegistry, ReflectionFactory reflectionFactory, Map<String,Object> customParameters) {
        this.template = template;
        this.dbMetadataProvider = dbMetadataProvider;
        this.enumMapper = enumMapper;
        this.classMapper = classMapper;
        this.dialect = dialect;
        this.domainMetaRegistry = domainMetaRegistry;
        this.reflectionFactory = reflectionFactory;
        this.customizer = (JdbcDatabaseCustomizer) customParameters.get("JDBC_DATABASE_CUSTOMIZER");
        this.captionProvider = captionProvider;
    }

    @Override
    public <A extends BaseAsset> DatabaseAssetWrapper<A> loadAssetWrapper(Class<A> aClass, String id) {
        var description = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(aClass.getName()));
        Objects.requireNonNull(description);
        var columnNames = getColumnNames(description, Collections.emptySet(), Collections.emptySet());
        var sql = "select %s from %s where %s = ?".formatted(TextUtils.join(columnNames, ", "), description.getName(), BaseIdentity.Fields.idName);
        var result = template.query(sql, ps -> ps.setObject(1, UUID.fromString(id)), rs -> {
            if (!rs.next()) {
                return null;
            }
            return ExceptionUtils.wrapException(() -> {
                var asset = aClass.getDeclaredConstructor().newInstance();
                var handler = new AssetWrapperReflectionHandler<>(asset, domainMetaRegistry);
                fillObject(rs, handler, description, Collections.emptySet(), Collections.emptySet());
                return handler;
            });
        });
        return result == null ? null : result.getWrapper();
    }


    @Override
    public <A extends BaseAsset> void saveAsset(DatabaseAssetWrapper<A> assetWrapper, DatabaseAssetWrapper<A> oldAsset) throws Exception {
        if (oldAsset != null) {
            Set<String> changedProperties = new LinkedHashSet<>();
            var handler = new AssetWrapperReflectionHandler<>(assetWrapper, domainMetaRegistry);
            var oldHandler = new AssetWrapperReflectionHandler<>(oldAsset, domainMetaRegistry);
            var description = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(assetWrapper.getAsset().getClass().getName()));
            entry:
            for (var entry : description.getFields().entrySet()) {
                var newValues = entry.getValue().getSqlValues(handler.getValue(entry.getKey()), enumMapper, classMapper, reflectionFactory, dialect);
                var oldValues = entry.getValue().getSqlValues(oldHandler.getValue(entry.getKey()), enumMapper, classMapper, reflectionFactory, dialect);
                if (newValues.size() != oldValues.size()) {
                    changedProperties.add(entry.getKey());
                    continue;
                }
                for (var entry2 : oldValues.entrySet()) {
                    var newValue = newValues.get(entry2.getKey());
                    if (newValue == null ||
                            JdbcUtils.isNotEquals(entry2.getValue().getLeft(), newValue.getLeft())) {
                        changedProperties.add(entry.getKey());
                        continue entry;
                    }
                }
            }
            if (!changedProperties.isEmpty()) {
                var columnsValues = new LinkedHashMap<String, Pair<Object, JdbcFieldType>>();
                var valueExpressions = new LinkedHashMap<String, String>();
                for (var it : changedProperties) {
                    var fh = description.getFields().get(it);
                    columnsValues.putAll(fh.getSqlValues(handler.getValue(it), enumMapper, classMapper, reflectionFactory, dialect));
                    valueExpressions.putAll(fh.getSqlValueExpressions(dialect));
                }
                var query = "update %s set %s where id = ?".formatted(description.getName(),
                        TextUtils.join(columnsValues.keySet().stream().map(col -> "%s = %s".formatted(col, valueExpressions.getOrDefault(col, "?"))).toList(), ", "));
                template.update(query, (ps) -> {
                    int idx = 1;
                    for (Map.Entry<String, Pair<Object, JdbcFieldType>> entry : columnsValues.entrySet()) {
                        var value = entry.getValue().getLeft();
                        if (value == null) {
                            ps.setNull(idx, getSqlType(entry.getValue().getRight()));
                        } else {
                            setValue(ps, idx, entry.getValue().getRight(), entry.getValue().getLeft());
                        }
                        idx++;
                    }
                    ps.setObject(columnsValues.size() + 1, UUID.fromString(assetWrapper.getAsset().getId()));
                });
            }
            return;
        }
        insert(new AssetWrapperReflectionHandler<>(assetWrapper, domainMetaRegistry),
                JdbcUtils.getTableName(assetWrapper.getAsset().getClass().getName()));
    }


    @Override
    public <A extends BaseAsset> void saveAssetVersion(Class<A> aClass, String id, DatabaseBinaryData data, VersionInfo vi) throws Exception {
        var handler = new VersionWrapperReflectionHandler(vi, data, id);
        insert(handler, JdbcUtils.getVersionTableName(aClass.getName()));
    }

    @Override
    public <A extends BaseAsset> List<A> searchAssets(Class<A> cls, SearchQuery updateQuery) throws Exception {
        Set<String> properties = new LinkedHashSet<>();
        if (!updateQuery.getPreferredFields().isEmpty()) {
            properties.add(BaseIdentity.Fields.idName);
            properties.add(VersionInfo.Fields.revision);
            properties.add(VersionInfo.Fields.comment);
            properties.add(VersionInfo.Fields.modified);
            properties.add(VersionInfo.Fields.modifiedBy);
            properties.add(VersionInfo.Fields.versionNumber);
            properties.addAll(updateQuery.getPreferredFields());
        }
        return searchObjects(cls, () -> new AssetWrapperReflectionHandler<>(cls.getDeclaredConstructor().newInstance(), domainMetaRegistry),
                updateQuery, properties, Collections.singleton(DatabaseAssetWrapper.Fields.aggregatedData)).stream().map(it -> it.wrapper.getAsset()).toList();
    }

    @Override
    public List<VersionInfo> getVersionsMetadata(Class<?> cls, String id) {
        var fields = new LinkedHashSet<String>();
        fields.add(VersionInfo.Fields.versionNumber.toLowerCase());
        fields.add(VersionInfo.Fields.comment.toLowerCase());
        fields.add(VersionInfo.Fields.modified.toLowerCase());
        fields.add(VersionInfo.Fields.modifiedBy.toLowerCase());
        var result = new ArrayList<VersionInfo>();
        {
            var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getVersionTableName(cls.getName()));
            var selectSql = "select %s from %s where %s = ?".formatted(TextUtils.join(fields, ","),
                    JdbcUtils.getVersionTableName(cls.getName()), JdbcDatabaseMetadataProvider.OBJECT_ID_COLUMN);
            result.addAll(template.query(selectSql, (ps) -> ps.setObject(1, UUID.fromString(id)), (rs, idx) -> ExceptionUtils.wrapException(() -> {
                var object = new VersionInfo();
                fillObject(rs, object, descr, fields, Collections.emptySet());
                return object;
            })));
        }
        {
            var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(cls.getName()));
            var selectSql = "select %s from %s where %s = ?".formatted(TextUtils.join(fields, ","),
                    JdbcUtils.getTableName(cls.getName()), BaseIdentity.Fields.idName);
            result.addAll(template.query(selectSql, (ps) -> ps.setObject(1, UUID.fromString(id)), (rs, idx) -> ExceptionUtils.wrapException(() -> {
                var object = new VersionInfo();
                fillObject(rs, object, descr, fields, Collections.emptySet());
                return object;
            })));
        }
        return result;
    }

    @Override
    public <A extends BaseAsset> void deleteAsset(Class<A> aClass, String id) {
        template.update("delete from %s where id = ?".formatted(JdbcUtils.getTableName(aClass.getName())), (ps) -> ps.setObject(1, UUID.fromString(id)));
    }

    @Override
    public DatabaseObjectData loadVersion(Class<?> cls, String id, int number) {
        var fields = new LinkedHashSet<String>();
        fields.add(VersionInfo.Fields.versionNumber.toLowerCase());
        fields.add(VersionInfo.Fields.comment.toLowerCase());
        fields.add(VersionInfo.Fields.modified.toLowerCase());
        fields.add(VersionInfo.Fields.modifiedBy.toLowerCase());
        fields.add(DatabaseObjectData.Fields.data.toLowerCase());
        var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getVersionTableName(cls.getName()));
        var selectSql = "select %s from %s where %s = ? and %s = ?".formatted(TextUtils.join(fields, ","),
                JdbcUtils.getVersionTableName(cls.getName()), JdbcDatabaseMetadataProvider.OBJECT_ID_COLUMN,
                VersionInfo.Fields.versionNumber);
        var res = template.query(selectSql, (ps) -> {
            ps.setObject(1, UUID.fromString(id));
            ps.setInt(2, number);
        }, (rs, idx) -> ExceptionUtils.wrapException(() -> {
            var object = new DatabaseObjectData();
            fillObject(rs, object, descr, fields, Collections.emptySet());
            return object;
        }));
        return res.get(0);
    }

    @Override
    public void deleteVersion(Class<?> cls, String id, int number) throws Exception {
        var version = loadVersion(cls, id, number);
        template.update("delete from %s where id = ? and number = ?".formatted(JdbcUtils.getVersionTableName(cls.getName())), (ps) -> {
            ps.setObject(1, UUID.fromString(id));
            ps.setInt(2, number);
        });
        withConnection((cnn) -> dialect.deleteBlob(cnn, version.getData().id()));
    }

    @Override
    public <A extends BaseAsset> A loadAsset(Class<A> cls, String id) {
        var description = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(cls.getName()));
        Objects.requireNonNull(description);
        var columnNames = getColumnNames(description, Collections.emptySet(), Collections.singleton(DatabaseAssetWrapper.Fields.aggregatedData));
        var sql = "select %s from %s where %s = ?".formatted(TextUtils.join(columnNames, ", "), description.getName(), BaseIdentity.Fields.idName);
        var result = template.query(sql, ps -> ps.setObject(1, UUID.fromString(id)), rs -> {
            if (!rs.next()) {
                return null;
            }
            return ExceptionUtils.wrapException(() -> {
                var asset = cls.getDeclaredConstructor().newInstance();
                var handler = new AssetWrapperReflectionHandler<>(asset, domainMetaRegistry);
                fillObject(rs, handler, description, Collections.emptySet(), Collections.singleton(DatabaseAssetWrapper.Fields.aggregatedData));
                return handler;
            });
        });
        return result == null ? null : result.getWrapper().getAsset();
    }

    @Override
    public <D extends BaseDocument> DatabaseObjectData loadDocumentData(Class<D> cls, String id) {
        var fields = new LinkedHashSet<String>();
        fields.add(VersionInfo.Fields.versionNumber);
        fields.add(VersionInfo.Fields.comment);
        fields.add(VersionInfo.Fields.modified);
        fields.add(VersionInfo.Fields.modifiedBy);
        fields.add(VersionInfo.Fields.revision);
        fields.add(DatabaseObjectData.Fields.data);
        var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(cls.getName()));
        var selectSql = "select %s from %s where %s = ?".formatted(TextUtils.join(fields, ","),
                JdbcUtils.getTableName(cls.getName()), BaseIdentity.Fields.idName);
        var res = template.query(selectSql, (ps) -> ps.setObject(1, UUID.fromString(id)), (rs, idx) -> ExceptionUtils.wrapException(() -> {
            var object = new DatabaseObjectData();
            fillObject(rs, object, descr, fields, Collections.emptySet());
            return object;
        }));
        return res.isEmpty() ? null : res.get(0);
    }

    @Override
    public <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<I> searchDocuments(Class<I> projClass, SearchQuery query) throws Exception {
        Set<String> properties = new LinkedHashSet<>();
        if (!query.getPreferredFields().isEmpty()) {
            properties.add(BaseIdentity.Fields.idName);
            properties.add(BaseSearchableProjection.Fields.document);
            properties.add(BaseSearchableProjection.Fields.navigationKey);
            properties.addAll(query.getPreferredFields());
        }

        return searchObjects(projClass, () ->
                        new DatabaseSearchableProjectionWrapper<>(reflectionFactory.newInstance(projClass), domainMetaRegistry, null),
                query, properties, Collections.singleton(DatabaseSearchableProjectionWrapper.Fields.aggregatedData)).stream().map(
                DatabaseSearchableProjectionWrapper::getProjection
        ).toList();
    }

    @Override
    public <A extends BaseAsset> List<List<Object>> searchAssets(Class<A> cls, AggregationQuery updateQuery) throws Exception {
        return aggregationSearchObjects(cls, updateQuery);
    }

    @Override
    public <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<List<Object>> searchDocuments(Class<I> cls, AggregationQuery updateQuery) throws Exception {
        return aggregationSearchObjects(cls, updateQuery);
    }

    @Override
    public void updateProjections(Class<BaseSearchableProjection<BaseDocument>> projectionClass, String id, ArrayList<DatabaseSearchableProjectionWrapper<BaseDocument, BaseSearchableProjection<BaseDocument>>> wrappers, boolean update) throws Exception {
        var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(projectionClass.getName()));
        if (!update) {
            for (var proj : wrappers) {
                insert(proj,
                        JdbcUtils.getTableName(projectionClass.getName()));
            }
            return;
        }
        var selectSql = "select %s from %s where document = ?".formatted(
                TextUtils.join(getColumnNames(descr, Collections.emptySet(), Collections.emptySet()), ", "),
                descr.getName());
        var existingProjections = template.query(selectSql, (ps) -> ps.setObject(1, UUID.fromString(id)), (rs, idx) -> ExceptionUtils.wrapException(() -> {
            var res = new DatabaseSearchableProjectionWrapper<>(reflectionFactory.newInstance(projectionClass), domainMetaRegistry, null);
            fillObject(rs, res, descr, Collections.emptySet(), Collections.emptySet());
            return res;
        }));
        if (!existingProjections.isEmpty()) {
            var navKeys = new HashSet<Integer>();
            var hasAmbiquity = new AtomicReference<>(false);
            existingProjections.forEach(it -> {
                var navKey = it.getProjection().getNavigationKey();
                if (navKeys.contains(navKey)) {
                    hasAmbiquity.set(true);
                }
                navKeys.add(navKey);
            });
            if (hasAmbiquity.get()) {
                template.update("delete from %s where document = ?".formatted(descr.getName()), (ps) -> ps.setObject(1, UUID.fromString(id)));
                existingProjections.clear();
            }
        }
        var toDeleteNavigationKeys = new ArrayList<>(existingProjections.stream().map(it -> it.getProjection().getNavigationKey()).toList());
        for (var wrapper : wrappers) {
            var existing = existingProjections.stream()
                    .filter(it -> Objects.equals(it.getProjection().getNavigationKey(), wrapper.getProjection().getNavigationKey())).findFirst().orElse(null);
            if (existing == null) {
                insert(wrapper,
                        JdbcUtils.getTableName(projectionClass.getName()));
                continue;
            }
            toDeleteNavigationKeys.remove(existing.getProjection().getNavigationKey());
            Set<String> changedProperties = new LinkedHashSet<>();
            entry:
            for (var entry : descr.getFields().entrySet()) {
                var newValues = entry.getValue().getSqlValues(wrapper.getValue(entry.getKey()), enumMapper, classMapper, reflectionFactory, dialect);
                var oldValues = entry.getValue().getSqlValues(existing.getValue(entry.getKey()), enumMapper, classMapper, reflectionFactory, dialect);
                if (newValues.size() != oldValues.size()) {
                    changedProperties.add(entry.getKey());
                    continue;
                }
                for (var entry2 : oldValues.entrySet()) {
                    var newValue = newValues.get(entry2.getKey());
                    if (newValue == null ||
                            JdbcUtils.isNotEquals(entry2.getValue().getLeft(), newValue.getLeft())) {
                        changedProperties.add(entry.getKey());
                        continue entry;
                    }
                }
            }
            if (!changedProperties.isEmpty()) {
                var columnsValues = new LinkedHashMap<String, Pair<Object, JdbcFieldType>>();
                var valueExpressions = new LinkedHashMap<String, String>();
                for (var it : changedProperties) {
                    var fh = descr.getFields().get(it);
                    columnsValues.putAll(fh.getSqlValues(wrapper.getValue(it), enumMapper, classMapper, reflectionFactory, dialect));
                    valueExpressions.putAll(fh.getSqlValueExpressions(dialect));
                }
                var query = "update %s set %s where %s = ? and %s".formatted(descr.getName(),
                        TextUtils.join(columnsValues.keySet().stream().map(col -> "%s = %s".formatted(col, valueExpressions.getOrDefault(col, "?"))).toList(), ", "),
                        BaseSearchableProjection.Fields.document, existing.getProjection().getNavigationKey() == null ?
                                "%s is null".formatted(BaseSearchableProjection.Fields.navigationKey) :
                                "%s = ?".formatted(BaseSearchableProjection.Fields.navigationKey));
                template.update(query, (ps) -> {
                    int idx = 1;
                    for (Map.Entry<String, Pair<Object, JdbcFieldType>> entry : columnsValues.entrySet()) {
                        var value = entry.getValue().getLeft();
                        if (value == null) {
                            ps.setNull(idx, getSqlType(entry.getValue().getRight()));
                        } else {
                            setValue(ps, idx, entry.getValue().getRight(), entry.getValue().getLeft());
                        }
                        idx++;
                    }
                    ps.setObject(columnsValues.size() + 1, UUID.fromString(id));
                    if (existing.getProjection().getNavigationKey() != null) {
                        ps.setObject(columnsValues.size() + 2, existing.getProjection().getNavigationKey());
                    }
                });
            }
        }
        for (var navKey : toDeleteNavigationKeys) {
            template.update("delete from %s where %s = ? and %s %s".formatted(
                            descr.getName(), BaseSearchableProjection.Fields.navigationKey, BaseSearchableProjection.Fields.navigationKey,
                            navKey == null ? "is null" : "= ?"), ps -> {
                        ps.setObject(1, id);
                        if (navKey != null) {
                            ps.setInt(2, navKey);
                        }
                    }
            );
        }
    }

    @Override
    public <D extends BaseDocument> void saveDocument(String id, Class<D> aClass, DatabaseObjectData obj, DatabaseObjectData oldDocument) throws Exception {
        if (oldDocument != null) {
            template.update("delete from %s where %s = ?".formatted(JdbcUtils.getTableName(aClass.getName()), BaseIdentity.Fields.idName)
                    , (ps -> ps.setObject(1, UUID.fromString(id))));
            withConnection((cnn) -> dialect.deleteBlob(cnn, oldDocument.getData().id()));
        }
        var wrapper = new DocumentWrapperReflectionHandler(id);
        wrapper.setData(obj.getData());
        wrapper.setComment(obj.getComment());
        wrapper.setRevision(obj.getRevision());
        wrapper.setModified(obj.getModified());
        wrapper.setModifiedBy(obj.getModifiedBy());
        wrapper.setVersionNumber(obj.getVersionNumber());
        insert(wrapper, JdbcUtils.getTableName(aClass.getName()));
    }

    @Override
    public <D extends BaseDocument> void saveDocumentVersion(Class<D> aClass, String id, DatabaseObjectData version, Long oldVersionDataId) throws Exception {
        if (oldVersionDataId != null) {
            template.update("delete from %s where %s = ? and %s = ?"
                            .formatted(JdbcUtils.getVersionTableName(aClass.getName()),
                                    JdbcDatabaseMetadataProvider.OBJECT_ID_COLUMN, VersionInfo.Fields.versionNumber)
                    , ps -> {
                        ps.setObject(1, UUID.fromString(id));
                        ps.setInt(2, version.getVersionNumber());
                    });
            withConnection((cnn) -> dialect.deleteBlob(cnn, oldVersionDataId));
        }
        var handler = new VersionWrapperReflectionHandler(version, version.getData(), id);
        insert(handler, JdbcUtils.getVersionTableName(aClass.getName()));
    }

    @Override
    public <D extends BaseDocument> void deleteDocument(Class<D> aClass, String id, Long oid) throws Exception {
        template.update("delete from %s where %s = ?"
                        .formatted(JdbcUtils.getTableName(aClass.getName()),
                                BaseIdentity.Fields.idName)
                , ps -> ps.setObject(1, UUID.fromString(id)));
        withConnection((cnn) -> dialect.deleteBlob(cnn, oid));
    }

    @Override
    public <D extends BaseIdentity> List<EntityReference<D>> searchCaptions(Class<D> cls, String pattern, int limit, Locale locale) {
        var localizable = false;
        var descr = domainMetaRegistry.getDocuments().get(cls.getName());
        if (descr != null) {
            localizable = descr.getLocalizableCaptionExpression() != null;
        } else {
            localizable = domainMetaRegistry.getAssets().get(cls.getName()).getLocalizableCaptionExpression() != null;
        }
        var column = "caption";
        if (localizable) {
            column = "%sCaption".formatted(locale.getLanguage());
        }
        //noinspection unchecked
        return template.query("select %s, %s from %s %s order by %s asc limit %s".formatted(BaseIdentity.Fields.idName, column,
                JdbcUtils.getCaptionTableName(cls.getName()), TextUtils.isBlank(pattern) ? "" :
                        "where %s %s '%s%%'".formatted(column, dialect.getIlikeFunctionName(), pattern.toLowerCase().trim()), column, limit), (rs, idx) -> (EntityReference<D>) ExceptionUtils.wrapException(() -> {
            var ref = new EntityReference<>();
            ref.setId(rs.getObject(1, String.class));
            //noinspection unchecked
            ref.setType((Class<BaseIdentity>) cls);
            ref.setCaption(rs.getString(2));
            return ref;
        }));
    }

    @Override
    public <D extends BaseDocument, I extends BaseSearchableProjection<D>> void deleteProjections(Class<I> projectionClass, String id) {
        template.update("delete from %s where %s = ?".formatted(JdbcUtils.getTableName(projectionClass.getName()), BaseSearchableProjection.Fields.document), (ps) -> ps.setObject(1, UUID.fromString(id)));
    }

    @Override
    public <I extends BaseIdentity> String getCaption(Class<I> type, String id, Locale locale) {
        var result = template.query("select %sCaption from %s where %s = ?".formatted(locale.getLanguage(),
                JdbcUtils.getCaptionTableName(type.getName()), BaseIdentity.Fields.idName), (ps) -> ps.setObject(1, UUID.fromString(id)), (rs, idx) -> rs.getString(1));
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public <I extends BaseIdentity> String getCaption(Class<I> type, String id) {
        var result = template.query("select caption from %s where %s = ?".formatted(JdbcUtils.getCaptionTableName(type.getName()),
                BaseIdentity.Fields.idName), (ps) -> ps.setObject(1, UUID.fromString(id)), (rs, idx) -> rs.getString(1));
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public <VA extends BaseVirtualAsset> List<VA> searchVirtualAssets(Class<VA> cls, SearchQuery query) throws Exception {
        var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(cls.getName()));
        var vad = domainMetaRegistry.getVirtualAssets().get(cls.getName());
        var wherePart = prepareWherePart(query.getCriterions(), query.getFreeText(), descr, vad);
        var joinPartSb = new StringBuilder(prepareJoinPart(query.getOrders(), cls));
        var baseAssetTableName = JdbcUtils.getTableName(vad.getBaseAsset());
        vad.getJoins().forEach(join ->{
            joinPartSb.insert(0, "left join %s on %s=%1$s.%s%s".formatted(JdbcUtils.getTableName(join.getJoinedEntity()), join.getForeignKey(), join.getPrimaryKey(), joinPartSb.isEmpty()? "": " "));
        });
        var excludedProperties = Collections.singleton(DatabaseAssetWrapper.Fields.aggregatedData);
        Set<String> properties = new LinkedHashSet<>();
        if (!query.getPreferredFields().isEmpty()) {
            properties.add("%s.%s".formatted(baseAssetTableName, BaseIdentity.Fields.idName));
            if(JdbcUtils.isFieldIncluded(BaseVirtualAsset.Fields.versionInfo, vad.getIncludedFields(), vad.getExcludedFields())) {
                properties.add("%s.%s".formatted(baseAssetTableName, VersionInfo.Fields.revision));
                properties.add("%s.%s".formatted(baseAssetTableName, VersionInfo.Fields.comment));
                properties.add("%s.%s".formatted(baseAssetTableName, VersionInfo.Fields.modified));
                properties.add("%s.%s".formatted(baseAssetTableName, VersionInfo.Fields.modifiedBy));
                properties.add("%s.%s".formatted(baseAssetTableName, VersionInfo.Fields.versionNumber));
            }
            properties.addAll(query.getPreferredFields());
        }
        var orderPart = prepareOrderPart(query.getOrders(), cls);
        var limitPart = prepareLimitPart(query);
        var selectSql = "select %s from %s ".formatted(TextUtils.join(getVirtualAssetColumnNames(descr, baseAssetTableName, properties, excludedProperties), ", ")
                , baseAssetTableName) +
                joinPartSb + " " +
                wherePart.sql +
                orderPart +
                limitPart;
        return template.query(selectSql, createPreparedStatementSetter(wherePart), (rs, idx) -> ExceptionUtils.wrapException(() -> {
            var object = new VirtualAssetWrapperReflectionHandler<VA>(cls.getDeclaredConstructor().newInstance(), getAssetDescription(vad.getId()).getProperties().keySet());
            fillObject(rs, object, descr, properties, excludedProperties);
            return object.asset;
        }));
    }

    @Override
    public <VA extends BaseVirtualAsset> List<List<Object>> searchVirtualAssets(Class<VA> cls, AggregationQuery query) throws Exception {
        var vad = domainMetaRegistry.getVirtualAssets().get(cls.getName());
        var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(cls.getName()));
        var wherePart = prepareWherePart(query.getCriterions(), query.getFreeText(), descr, vad);
        var joinPartSb = new StringBuilder(prepareJoinPart(query.getOrders(), cls));
        var baseAssetTableName = JdbcUtils.getTableName(vad.getBaseAsset());
        vad.getJoins().forEach(join ->{
            if(!joinPartSb.isEmpty()){
                joinPartSb.append(" ");
            }
            joinPartSb.append("left join %s on %s=%1$s.%s".formatted(JdbcUtils.getTableName(join.getJoinedEntity()), join.getForeignKey(), join.getPrimaryKey()));
        });

        var selectPart = prepareProjectionSelectPart(query);
        var groupByPart = prepareProjectionGroupByPart(query);
        var orderPart = prepareOrderPart(query.getOrders(), cls);
        var selectSql = "select %s from %s %s %s %s%s".formatted(selectPart, baseAssetTableName, joinPartSb, wherePart.sql, groupByPart, orderPart);
        var searchStatement = createPreparedStatementSetter(wherePart);
        return template.query(selectSql, searchStatement, (rs, idx) -> {
            var result = new ArrayList<>();
            for (int n = 0; n < query.getAggregations().size(); n++) {
                result.add(rs.getObject(n + 1));
            }
            return result;
        });
    }

    @Override
    public <RP> RP performNativeOperation(CallableWithExceptionAndArgument<RP, ElsaTransactionContext> operation, ElsaTransactionContext ctx) throws Exception {
        ctx.setAttribute(JDBC_TEMPLATE_PARAM, template);
        ctx.setAttribute(JDBC_DIALECT, dialect);
        ctx.setAttribute(ENUM_MAPPER, enumMapper);
        return operation.call(ctx);
    }


    private <E extends BaseIntrospectableObject> List<List<Object>> aggregationSearchObjects(
            Class<E> cls, AggregationQuery query) throws Exception {
        var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(cls.getName()));
        var wherePart = prepareWherePart(query.getCriterions(), query.getFreeText(), descr, null);
        var selectPart = prepareProjectionSelectPart(query);
        var groupByPart = prepareProjectionGroupByPart(query);
        var orderPart = prepareOrderPart(query.getOrders(), cls);
        var limitPart = prepareLimitPart(query);
        var selectSql = "select %s from %s %s %s%s%s".formatted(selectPart, descr.getName(), wherePart.sql, groupByPart, orderPart, limitPart);
        var searchStatement = createPreparedStatementSetter(wherePart);
        return template.query(selectSql, searchStatement, (rs, idx) -> {
            var result = new ArrayList<>();
            for (int n = 0; n < query.getAggregations().size(); n++) {
                result.add(rs.getObject(n + 1));
            }
            return result;
        });
    }

    private String prepareProjectionGroupByPart(AggregationQuery query) {
        return TextUtils.join(query.getGroupBy().stream().map("group by %s"::formatted).toList(), ", ");
    }

    private String prepareProjectionSelectPart(AggregationQuery query) {
        var selectProperties = new ArrayList<String>();
        for (var proj : query.getAggregations()) {
            switch (proj.operation()) {
                case MAX -> selectProperties.add("max(%s)".formatted(proj.property()));
                case MIN -> selectProperties.add("min(%s)".formatted(proj.property()));
                case AVG -> selectProperties.add("avg(%s)".formatted(proj.property()));
                case SUM -> selectProperties.add("sum(%s)".formatted(proj.property()));
                case COUNT -> selectProperties.add("count(%s)".formatted(proj.property()));
                case PROPERTY -> selectProperties.add(proj.property());
            }
        }
        return TextUtils.join(selectProperties, ", ");
    }

    private void insert(BaseIntrospectableObject obj, String tableName) throws Exception {
        var description = dbMetadataProvider.getDescriptions().get(tableName);
        var columnsValues = new LinkedHashMap<String, Pair<Object, JdbcFieldType>>();
        var valueExpressions = new LinkedHashMap<String, String>();
        for (var entry : description.getFields().entrySet()) {
            columnsValues.putAll(entry.getValue().getSqlValues(obj.getValue(entry.getKey()), enumMapper, classMapper, reflectionFactory, dialect));
            valueExpressions.putAll(entry.getValue().getSqlValueExpressions(dialect));
        }
        var query = "insert into %s (%s) values (%s)".formatted(description.getName(),
                TextUtils.join(columnsValues.keySet(), ", "),
                TextUtils.join(columnsValues.keySet().stream().map(col -> valueExpressions.getOrDefault(col, "?")).toList(), ", "));
        template.update(query, (ps) -> {
            int idx = 1;
            for (Map.Entry<String, Pair<Object, JdbcFieldType>> entry : columnsValues.entrySet()) {
                var value = entry.getValue().getLeft();
                if (value == null) {
                    ps.setNull(idx, getSqlType(entry.getValue().getRight()));
                } else {
                    setValue(ps, idx, entry.getValue().getRight(), entry.getValue().getLeft());
                }
                idx++;
            }
        });
    }

    @Override
    public void updateCaptions(Class<?> aClass, String id, String caption, boolean insert) {
        if (insert) {
            template.update("insert into %s(id, caption) values (?,?)".formatted(JdbcUtils.getCaptionTableName(aClass.getName())), (ps) -> {
                ps.setObject(1, UUID.fromString(id));
                ps.setString(2, caption);
            });
            return;
        }
        template.update("update %s set caption = ? where id = ?".formatted(
                JdbcUtils.getCaptionTableName(aClass.getName())), (ps) -> {
            ps.setString(1, caption);
            ps.setObject(2, UUID.fromString(id));
        });
    }

    @Override
    public void deleteCaptions(Class<?> aClass, String id) {
        template.update("delete from %s where id = ?".formatted(JdbcUtils.getCaptionTableName(aClass.getName())), (ps) -> ps.setObject(1, UUID.fromString(id)));
    }

    @Override
    public void updateCaptions(Class<?> aClass, String id, LinkedHashMap<Locale, String> captions, boolean insert) {
        if (insert) {
            template.update("insert into %s(id, %s) values (?, %s)".formatted(JdbcUtils.getCaptionTableName(aClass.getName()),
                    TextUtils.join(captions.keySet().stream().map(it -> "%sCaption".formatted(it.getLanguage())).toList(), ", "),
                    TextUtils.join(captions.keySet().stream().map(it -> "?").toList(), ", ")
            ), (ps) -> {
                ps.setObject(1, UUID.fromString(id));
                var cpts = captions.values().stream().toList();
                for (int n = 0; n < captions.size(); n++) {
                    ps.setString(2 + n, cpts.get(n));
                }
            });
            return;
        }
        template.update("update %s set %s where id = ?".formatted(
                JdbcUtils.getCaptionTableName(aClass.getName()),
                TextUtils.join(captions.keySet().stream().map(it -> "%sCaption = ?".formatted(it.getLanguage())).toList(), ", ")
        ), (ps) -> {
            var cpts = captions.values().stream().toList();
            for (int n = 0; n < captions.size(); n++) {
                ps.setString(1 + n, cpts.get(n));
            }
            ps.setObject(cpts.size(), UUID.fromString(id));
        });
    }

    @SuppressWarnings("unchecked")
    private void setValue(PreparedStatement ps, int idx, JdbcFieldType key, Object value) {
        ExceptionUtils.wrapException(() -> {
            switch (key) {
                case LONG -> ps.setLong(idx, (long) value);
                case LONG_ARRAY -> ps.setArray(idx, ps.getConnection().createArrayOf("bigint", ((Collection<Object>) value).toArray()));
                case INT_ID, INT -> ps.setInt(idx, (int) value);
                case INT_ARRAY -> ps.setArray(idx, ps.getConnection().createArrayOf("integer", ((Collection<Object>) value).toArray()));
                case STRING, TEXT -> ps.setString(idx, (String) value);
                case STRING_ARRAY -> ps.setArray(idx, ps.getConnection().createArrayOf("varchar", ((Collection<Object>) value).toArray()));
                case BOOLEAN -> ps.setBoolean(idx, (boolean) value);
                case DATE -> ps.setDate(idx, (Date) value);
                case JSON -> ps.setObject(idx, value);
                case TEXT_SEARCH -> ps.setObject(idx, value);
                case DATE_TIME, INSTANT -> ps.setTimestamp(idx, (Timestamp) value);
                case BIG_DECIMAL -> ps.setBigDecimal(idx, (BigDecimal) value);
                case BLOB -> dialect.setBlob(ps, idx, (DatabaseBinaryData) value);
                case UUID,UUID_ID -> ps.setObject(idx, UUID.fromString(value.toString()));
                case UUID_ARRAY -> ps.setArray(idx, ps.getConnection().createArrayOf("uuid", ((Collection<String >) value).stream().map(UUID::fromString).toArray()));
            }
        });

    }

    private int getSqlType(JdbcFieldType key) {
        return switch (key) {
            case UUID_ID, UUID, UUID_ARRAY, TEXT, JSON, TEXT_SEARCH -> Types.OTHER;
            case LONG, LONG_ARRAY -> Types.BIGINT;
            case INT_ID, INT, INT_ARRAY -> Types.INTEGER;
            case STRING, STRING_ARRAY -> Types.VARCHAR;
            case BOOLEAN -> Types.BOOLEAN;
            case DATE -> Types.DATE;
            case DATE_TIME -> Types.TIMESTAMP;
            case INSTANT -> Types.TIMESTAMP_WITH_TIMEZONE;
            case BIG_DECIMAL -> Types.NUMERIC;
            case BLOB -> Types.LONGVARBINARY;
        };
    }

    private void fillObject(ResultSet rs, BaseIntrospectableObject handler, JdbcTableDescription description,
                            Set<String> includedProperties, Set<String> excludedProperties) throws Exception {
        fillObject(rs, handler, description, includedProperties, excludedProperties, null);
    }

    private void fillObject(ResultSet rs, BaseIntrospectableObject handler, JdbcTableDescription description,
                            Set<String> includedProperties, Set<String> excludedProperties, SimpleCriterion ftsCriterion) throws Exception {
        for (Map.Entry<String, JdbcFieldHandler> entry : description.getFields().entrySet()) {
            if (!isIncluded(entry.getKey(), includedProperties, excludedProperties)) {
                continue;
            }
            handler.setValue(entry.getKey(), entry.getValue().getModelValue(rs, enumMapper, classMapper, captionProvider, reflectionFactory, dialect, ftsCriterion));
        }
    }

    private SimpleCriterion findFtsCriterion(List<SearchCriterion> criterions) {
        for (var criterion : criterions) {
            if (criterion instanceof SimpleCriterion sc && sc.operation == SimpleCriterion.Operation.FTS) {
                return sc;
            }
        }
        return null;
    }
    private Set<String> getVirtualAssetColumnNames(JdbcTableDescription description, String baseAssetTableName, Set<String> includedProperties, Set<String> excludedProperties) {
        var result = new LinkedHashSet<String>();
        description.getFields().forEach((id, handler) -> {
            if (isIncluded(id, includedProperties, excludedProperties)) {
                if(BaseIdentity.Fields.idName.equals(id)){
                    result.add("%s.%s".formatted(baseAssetTableName, id));
                    return;
                }
                if(VersionInfo.Fields.revision.equals(id)){
                    result.add("%s.%s".formatted(baseAssetTableName, id));
                    return;
                }
                if(VersionInfo.Fields.versionNumber.equals(id)){
                    result.add("%s.%s".formatted(baseAssetTableName, id));
                    return;
                }
                if(VersionInfo.Fields.comment.equals(id)){
                    result.add("%s.%s".formatted(baseAssetTableName, id));
                    return;
                }
                if(VersionInfo.Fields.modified.equals(id)){
                    result.add("%s.%s".formatted(baseAssetTableName, id));
                    return;
                }
                if(VersionInfo.Fields.modifiedBy.equals(id)){
                    result.add("%s.%s".formatted(baseAssetTableName, id));
                    return;
                }
                result.addAll(handler.getColumns().keySet());
            }
        });
        return result;
    }


    private Set<String> getColumnNames(JdbcTableDescription description, Set<String> includedProperties, Set<String> excludedProperties) {
        return getColumnNames(description, includedProperties, excludedProperties, null);
    }

    private Set<String> getColumnNames(JdbcTableDescription description, Set<String> includedProperties, Set<String> excludedProperties, SimpleCriterion ftsCriterion) {
        var result = new LinkedHashSet<String>();
        description.getFields().forEach((id, handler) -> {
            if (isIncluded(id, includedProperties, excludedProperties)) {
                if(id.equals("id")){
                    result.add("%s.id".formatted(description.getName()));
                } else {
                    result.addAll(handler.getColumns().keySet());
                    if (ftsCriterion != null) {
                        var extra = handler.getExtraSelectExpression(ftsCriterion, dialect);
                        if (extra != null) {
                            result.add(extra);
                        }
                    }
                }
            }
        });
        return result;
    }

    private List<Pair<Object, JdbcFieldType>> getExtraSelectValues(JdbcTableDescription description, Set<String> includedProperties, Set<String> excludedProperties, SimpleCriterion ftsCriterion) {
        var result = new ArrayList<Pair<Object, JdbcFieldType>>();
        if (ftsCriterion == null) {
            return result;
        }
        description.getFields().forEach((id, handler) -> {
            if (isIncluded(id, includedProperties, excludedProperties) && handler.getExtraSelectExpression(ftsCriterion, dialect) != null) {
                result.add(Pair.of(ftsCriterion.value, JdbcFieldType.STRING));
            }
        });
        return result;
    }

    private boolean isIncluded(String id, Set<String> includedProperties, Set<String> excludedProperties) {
        if (!includedProperties.isEmpty() && !includedProperties.contains(id)) {
            return false;
        }
        return excludedProperties.isEmpty() || !excludedProperties.contains(id);
    }

    private <A extends BaseIntrospectableObject> List<A> searchObjects(Class<?> cls, Callable<A> factory, SearchQuery query,
                                                                       Set<String> properties, Set<String> excludedProperties) throws Exception {
        var descr = dbMetadataProvider.getDescriptions().get(JdbcUtils.getTableName(cls.getName()));
        var ftsCriterion = findFtsCriterion(query.getCriterions());
        var wherePart = prepareWherePart(query.getCriterions(), query.getFreeText(), descr, null);
        var joinPart = prepareJoinPart(query.getOrders(), cls);
        var orderPart = prepareOrderPart(query.getOrders(), cls);
        var limitPart = prepareLimitPart(query);
        var selectSql = "select %s from %s ".formatted(TextUtils.join(getColumnNames(descr, properties, excludedProperties, ftsCriterion), ", ")
                , JdbcUtils.getTableName(cls.getName())) +
                joinPart + " " +
                wherePart.sql +
                orderPart +
                limitPart;
        var extraValues = getExtraSelectValues(descr, properties, excludedProperties, ftsCriterion);
        return template.query(selectSql, createPreparedStatementSetter(extraValues, wherePart), (rs, idx) -> ExceptionUtils.wrapException(() -> {
            var object = factory.call();
            fillObject(rs, object, descr, properties, excludedProperties, ftsCriterion);
            return object;
        }));
    }

    private PreparedStatementSetter createPreparedStatementSetter(WherePartData wherePart) {
        return createPreparedStatementSetter(Collections.emptyList(), wherePart);
    }

    private PreparedStatementSetter createPreparedStatementSetter(List<Pair<Object, JdbcFieldType>> prefixValues, WherePartData wherePart) {
        return ps -> {
            var idx = 1;
            for (var item : prefixValues) {
                setValue(ps, idx++, item.getRight(), item.getLeft());
            }
            for (int n = 0; n < wherePart.values().size(); n++) {
                var item = wherePart.values.get(n);
                setValue(ps, idx++, item.getRight(), item.getLeft());
            }
        };
    }

    private String prepareLimitPart(BaseQuery query) {
        return (query.getLimit() > 0 ? " limit %s".formatted(query.getLimit()) : "") + (query.getOffset() > 0 ? " offset %s".formatted(query.getOffset()) : "");
    }

    private String prepareOrderPart(Map<String, SortOrder> orders, Class<?> cls) {
        BaseSearchableDescription descr = domainMetaRegistry.getSearchableProjections().get(cls.getName());
        if (descr == null) {
            descr = domainMetaRegistry.getAssets().get(cls.getName());
        }
        if(descr == null){
            descr = getAssetDescription(cls.getName());
        }
        var sb = new StringBuilder();
        for (var key : orders.keySet()) {
            var prop = descr.getProperties().get(key);
            if (prop != null && prop.getType() == DatabasePropertyType.ENUM) {
                if (sb.isEmpty()) {
                    sb.append(" order by ");
                } else {
                    sb.append(", ");
                }
                sb.append("%sjoin.%sname %s".formatted(key.toLowerCase(), LocaleUtils.getCurrentLocale().getLanguage(),  orders.get(key) == SortOrder.ASC ? "asc" : "desc"));
                continue;
            }
            if (prop != null && prop.getType() == DatabasePropertyType.ENTITY_REFERENCE) {
                if (sb.isEmpty()) {
                    sb.append(" order by ");
                } else {
                    sb.append(", ");
                }
                if (domainMetaRegistry.getDocuments().get(prop.getClassName()) != null && domainMetaRegistry.getDocuments().get(prop.getClassName()).getLocalizableCaptionExpression() != null) {
                    sb.append("%sjoin.%scaption".formatted(key, LocaleUtils.getCurrentLocale().getLanguage()));
                } if (domainMetaRegistry.getAssets().get(prop.getClassName()) != null && domainMetaRegistry.getAssets().get(prop.getClassName()).getLocalizableCaptionExpression() != null) {
                    sb.append("%sjoin.%scaption".formatted(key, LocaleUtils.getCurrentLocale().getLanguage()));
                } else {
                    sb.append("%sjoin.caption".formatted(key));
                }
                sb.append(" %s".formatted(orders.get(key) == SortOrder.ASC ? "asc" : "desc"));
                continue;
            }
            if (sb.isEmpty()) {
                sb.append(" order by ");
            } else {
                sb.append(", ");
            }
            sb.append("%s %s".formatted(key.toLowerCase(), orders.get(key) == SortOrder.ASC ? "asc" : "desc"));
        }
        return sb.toString();
    }

    private String prepareJoinPart(Map<String, SortOrder> orders, Class<?> cls) {
        BaseSearchableDescription descr = domainMetaRegistry.getSearchableProjections().get(cls.getName());
        if (descr == null) {
            descr = domainMetaRegistry.getAssets().get(cls.getName());
        }
        if(descr == null){
            descr = getAssetDescription(cls.getName());
        }
        if (descr == null) {
            return "";
        }
        var sb = new StringBuilder();
        for (var key : orders.keySet()) {
            var prop = descr.getProperties().get(key);
            var joinKey = key.toLowerCase();
            if(domainMetaRegistry.getVirtualAssets().containsKey(cls.getName())){
                var join = domainMetaRegistry.getVirtualAssets().get(cls.getName()).getJoins().stream().filter(it ->
                        domainMetaRegistry.getAssets().get(it.getJoinedEntity()).getProperties().containsKey(prop.getId()) &&
                                JdbcUtils.isFieldIncluded(prop.getId(), it.getIncludedFields(), it.getExcludedFields())).findFirst().orElse(null);
                if(join != null){
                    joinKey = "%s.%s".formatted(JdbcUtils.getTableName(join.getJoinedEntity()).toLowerCase(), key.toLowerCase());
                }
            }
            if (prop != null && prop.getType() == DatabasePropertyType.ENUM) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append("left join enummapping as %s on %s = %1$s.id".formatted(
                        "%sjoin".formatted(key.toLowerCase()), joinKey));
            }
            if (prop != null && prop.getType() == DatabasePropertyType.ENTITY_REFERENCE) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append("left join %s as %s on %s = %2$s.id".formatted(JdbcUtils.getCaptionTableName(prop.getClassName()),
                        "%sjoin".formatted(key.toLowerCase()), joinKey));
            }
        }

        return sb.toString();
    }

    private WherePartData prepareWherePart(List<SearchCriterion> crits, String freeTextPattern,
                                           JdbcTableDescription descr, VirtualAssetDescription vad) throws Exception {
        var criterions = new ArrayList<>(crits);
        if (!TextUtils.isBlank(freeTextPattern)) {
            for (var ptt : freeTextPattern.split(" ")) {
                if (!TextUtils.isBlank(ptt)) {
                    criterions.add(new SimpleCriterion(DatabaseSearchableProjectionWrapper.Fields.aggregatedData, SimpleCriterion.Operation.ILIKE, "%%%s%%".formatted(ptt.toLowerCase())));
                }
            }
        }
        if (criterions.isEmpty()) {
            return new WherePartData(Collections.emptyList(), "");
        }
        var values = new ArrayList<Pair<Object, JdbcFieldType>>();
        var sql = new StringBuilder();
        var indexOfSQL = new AtomicReference<>(0);
        prepareWherePartInternal(sql, values, indexOfSQL, criterions, descr, vad);
        return new WherePartData(values, "where %s".formatted(sql));
    }

    private String makeAndToken(int currentSQLIndex) {
        return currentSQLIndex > 0 ? " and " : "";
    }

    private void prepareWherePartInternal(StringBuilder sql,
                                          List<Pair<Object, JdbcFieldType>> values, AtomicReference<Integer> indexOfSQL,
                                          List<SearchCriterion> criterions,
                                          JdbcTableDescription descr, VirtualAssetDescription vad) throws Exception {
        for (var criterion : criterions) {
            if (criterion instanceof BetweenCriterion bc) {
                var currentSQLIndex = indexOfSQL.get();
                var subQuery = "%s%s between ? and ?".formatted(makeAndToken(currentSQLIndex), bc.property);
                sql.insert(currentSQLIndex, subQuery);
                indexOfSQL.set(currentSQLIndex + subQuery.length());
                values.add(getSqlQueryValue(bc.lo, descr, bc.property));
                continue;
            }
            if (criterion instanceof CheckCriterion cc) {
                switch (cc.check) {
                    case IS_EMPTY -> addCollectionCheckCriterion(sql, cc.property, "=", indexOfSQL);
                    case NOT_EMPTY -> addCollectionCheckCriterion(sql, cc.property, ">", indexOfSQL);
                    case IS_NULL -> addNullCheckCriterion(sql, cc.property, true, indexOfSQL);
                    case IS_NOT_NULL -> addNullCheckCriterion(sql, cc.property, false, indexOfSQL);
                }
                continue;
            }
            if (criterion instanceof InCriterion<?> ic) {
                var inBuilder = new StringBuilder();
                for (var value : ic.values) {
                    if (!inBuilder.isEmpty()) {
                        inBuilder.append(", ");
                    }
                    inBuilder.append("?");
                    values.add(getSqlQueryValue(value, descr, getPropertyName(ic.property, descr, vad)));
                }
                var currentSQLIndex = indexOfSQL.get();
                var subQuery = "%s%s in(%s)".formatted(makeAndToken(currentSQLIndex), getPropertyName(ic.property, descr, vad), inBuilder);
                sql.insert(currentSQLIndex, subQuery);
                indexOfSQL.set(currentSQLIndex + subQuery.length());
                continue;
            }
            if (criterion instanceof NotCriterion nc) {
                var currentSQLIndex = indexOfSQL.get();
                var subSQL = new StringBuilder();
                var subIndexOfSql = new AtomicReference<>(0);
                prepareWherePartInternal(subSQL, values, subIndexOfSql,
                        List.of(nc.criterion), descr, vad);
                sql.insert(currentSQLIndex, "%snot(%s)".formatted(makeAndToken(currentSQLIndex), subSQL));
                indexOfSQL.set(currentSQLIndex + subSQL.length());
                continue;
            }
            if (criterion instanceof JunctionCriterion jc) {
                if(jc.criterions.isEmpty()){
                    continue;
                }
                var operation = jc.disjunction ? "or" : "and";
                var currentSQLIndex = indexOfSQL.get();
                var subSQL = new StringBuilder();
                for (var subCrit : jc.criterions) {
                    if (!subSQL.isEmpty()) {
                        subSQL.append(" %s ".formatted(operation));
                    }
                    var subSubSql = new StringBuilder();
                    var subIndexOfSql = new AtomicReference<>(0);
                    prepareWherePartInternal(subSubSql, values, subIndexOfSql,
                            List.of(subCrit),
                            descr, vad);
                    subSQL.append(subSubSql);
                }
                String subSqlStr = "%s(%s)".formatted(makeAndToken(currentSQLIndex), subSQL);
                sql.insert(currentSQLIndex, subSqlStr);
                indexOfSQL.set(currentSQLIndex + subSqlStr.length());
                continue;
            }
            if (criterion instanceof SimpleCriterion sc) {
                switch (sc.operation) {
                    case EQ -> addSimpleCriterion(sql, values, sc.property, "=",
                            sc.value, indexOfSQL, descr);
                    case NE -> addSimpleCriterion(sql, values, sc.property, "!=",
                            sc.value, indexOfSQL, descr);
                    case LIKE -> addSimpleCriterion(sql, values, sc.property, "like",
                            sc.value, indexOfSQL, descr);
                    case ILIKE -> addSimpleCriterion(sql, values, sc.property, dialect.getIlikeFunctionName(),
                            sc.value, indexOfSQL, descr);
                    case GT -> addSimpleCriterion(sql, values, sc.property, ">",
                            sc.value, indexOfSQL, descr);
                    case LT -> addSimpleCriterion(sql, values, sc.property, "<",
                            sc.value, indexOfSQL, descr);
                    case GE -> addSimpleCriterion(sql, values, sc.property, ">=",
                            sc.value, indexOfSQL, descr);
                    case LE -> addSimpleCriterion(sql, values, sc.property, "<=",
                            sc.value, indexOfSQL, descr);
                    case CONTAINS -> {
                        if(dialect.hasArraysAnyOperationSupport()) {
                            var currentSQLIndex = indexOfSQL.get();
                            var subQuery = "%s ? = ANY(%s)".formatted(makeAndToken(currentSQLIndex), sc.property);
                            sql.insert(currentSQLIndex, subQuery);
                            indexOfSQL.set(currentSQLIndex + subQuery.length());
                            values.add(getSqlQueryValue(sc.value, descr, sc.property));
                        } else {
                            var currentSQLIndex = indexOfSQL.get();
                            var subQuery = "%s ? in(unnest(%s))".formatted(makeAndToken(currentSQLIndex), sc.property);
                            sql.insert(currentSQLIndex, subQuery);
                            indexOfSQL.set(currentSQLIndex + subQuery.length());
                            values.add(getSqlQueryValue(sc.value, descr, sc.property));
                        }
                    }
                    case ANY_IN -> createArrayInCriterion(sc, true, indexOfSQL, sql, descr, vad, values);
                    case NONE_IN -> createArrayInCriterion(sc, false, indexOfSQL, sql, descr, vad, values);
                    case FTS -> {
                        var handler = descr.getFields().get(sc.property);
                        var expr = handler.getFtsWhereExpression(sc, dialect);
                        var currentSQLIndex = indexOfSQL.get();
                        var subQuery = "%s%s".formatted(makeAndToken(currentSQLIndex), expr.getLeft());
                        sql.insert(currentSQLIndex, subQuery);
                        indexOfSQL.set(currentSQLIndex + subQuery.length());
                        values.addAll(expr.getRight());
                    }
                }
                continue;
            }
            throw new IllegalArgumentException("unsupported criterion type " + criterion.getClass().getName());
        }
    }

    private void createArrayInCriterion(SimpleCriterion sc, boolean value, AtomicReference<Integer> indexOfSQL, StringBuilder sql, JdbcTableDescription descr, VirtualAssetDescription vad, List<Pair<Object, JdbcFieldType>> values) throws Exception {
        if(dialect.hasArraysIntersectionOperationSupport()) {
            var currentSQLIndex = indexOfSQL.get();
            var subQuery = "%s (? && %s) = %s".formatted(makeAndToken(currentSQLIndex), sc.property, value);
            sql.insert(currentSQLIndex, subQuery);
            indexOfSQL.set(currentSQLIndex + subQuery.length());
            values.add(getSqlQueryValue(sc.value, descr, sc.property));
            return;
        }
        var items = (Collection<?>)sc.value;
        var crits = items.stream().map(it -> (SearchCriterion)new SimpleCriterion(sc.property, SimpleCriterion.Operation.CONTAINS, it)).toList();
        SearchCriterion crit = value? SearchCriterion.or(crits): SearchCriterion.not(SearchCriterion.or(crits));
        prepareWherePartInternal(sql, values, indexOfSQL,List.of(crit), descr, vad);
    }

    private String getPropertyName(String property, JdbcTableDescription descr, VirtualAssetDescription vad) {
        if(vad != null  && BaseIdentity.Fields.idName.equals(property)){
            return "%s.id".formatted(JdbcUtils.getTableName(vad.getBaseAsset()));
        }
        return property;
    }

    private void addNullCheckCriterion(StringBuilder sql, String property, boolean isNull, AtomicReference<Integer> indexOfSQL) {
        var currentSQLIndex = indexOfSQL.get();
        var subQuery = "%s%s is %s".formatted(makeAndToken(currentSQLIndex), property, isNull ? "null" : "not null");
        sql.insert(currentSQLIndex, subQuery);
        indexOfSQL.set(currentSQLIndex + subQuery.length());
    }


    private void addCollectionCheckCriterion(StringBuilder sql,
                                             String property,
                                             String operation,
                                             AtomicReference<Integer> indexOfSql) {
        var currentSQLIndex = indexOfSql.get();
        var subQuery = "%s%s %s 0".formatted(makeAndToken(currentSQLIndex), dialect.getCardinalitySql(property), operation);
        sql.insert(currentSQLIndex, subQuery);
        indexOfSql.set(currentSQLIndex + subQuery.length());
    }

    private void addSimpleCriterion(StringBuilder sql,
                                    List<Pair<Object, JdbcFieldType>> values,
                                    String property,
                                    String operation,
                                    Object value,
                                    AtomicReference<Integer> indexOfSql,
                                    JdbcTableDescription descr) throws Exception {
        var currentSQLIndex = indexOfSql.get();
        var subQuery = "%s%s %s ?".formatted(makeAndToken(currentSQLIndex), property, operation);
        sql.insert(currentSQLIndex, subQuery);
        indexOfSql.set(currentSQLIndex + subQuery.length());
        values.add(getSqlQueryValue(value, descr, property));
    }

    private Pair<Object, JdbcFieldType> getSqlQueryValue(Object value, JdbcTableDescription descr, String propertyName) throws Exception {
        var fieldId = propertyName.contains(".")? propertyName.substring(propertyName.lastIndexOf(".")+1): propertyName;
        var field =  descr.getFields().get(fieldId);
        return field.getSqlQueryValue(value, enumMapper, classMapper, reflectionFactory);
    }

    private void withConnection(RunnableWithExceptionAndArgument<Connection> callback) throws Exception {
        Connection con = DataSourceUtils.getConnection(template.getDataSource());
        try {
            callback.run(con);
        } finally {
            DataSourceUtils.releaseConnection(con, template.getDataSource());
        }
    }

    static class AssetWrapperReflectionHandler<A extends BaseAsset> extends BaseIntrospectableObject {
        private final DatabaseAssetWrapper<A> wrapper;

        private final AssetDescription assetDescr;

        AssetWrapperReflectionHandler(DatabaseAssetWrapper<A> wrapper, DomainMetaRegistry registry) {
            this.wrapper = wrapper;
            this.assetDescr = registry.getAssets().get(wrapper.getAsset().getClass().getName());
        }

        AssetWrapperReflectionHandler(A asset, DomainMetaRegistry registry) {
            this.wrapper = new DatabaseAssetWrapper<>();
            wrapper.setAsset(asset);
            this.assetDescr = registry.getAssets().get(wrapper.getAsset().getClass().getName());
            wrapper.getAsset().setVersionInfo(new VersionInfo());
        }

        public DatabaseAssetWrapper<A> getWrapper() {
            return wrapper;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setValue(String propertyName, Object value) {
            if (VersionInfo.Fields.revision.equals(propertyName)) {
                wrapper.getAsset().getVersionInfo().setValue("revision", value);
                return;
            }
            if (VersionInfo.Fields.modifiedBy.equals(propertyName)) {
                wrapper.getAsset().getVersionInfo().setModifiedBy((String) value);
                return;
            }
            if (VersionInfo.Fields.modified.equals(propertyName)) {
                wrapper.getAsset().getVersionInfo().setModified((Instant) value);
                return;
            }
            if (VersionInfo.Fields.comment.equals(propertyName)) {
                wrapper.getAsset().getVersionInfo().setComment((String) value);
                return;
            }
            if (VersionInfo.Fields.versionNumber.equals(propertyName)) {
                wrapper.getAsset().getVersionInfo().setVersionNumber((Integer) value);
                return;
            }
            if (DatabaseAssetWrapper.Fields.aggregatedData.equals(propertyName)) {
                wrapper.setAggregatedData((String) value);
                return;
            }
            if (BaseIdentity.Fields.idName.equals(propertyName)) {
                wrapper.getAsset().setId(((UUID) value).toString());
                return;
            }
            if (assetDescr.getProperties().containsKey(propertyName)) {
                wrapper.getAsset().setValue(propertyName, value);
                return;
            }

            var coll = (Collection<Object>) wrapper.getAsset().getCollection(propertyName);
            coll.clear();
            coll.addAll((Collection<Object>) value);

        }

        @Override
        public Object getValue(String propertyName) {
            if (VersionInfo.Fields.revision.equals(propertyName)) {
                return wrapper.getAsset().getVersionInfo().getRevision();
            }
            if (VersionInfo.Fields.modifiedBy.equals(propertyName)) {
                return wrapper.getAsset().getVersionInfo().getModifiedBy();
            }
            if (VersionInfo.Fields.modified.equals(propertyName)) {
                return wrapper.getAsset().getVersionInfo().getModified();
            }
            if (VersionInfo.Fields.comment.equals(propertyName)) {
                return wrapper.getAsset().getVersionInfo().getComment();
            }
            if (VersionInfo.Fields.versionNumber.equals(propertyName)) {
                return wrapper.getAsset().getVersionInfo().getVersionNumber();
            }
            if (BaseIdentity.Fields.idName.equals(propertyName)) {
                return wrapper.getAsset().getId();
            }
            if (DatabaseAssetWrapper.Fields.aggregatedData.equals(propertyName)) {
                return wrapper.getAggregatedData();
            }
            if (assetDescr.getProperties().containsKey(propertyName)) {
                return wrapper.getAsset().getValue(propertyName);
            }
            return wrapper.getAsset().getCollection(propertyName);
        }
    }


    static class VirtualAssetWrapperReflectionHandler<A extends BaseVirtualAsset> extends BaseIntrospectableObject {

        private final A asset;

        private final Set<String> propertiesIds;
        VirtualAssetWrapperReflectionHandler(A asset, Set<String> properties) {
            this.asset = asset;
            this.propertiesIds = properties;
            if(asset.getVersionInfo() == null){
                asset.setVersionInfo(new VersionInfo());
            }
        }

        @Override
        public void setValue(String propertyName, Object value) {
            if (VersionInfo.Fields.revision.equals(propertyName)) {
                asset.getVersionInfo().setValue("revision", value);
                return;
            }
            if (VersionInfo.Fields.modifiedBy.equals(propertyName)) {
                asset.getVersionInfo().setModifiedBy((String) value);
                return;
            }
            if (VersionInfo.Fields.modified.equals(propertyName)) {
                asset.getVersionInfo().setModified((Instant) value);
                return;
            }
            if (VersionInfo.Fields.comment.equals(propertyName)) {
                asset.getVersionInfo().setComment((String) value);
                return;
            }
            if (VersionInfo.Fields.versionNumber.equals(propertyName)) {
                asset.getVersionInfo().setVersionNumber((Integer) value);
                return;
            }
            if (BaseIdentity.Fields.idName.equals(propertyName)) {
                asset.setId(((UUID) value).toString());
                return;
            }
            if (propertiesIds.contains(propertyName)) {
                asset.setValue(propertyName, value);
                return;
            }

            var coll = (Collection<Object>) asset.getCollection(propertyName);
            coll.clear();
            coll.addAll((Collection<Object>) value);

        }

        @Override
        public Object getValue(String propertyName) {
            return null;
        }
    }


    static class DocumentWrapperReflectionHandler extends DatabaseObjectData {
        private String id;

        DocumentWrapperReflectionHandler(String id) {
            this.id = id;
        }

        @Override
        public void setValue(String propertyName, Object value) {
            if (BaseIdentity.Fields.idName.equals(propertyName)) {
                id = (String) value;
                return;
            }
            super.setValue(propertyName, value);
        }

        @Override
        public Object getValue(String propertyName) {
            if (BaseIdentity.Fields.idName.equals(propertyName)) {
                return id;
            }
            return super.getValue(propertyName);
        }
    }

    static class VersionWrapperReflectionHandler extends BaseIntrospectableObject {
        private final VersionInfo info;

        private DatabaseBinaryData data;

        private String objectid;

        VersionWrapperReflectionHandler(VersionInfo info, DatabaseBinaryData data, String objectid) {
            this.info = info;
            this.data = data;
            this.objectid = objectid;
        }

        @Override
        public void setValue(String propertyName, Object value) {
            if (DatabaseObjectData.Fields.data.equals(propertyName)) {
                data = (DatabaseBinaryData) value;
                return;
            }
            if (JdbcDatabaseMetadataProvider.OBJECT_ID_COLUMN.equals(propertyName)) {
                objectid = (String) value;
                return;
            }
            info.setValue(propertyName, value);
        }

        @Override
        public Object getValue(String propertyName) {
            if (DatabaseObjectData.Fields.data.equals(propertyName)) {
                return data;
            }
            if (JdbcDatabaseMetadataProvider.OBJECT_ID_COLUMN.equals(propertyName)) {
                return objectid;
            }
            return info.getValue(propertyName);
        }
    }



    record WherePartData(List<Pair<Object, JdbcFieldType>> values, String sql) {
    }

}
