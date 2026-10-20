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

package com.gridnine.platform.elsa.core.codec;

import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

public class DesCodec {

    private Cipher encryptCipher;
    private Cipher decryptCipher;

    @Autowired
    public void setEnvironment(Environment env) throws Exception {
        final String KEY = env.getProperty("elsa.encryptionKey", "1234345356");
        encryptCipher = Cipher.getInstance("DES");
        encryptCipher.init(Cipher.ENCRYPT_MODE, getKey(KEY.getBytes()));
        decryptCipher = Cipher.getInstance("DES");
        decryptCipher.init(Cipher.DECRYPT_MODE, getKey(KEY.getBytes()));
    }

    private Key getKey(byte[] arrBTmp) {
        final byte[] arrB = new byte[8];

        var i = 0;
        while (i < arrBTmp.length && i < arrB.length) {
            arrB[i] = arrBTmp[i];
            i++;
        }
        return new SecretKeySpec(arrB, "DES");
    }

    public String encrypt(String strIn) {
        return Base64.getEncoder().encodeToString(encrypt(strIn.getBytes(StandardCharsets.UTF_8)));
    }

    public byte[] encrypt(byte[] arrB) {
        return ExceptionUtils.wrapException(() -> encryptCipher.doFinal(arrB));
    }

    public byte[] decrypt(byte[] arrB) {
        return ExceptionUtils.wrapException(() -> decryptCipher.doFinal(arrB));
    }

    public String decrypt(String strIn) {
        return new String(decrypt(Base64.getDecoder().decode(strIn)), StandardCharsets.UTF_8);
    }
}
