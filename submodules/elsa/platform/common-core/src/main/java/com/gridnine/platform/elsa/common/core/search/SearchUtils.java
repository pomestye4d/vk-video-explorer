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

import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SearchUtils {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String valueToString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        if (value instanceof LocalDateTime ld) {
            return dtf.format(ld);
        }
        if (value instanceof LocalDate ld) {
            return df.format(ld);
        }
        if (value instanceof EntityReference<?> ld) {
            return ld.getCaption() != null ? ld.getCaption() : "%s %s".formatted(ld.getType().getName(), ld.getId());
        }
        return value.toString();
    }
}
