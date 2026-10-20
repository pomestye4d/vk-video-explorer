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

import com.gridnine.platform.elsa.common.core.utils.TextUtils;
import com.gridnine.platform.elsa.common.meta.domain.AssetDescription;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.VirtualAssetDescription;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class JdbcUtils {
    public static String getTableName(String id) {
        var idx = id.lastIndexOf(".");
        return idx == -1 ? id.toLowerCase() : id.substring(idx + 1).toLowerCase();
    }

    public static String getVersionTableName(String id) {
        var idx = id.lastIndexOf(".");
        return (idx == -1 ? id.toLowerCase() : id.substring(idx + 1).toLowerCase()) + "version";
    }

    public static String getCaptionTableName(String id) {
        var idx = id.lastIndexOf(".");
        return (idx == -1 ? id.toLowerCase() : id.substring(idx + 1).toLowerCase()) + "caption";
    }

    public static boolean isNull(ResultSet rs, String propertyName) throws SQLException {
        return rs.getObject(propertyName) == null;
    }

    public static boolean isNotEquals(Object obj1, Object obj2) throws SQLException {
        if (obj1 == null) {
            return obj2 != null;
        }
        if (obj1 instanceof Array) {
            if (!(obj2 instanceof Array)) {
                return true;
            }
            var arr1 = (Object[]) ((Array) obj1).getArray();
            var arr2 = (Object[]) ((Array) obj2).getArray();
            if (arr1.length != arr2.length) {
                return true;
            }
            for (int n = 0; n < arr1.length; n++) {
                if (!Objects.equals(arr1[n], arr2[n])) {
                    return true;
                }
            }
            return false;
        }
        return !obj1.equals(obj2);
    }

    private static boolean isFieldMatchesPattern(String fieldId, String excludedFields) {
        for (String item : excludedFields.split(";")) {
            if (!TextUtils.isBlank(item) && fieldId.matches(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFieldIncluded(String fieldId, String includedFields, String excludedFields) {
        if (!TextUtils.isBlank(excludedFields) && isFieldMatchesPattern(fieldId, excludedFields)) {
            return false;
        }
        return TextUtils.isBlank(includedFields) || isFieldMatchesPattern(fieldId, includedFields);
    }

    public static AssetDescription createAssetDescription(VirtualAssetDescription va, DomainMetaRegistry registry) {
        var assetDescription = new AssetDescription(va.getId());
        var baseAsset = registry.getAssets().get(va.getBaseAsset());
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
            var joinedAsset = registry.getAssets().get(join.getJoinedEntity());
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

    private JdbcUtils() {
    }

}
