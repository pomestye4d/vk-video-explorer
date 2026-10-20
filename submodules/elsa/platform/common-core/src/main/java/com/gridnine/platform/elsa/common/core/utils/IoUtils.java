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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class IoUtils {
    private static final int COPY_BUF_SIZE = 8024;

    public static byte[] gunzip(byte[] input) {
        return ExceptionUtils.wrapException(() -> {
            try (var is = new GZIPInputStream(new ByteArrayInputStream(input))) {
                var baos = new ByteArrayOutputStream();
                copy(is, baos, COPY_BUF_SIZE);
                return baos.toByteArray();
            }
        });
    }

    public static byte[] gzip(byte[] data) throws IOException {
        var baos3 = new ByteArrayOutputStream();
        try (var os = new GZIPOutputStream(baos3)) {
            copy(new ByteArrayInputStream(data), os, COPY_BUF_SIZE);
        }
        return baos3.toByteArray();
    }

    public static long copy(final InputStream input, final OutputStream output) throws IOException {
        return copy(input, output, COPY_BUF_SIZE);
    }

    public static long copy(final InputStream input, final OutputStream output, final int buffersize) throws IOException {
        if (buffersize < 1) {
            throw new IllegalArgumentException("buffersize must be bigger than 0");
        }
        final byte[] buffer = new byte[buffersize];
        int n;
        long count = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static String sha1sum(File file) {
        return ExceptionUtils.wrapException(() -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                return sha1sum(fis);
            }
        });
    }

    public static String sha1sum(String data) {
        return ExceptionUtils.wrapException(() -> {
            try (var bais =new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
                return sha1sum(bais);
            }
        });
    }

    public static String sha1sum(InputStream is) {
        return ExceptionUtils.wrapException(() -> {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA1");
            byte[] buffer = new byte[1024];
            int n;
            while ((n = is.read(buffer)) != -1) {
                md.update(buffer, 0, n);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        });
    }
    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {}
    }
}
