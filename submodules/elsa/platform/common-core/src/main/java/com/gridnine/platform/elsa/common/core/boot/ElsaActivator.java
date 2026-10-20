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

package com.gridnine.platform.elsa.common.core.boot;

import com.gridnine.platform.elsa.common.core.model.common.HasPriority;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Comparator;

public interface ElsaActivator extends HasPriority {
    void activate() throws Exception;

    static void performActivation(ConfigurableApplicationContext ctx) {
        try {
            ctx.getBeanFactory().getBeansOfType(ElsaActivator.class).values().stream()
                    .sorted(Comparator.comparing(HasPriority::getPriority)).forEach((b) -> ExceptionUtils.wrapException(b::activate));
        } catch (Throwable t) {
            LoggerFactory.getLogger(ElsaActivator.class).error("unable to start application", t);
            ctx.close();
        }
    }
}
