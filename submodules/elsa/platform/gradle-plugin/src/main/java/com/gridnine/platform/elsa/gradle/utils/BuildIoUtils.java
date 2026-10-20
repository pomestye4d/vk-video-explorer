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

package com.gridnine.platform.elsa.gradle.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class BuildIoUtils {

    public static void copyStream(InputStream input, OutputStream output) {
        BuildExceptionUtils.wrapException(()->{
            byte[] buffer = new byte[1024];
            long count;
            int n;
            for(count = 0L; -1 != (n = input.read(buffer)); count += n) {
                output.write(buffer, 0, n);
            }
        });
    }

    public static byte[] readBytes(URL url) {
        var result = new AtomicReference<byte[]>();
        BuildExceptionUtils.wrapException(()->{
            var baos= new ByteArrayOutputStream();
            try(var is = url.openStream()) {
                copyStream(is, baos);
            }
            result.set(baos.toByteArray());
        });
        return result.get();
    }

    public static String readText(URL url) {
        var result = new AtomicReference<String>();
        BuildExceptionUtils.wrapException(()->{
            result.set(new String(readBytes(url), StandardCharsets.UTF_8));
        });
        return result.get();
    }
}
