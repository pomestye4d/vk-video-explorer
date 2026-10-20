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

import com.gridnine.platform.elsa.common.core.lock.NamedLock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LocalLock extends ReentrantLock implements NamedLock {

    private final Map<Thread, Boolean> threadsMap = new ConcurrentHashMap<>();

    private final String name;

    private final ReentrantLock masterLock;

    private final Map<String, LocalLock> locks;

    public LocalLock(String name, ReentrantLock masterLock, Map<String, LocalLock> locks) {
        super(true);
        this.name = name;
        this.masterLock = masterLock;
        this.locks = locks;
    }

    @Override
    public String getName() {
        return name;
    }

    void registerThread(Thread thread) {
        threadsMap.put(thread, true);
    }

    @Override
    public void close() {
        if (getHoldCount() == 0) {
            masterLock.lock();
            try {
                threadsMap.remove(Thread.currentThread());
                if (threadsMap.isEmpty()) {
                    locks.remove(name);
                }
            } finally {
                masterLock.unlock();
            }
        }
    }
}
