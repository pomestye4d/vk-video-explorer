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

import com.gridnine.platform.elsa.common.core.utils.TextUtils;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class AggregatedData {

    private final Set<String> data = new LinkedHashSet<>();

    private final DomainMetaRegistry registry;

    public AggregatedData(DomainMetaRegistry registry) {
        this.registry = registry;
    }

    public void aggregate(Object obj) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Enum<?> en) {
            var enumDescr = registry.getEnums().get(obj.getClass().getName());
            if (enumDescr == null) {
                data.add(en.name().toLowerCase());
                return;
            }
            var descr = enumDescr.getItems().get(en.name());
            if (descr == null) {
                data.add(en.name().toLowerCase());
                return;
            }
            data.addAll(descr.getDisplayNames().values().stream().map(String::toLowerCase).toList());
            return;
        }
        if (obj instanceof Collection<?> coll) {
            for (Object elm : coll) {
                aggregate(elm);
            }
            return;
        }
        data.add(obj.toString().toLowerCase());
    }

    public String getAggregatedData() {
        return TextUtils.join(data, " ");
    }

}
