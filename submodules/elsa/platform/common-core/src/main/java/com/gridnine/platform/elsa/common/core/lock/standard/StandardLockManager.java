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

package com.gridnine.platform.elsa.common.core.lock.standard;

import com.gridnine.platform.elsa.common.core.lock.LockManager;
import com.gridnine.platform.elsa.common.core.lock.NamedLock;
import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class StandardLockManager implements LockManager {

    private final ReentrantLock masterLock = new ReentrantLock();

    private final Map<String, LocalLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(Object obj, long tryTime, TimeUnit timeUnit, Callable<T> func) {
        return ExceptionUtils.wrapException(() -> {
            var lockName = getLockName(obj);
            try (var loc = getLock(lockName)) {
                if (!loc.tryLock(tryTime, timeUnit)) {
                    throw Xeption.forDeveloper("unable to get lock %s during %s %s".formatted(lockName, tryTime, timeUnit));
                }
                try {
                    return func.call();
                } finally {
                    loc.unlock();
                }
            }
        });

    }

    private String getLockName(Object obj) {
        if (obj instanceof BaseIdentity bi) {
            return "%s-%s".formatted(bi.getClass().getName(), bi.getId());
        }
        if (obj instanceof String st) {
            return st;
        }
        return "%s-%s".formatted(obj.getClass().getName(), obj.hashCode());
    }

    private NamedLock getLock(String name) {
        masterLock.lock();
        try {
            var result = locks.computeIfAbsent(name, (it) -> new LocalLock(it, masterLock, locks));
            result.registerThread(Thread.currentThread());
            return result;
        } finally {
            masterLock.unlock();
        }
    }

}
