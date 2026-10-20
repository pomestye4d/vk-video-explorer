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

package com.gridnine.platform.elsa.common.core.lock;

import com.gridnine.platform.elsa.common.core.model.common.RunnableWithException;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface LockManager {
    <T> T withLock(Object obj, long tryTime, TimeUnit timeUnit, Callable<T> func);

    default <T> T withLock(Object obj, Callable<T> func) {
        return withLock(obj, 1, TimeUnit.MINUTES, func);
    }

    default void withLock(Object obj, RunnableWithException func) {
        withLock(obj, 1, TimeUnit.MINUTES, () -> {
            func.run();
            return null;
        });
    }

}
