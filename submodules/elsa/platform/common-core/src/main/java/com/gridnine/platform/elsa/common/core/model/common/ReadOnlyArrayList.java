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

import java.util.ArrayList;
import java.util.Collection;

public class ReadOnlyArrayList<T> extends ArrayList<T> {
    private boolean allowChanges = false;

    public void setAllowChanges(boolean allowChanges) {
        this.allowChanges = allowChanges;
    }

    @Override
    public boolean add(T t) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        return super.add(t);
    }

    @Override
    public void clear() {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        super.clear();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        return super.addAll(c);
    }

    @Override
    public void add(int index, T element) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        super.add(index, element);
    }

    @Override
    public T remove(int index) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        return super.remove(index);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        return super.removeAll(c);
    }

    @Override
    public boolean remove(Object o) {
        if (!allowChanges) {
            throw Xeption.forDeveloper("changes are not allowed");
        }
        return super.remove(o);
    }
}
