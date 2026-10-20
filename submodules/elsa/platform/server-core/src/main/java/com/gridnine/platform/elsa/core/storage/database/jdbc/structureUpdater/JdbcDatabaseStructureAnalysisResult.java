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

package com.gridnine.platform.elsa.core.storage.database.jdbc.structureUpdater;

import com.gridnine.platform.elsa.common.core.utils.TextUtils;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcSequenceDescription;

import java.util.Set;
import java.util.stream.Collectors;

public record JdbcDatabaseStructureAnalysisResult(Set<String> tablesToDelete, Set<JdbcCreateTableData> tablesToCreate,
                                                  Set<JdbcUpdateTableData> tablesToUpdate,
                                                  Set<String> sequencesToDelete,
                                                  Set<JdbcSequenceDescription> sequencesToCreate) {

    @Override
    public String toString() {
        if (tablesToDelete.isEmpty() && tablesToCreate.isEmpty()
                && tablesToUpdate.isEmpty()) {
            return "DatabaseStructureAnalysisResult: nothing to change";
        }
        var result = new StringBuilder("DatabaseStructureAnalysisResult:");
        result.append("\ntable to delete: %s".formatted(tablesToDelete));
        result.append("\ntable to create:");
        for (var table : tablesToCreate) {
            result.append("\n%s".formatted(table.tableName()));
            result.append("\n\tcolumns:%s".formatted(TextUtils.join(table.columns().entrySet().stream().map(it ->
                    "\n\t\t%s: %s".formatted(it.getKey(), it.getValue())).collect(Collectors.toList()), ", ")));
            result.append("\n\tindexes:%s".formatted(TextUtils.join(table.indexes().entrySet().stream().map(it ->
                    "\n\t\t%s: %s".formatted(it.getKey(), it.getValue())).collect(Collectors.toList()), ", ")));
        }
        result.append("\ntable to update:");
        for (var table : tablesToUpdate) {
            result.append("\n%s".formatted(table.tableName()));
            result.append("\n\tcolumns to delete:%s".formatted(TextUtils.join(table.columnsToDelete().stream()
                    .map("\n\t\t%s"::formatted).collect(Collectors.toList()), ", ")));
            result.append("\n\tindexes to delete:%s".formatted(TextUtils.join(table.indexesToDelete().stream()
                    .map("\n\t\t%s"::formatted).collect(Collectors.toList()), ", ")));
            result.append("\n\tindexes to create:%s".formatted(TextUtils.join(table.indexesToCreate().entrySet().stream().map(it ->
                    "\n\t\t%s: %s".formatted(it.getKey(), it.getValue())).collect(Collectors.toList()), ", ")));
            result.append("\n\tcolumns to create:%s".formatted(TextUtils.join(table.columnsToCreate().entrySet().stream().map(it ->
                    "\n\t\t%s: %s".formatted(it.getKey(), it.getValue())).collect(Collectors.toList()), ", ")));
            result.append("\n\tindexes to create:%s".formatted(TextUtils.join(table.indexesToCreate().entrySet().stream().map(it ->
                    "\n\t\t%s: %s".formatted(it.getKey(), it.getValue())).collect(Collectors.toList()), ", ")));
        }
        result.append("\nsequences to delete: %s".formatted(sequencesToDelete));
        result.append("\nsequences to create:");
        for (var sequence : sequencesToCreate) {
            result.append("\n%s as %s".formatted(sequence.sequenceName(), sequence.type()));
        }
        return result.toString();
    }

}
