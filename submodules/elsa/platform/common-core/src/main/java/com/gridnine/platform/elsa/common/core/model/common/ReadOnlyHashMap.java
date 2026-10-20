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

package com.gridnine.platform.elsa.common.core.model.common;

import java.util.HashMap;
import java.util.Map;

public class ReadOnlyHashMap<K, T> extends HashMap<K, T> {
    private boolean allowChanges = false;

    public void setAllowChanges(boolean allowChanges) {
        this.allowChanges = allowChanges;
    }

    @Override
    public T put(K key, T value) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        return super.put(key, value);
    }

    @Override
    public void clear() {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        super.clear();
    }

    @Override
    public T remove(Object key) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        return super.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends T> m) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        super.putAll(m);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        return super.remove(key, value);
    }
}
