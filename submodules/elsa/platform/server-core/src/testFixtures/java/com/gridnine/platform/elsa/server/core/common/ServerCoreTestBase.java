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

package com.gridnine.platform.elsa.server.core.common;

import com.gridnine.elsa.common.core.common.TestBase;
import com.gridnine.platform.elsa.config.ElsaServerCoreConfiguration;
import com.gridnine.platform.elsa.config.ElsaServerCoreStorageConfiguration;
import com.gridnine.platform.elsa.core.remoting.standard.BinaryData;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.*;

@ContextConfiguration(classes = {ElsaServerCoreStorageConfiguration.class, ElsaServerCoreConfiguration.class, ElsaServerCoreTestConfiguration.class},
        initializers = ServerCoreTestPropertyOverrideContextInitializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class ServerCoreTestBase extends TestBase {

    public void saveDataToTempDir(BinaryData data) throws IOException {
        var file = new File("temp/%s".formatted(data.getName()));
        if(!file.getParentFile().exists() && !file.getParentFile().mkdirs()){
            throw new RuntimeException("unable to create temp dir");
        }
        if(file.exists() && !file.delete()){
            throw new RuntimeException("unable to delete file " + file.getAbsolutePath() );
        }
        try(var os = new FileOutputStream(file); var is = data.getInputStream()){
            is.transferTo(os);
            os.flush();
        }
    }
}
