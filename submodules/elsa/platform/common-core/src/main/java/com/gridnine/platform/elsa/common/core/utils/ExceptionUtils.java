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

package com.gridnine.platform.elsa.common.core.utils;

import com.gridnine.platform.elsa.common.core.l10n.Localizer;
import com.gridnine.platform.elsa.common.core.model.common.RunnableWithException;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Callable;

public class ExceptionUtils {

    @Autowired
    private Localizer localizer;

    public static void wrapException(RunnableWithException body) {
        try {
            body.run();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static <T> T wrapException(Callable<T> body) {
        try {
            return body.call();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public String getLocalizedMessage(Xeption ex) {
        return switch (ex.getType()) {
            case FOR_END_USER -> localizer.toString(ex.getEndUserMessage());
            case FOR_ADMIN -> localizer.toString(ex.getAdminMessage());
            case FOR_DEVELOPER -> ex.getDeveloperMessage();
        };
    }
}
