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

package com.gridnine.platform.elsa.common.core.reflection;

import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionFactory {

    private final Map<String, Class<?>> cache = new ConcurrentHashMap<>();

    public <T> T newInstance(Class<T> cls) {
        return ExceptionUtils.wrapException(() -> cls.getDeclaredConstructor().newInstance());
    }

    @SuppressWarnings("unchecked")
    public <T> T newInstance(String className) {
        return (T) ExceptionUtils.wrapException(() -> getClass(className).getDeclaredConstructor().newInstance());
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(String className) {
        var cleanClassName = className;
        var idx = className.indexOf("<");
        if (idx != -1) {
            cleanClassName = className.substring(0, idx);
        }
        return (Class<T>) cache.computeIfAbsent(cleanClassName, (key) -> ExceptionUtils.wrapException(() -> Class.forName(key)));
    }

    public Enum<?> safeGetEnum(Class<Enum<?>> cls, String item) {
        for (Enum<?> constant : cls.getEnumConstants()) {
            if (constant.name().equals(item)) {
                return constant;
            }
        }
        return null;
    }

    public Enum<?> safeGetEnum(String className, String item) {
        return safeGetEnum(getClass(className), item);
    }

}
