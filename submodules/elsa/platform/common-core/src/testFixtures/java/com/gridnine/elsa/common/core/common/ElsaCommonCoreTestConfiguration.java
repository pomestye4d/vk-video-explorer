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

package com.gridnine.elsa.common.core.common;

import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.test.ElsaCommonCoreTestDomainMetaRegistryConfigurator;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistryConfigurator;
import com.gridnine.platform.elsa.config.ElsaCommonCoreCustomMetaRegistryConfigurator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Configuration
public class ElsaCommonCoreTestConfiguration {

    @Bean
    public DomainMetaRegistryConfigurator elsaCommonCoreTestDomainMetaRegistryConfigurator() {
        return new ElsaCommonCoreTestDomainMetaRegistryConfigurator();
    }

    @Bean
    public ElsaCommonCoreCustomMetaRegistryConfigurator elsaCommonCoreCustomMetaRegistryConfigurator() {
        return new ElsaCommonCoreCustomMetaRegistryConfigurator();
    }

    @Bean
    public ClassMapper classMapper() {
        return new ClassMapper() {
            @Override
            public int getId(String name) {
                throw Xeption.forDeveloper("not implemented");
            }

            @Override
            public String getName(int id) {
                throw Xeption.forDeveloper("not implemented");
            }
        };
    }

    @Bean
    public EnumMapper standardEnumMapper() {
        return new EnumMapper() {
            @Override
            public int getId(Enum<?> value) {
                return value.ordinal();
            }

            @Override
            public String getName(int id, Class<Enum<?>> cls) {
                return Stream.of(cls.getEnumConstants()).filter(it -> it.ordinal() == id).map(Enum::name).findFirst().orElse(null);
            }
        };
    }

    @Bean
    public ClassMapper fakeClassMapper() {
        return new ClassMapper() {
            private final AtomicInteger ref = new AtomicInteger(0);
            private final Map<Integer, String> id2NameMap = new ConcurrentHashMap<>();
            private final Map<String, Integer> name2IdMap = new ConcurrentHashMap<>();

            @Override
            public int getId(String name) {
                var res = name2IdMap.get(name);
                if (res == null) {
                    synchronized (this) {
                        res = name2IdMap.get(name);
                        if (res == null) {
                            res = ref.incrementAndGet();
                            name2IdMap.put(name, res);
                            id2NameMap.put(res, name);
                        }
                    }
                }
                return name2IdMap.get(name);
            }

            @Override
            public String getName(int id) {
                return id2NameMap.get(id);
            }
        };
    }

}
