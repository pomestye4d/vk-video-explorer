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

import java.util.Arrays;
import java.util.List;

public abstract class SearchCriterion {

    public static <A, T extends FieldNameSupport & EqualitySupport & ArgumentType<A>> SimpleCriterion eq(T property, A value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.EQ, value);
    }

    public static <A, T extends FieldNameSupport & EqualitySupport> SimpleCriterion ne(T property, Object value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.NE, value);
    }

    public static <T extends FieldNameSupport & StringOperationsSupport> SimpleCriterion like(T property, String value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.LIKE, value);
    }

    public static <T extends FieldNameSupport & StringOperationsSupport> SimpleCriterion ilike(T property, String value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.ILIKE, value);
    }

    public static <T extends FieldNameSupport & FullTextSearchSupport> SimpleCriterion fts(T property, String phrase) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.FTS, phrase);
    }

    public static <A, T extends FieldNameSupport & ComparisonSupport & ArgumentType<A>> SimpleCriterion gt(T property, A value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.GT, value);
    }

    public static <A, T extends FieldNameSupport & ComparisonSupport & ArgumentType<A>> SimpleCriterion ge(T property, A value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.GE, value);
    }

    public static <A, T extends FieldNameSupport & ComparisonSupport & ArgumentType<A>> SimpleCriterion le(T property, A value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.LE, value);
    }

    public static <A, T extends FieldNameSupport & ComparisonSupport & ArgumentType<A>> SimpleCriterion lt(T property, A value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.LT, value);
    }

    public static <A, T extends FieldNameSupport & CollectionSupport & ArgumentType<A>> SimpleCriterion contains(T property, A value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.CONTAINS, value);
    }

    public static <A, T extends FieldNameSupport & CollectionSupport & ArgumentType<A>> SimpleCriterion anyIn(T property, List<A> value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.ANY_IN, value);
    }

    public static <A, T extends FieldNameSupport & CollectionSupport & ArgumentType<A>> SimpleCriterion noneIn(T property, List<A> value) {
        return new SimpleCriterion(property.name, SimpleCriterion.Operation.NONE_IN, value);
    }

    public static <T extends FieldNameSupport & CollectionSupport> CheckCriterion isEmpty(T property) {
        return new CheckCriterion(property.name, CheckCriterion.Check.IS_EMPTY);
    }

    public static <T extends FieldNameSupport & CollectionSupport> CheckCriterion isNotEmpty(T property) {
        return new CheckCriterion(property.name, CheckCriterion.Check.NOT_EMPTY);
    }

    public static <T extends FieldNameSupport> CheckCriterion isNull(T property) {
        return new CheckCriterion(property.name, CheckCriterion.Check.IS_NULL);
    }

    public static <T extends FieldNameSupport> CheckCriterion isNotNull(T property) {
        return new CheckCriterion(property.name, CheckCriterion.Check.IS_NOT_NULL);
    }

    public static <A, T extends FieldNameSupport & ComparisonSupport & ArgumentType<A>> BetweenCriterion between(T property, A lo, A hi) {
        return new BetweenCriterion(property.name, lo, hi);
    }

    public static <A, T extends FieldNameSupport & EqualitySupport & ArgumentType<A>> InCriterion<A> inCollection(T property, List<A> values) {
        return new InCriterion<>(property.name, values);
    }

    @SafeVarargs
    public static <A, T extends FieldNameSupport & EqualitySupport & ArgumentType<A>> InCriterion<A> inCollection(T property, A... values) {
        return new InCriterion<>(property.name, Arrays.asList(values));
    }

    public static JunctionCriterion and(List<SearchCriterion> crits) {
        return new JunctionCriterion(false, crits);
    }

    public static JunctionCriterion and(SearchCriterion... crits) {
        return new JunctionCriterion(false, Arrays.asList(crits));
    }

    public static JunctionCriterion or(List<SearchCriterion> crits) {
        return new JunctionCriterion(true, crits);
    }

    public static JunctionCriterion or(SearchCriterion... crits) {
        return new JunctionCriterion(true, Arrays.asList(crits));
    }

    public static NotCriterion not(SearchCriterion crit) {
        return new NotCriterion(crit);
    }

}
