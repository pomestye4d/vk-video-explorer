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

package com.gridnine.platform.elsa.common.core.search;

public class AggregationQueryBuilder extends BaseQueryBuilder<AggregationQueryBuilder, AggregationQuery> {

    public AggregationQueryBuilder() {
        super(new AggregationQuery());
    }

    public AggregationQueryBuilder count() {
        query.getAggregations().add(new Aggregation("*", Aggregation.Operation.COUNT));
        return this;
    }

    public<T extends FieldNameSupport> AggregationQueryBuilder property(T property) {
        query.getAggregations().add(new Aggregation(property.name, Aggregation.Operation.PROPERTY));
        return this;
    }

    public <T extends FieldNameSupport & NumberOperationsSupport> AggregationQueryBuilder sum(T property) {
        query.getAggregations().add(new Aggregation(property.name, Aggregation.Operation.SUM));
        return this;
    }

    public <T extends FieldNameSupport & ComparisonSupport> AggregationQueryBuilder min(T property) {
        query.getAggregations().add(new Aggregation(property.name, Aggregation.Operation.MIN));
        return this;
    }

    public <T extends FieldNameSupport & ComparisonSupport> AggregationQueryBuilder max(T property) {
        query.getAggregations().add(new Aggregation(property.name, Aggregation.Operation.MAX));
        return this;
    }

    public <T extends FieldNameSupport & NumberOperationsSupport> AggregationQueryBuilder avg(T property) {
        query.getAggregations().add(new Aggregation(property.name, Aggregation.Operation.AVG));
        return this;
    }

    public <T extends FieldNameSupport> AggregationQueryBuilder groupBy(T property) {
        query.getGroupBy().add(property.name);
        return this;
    }

    public  AggregationQueryBuilder offset(int offset) {
        query.setOffset(offset);
        return this;
    }


    public AggregationQueryBuilder limit(int limit) {
        query.setLimit(limit);
        return this;
    }

    public AggregationQueryBuilder orderBy(String expression, SortOrder order) {
        query.getOrders().put(expression, order);
        return this;
    }
}
