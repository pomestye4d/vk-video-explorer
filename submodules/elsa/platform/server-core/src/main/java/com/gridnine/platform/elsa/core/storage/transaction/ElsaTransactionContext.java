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

package com.gridnine.platform.elsa.core.storage.transaction;

import com.gridnine.platform.elsa.common.core.model.common.RunnableWithException;
import com.gridnine.platform.elsa.common.core.utils.TypedParameterId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ElsaTransactionContext {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final List<RunnableWithException> postCommitCallbacks = new ArrayList<>();

    public List<RunnableWithException> getPostCommitCallbacks() {
        return postCommitCallbacks;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public <T> T getAttribute(final TypedParameterId<T> attributeId) {
        //noinspection unchecked
        return (T) attributes.get(attributeId.getId());
    }

    public <T> void setAttribute(final TypedParameterId<T> attributeId, final T value) {
        attributes.put(attributeId.getId(), value);
    }
}
