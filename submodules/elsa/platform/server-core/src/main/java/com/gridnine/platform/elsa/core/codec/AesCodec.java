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
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

public class AesCodec {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // 128, 120, 112, 104, or 96
    private static final int GCM_IV_LENGTH = 12;
    private static final SecureRandom random = new SecureRandom();

    private SecretKey secretKey;

    @Autowired
    public void setEnvironment(Environment env) throws Exception {
        final String secretKey = env.getProperty("elsa.encryptionKey", "234543212378fgty");
        this.secretKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }


    public String encrypt(String strIn) {
        return Base64.getEncoder().encodeToString(encrypt(strIn.getBytes(StandardCharsets.UTF_8)));
    }

    public byte[] encrypt(byte[] plainData) {
        return ExceptionUtils.wrapException(() -> {
            var encryptCipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            var spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] ciphertext = encryptCipher.doFinal(plainData);
            var byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);
            return byteBuffer.array();
        });
    }

    public byte[] decrypt(byte[] cipheredData) {
        return ExceptionUtils.wrapException(() -> {
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipheredData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // Остальное - ciphertext
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            // Инициализация дешифратора
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Дешифрование
            return cipher.doFinal(ciphertext);
        });
    }

    public String decrypt(String strIn) {
        return new String(decrypt(Base64.getDecoder().decode(strIn)), StandardCharsets.UTF_8);
    }
}
