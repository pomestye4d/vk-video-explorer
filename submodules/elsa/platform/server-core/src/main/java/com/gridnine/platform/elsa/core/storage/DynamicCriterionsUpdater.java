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

package com.gridnine.platform.elsa.core.storage;

import com.gridnine.platform.elsa.common.core.search.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicCriterionsUpdater {
    private final ConcurrentHashMap<String, DynamicCriterionHandler<Object>> criterionHandlersCache = new ConcurrentHashMap<>();

    private final List<DynamicCriterionHandler<Object>> handlers;

    public DynamicCriterionsUpdater(List<DynamicCriterionHandler<Object>> handlers) {
        this.handlers = handlers;
    }

    public <T extends BaseQuery> T updateQuery(String listId, T query) {
        updateQueryInternal(listId, query.getCriterions());
        return query;
    }

    private SearchCriterion getCriterionInternal(String listId, SearchCriterion crit) {
        if (crit instanceof DynamicCriterion dc) {
            var key = "%s||%s||%s".formatted(listId, dc.propertyId, dc.conditionId);
            var handler = handlers.stream().filter(dch -> dch.isApplicable(listId, dc.propertyId)).findFirst().orElseThrow();
            return handler.getCriterion(listId, dc.propertyId, dc.conditionId, dc.value);
        }
        if (crit instanceof JunctionCriterion jc) {
            var subcrits = new ArrayList<>(jc.criterions);
            if (updateQueryInternal(listId, subcrits)) {
                return new JunctionCriterion(jc.disjunction, subcrits);
            }
        }
        if (crit instanceof NotCriterion nc) {
            var modifiedCrit = getCriterionInternal(listId, nc.criterion);
            if (modifiedCrit != null) {
                return new NotCriterion(modifiedCrit);
            }
        }
        return null;
    }

    private boolean updateQueryInternal(String listId, List<SearchCriterion> criterions) {
        var modified = false;
        var updatedCriterions = new HashMap<SearchCriterion, SearchCriterion>();
        for (SearchCriterion crit : criterions) {
            var modifiedCrit = getCriterionInternal(listId, crit);
            if (modifiedCrit != null) {
                updatedCriterions.put(crit, modifiedCrit);
                modified = true;
            }
        }
        updatedCriterions.forEach((sourceCrit, destCrit) -> {
            var idx = criterions.indexOf(sourceCrit);
            criterions.set(idx, destCrit);
        });
        return modified;
    }
}
