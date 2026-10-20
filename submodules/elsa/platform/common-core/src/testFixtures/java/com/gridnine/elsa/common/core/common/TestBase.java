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

import com.gridnine.platform.elsa.config.ElsaCommonCoreConfiguration;
import com.gridnine.platform.elsa.config.ElsaCommonMetaConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

@ContextConfiguration(classes = {ElsaCommonMetaConfiguration.class,
        ElsaCommonCoreConfiguration.class,
        ElsaCommonCoreTestConfiguration.class})
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
public abstract class TestBase {

    public TestBase(){
        System.setProperty("elsa.testing", "true");
    }


    protected final Logger log = LoggerFactory.getLogger(getClass());

    @BeforeEach
    public void beforeEach() throws Exception {
        setup();
    }

    protected void setup() throws Exception {
    }

    @AfterEach
    public void afterEach() throws Exception {
        dispose();
    }

    protected void dispose() throws Exception {
    }


    public void println(Object value) {
        log.debug(value.toString());
    }

    public void assertEquals(BigDecimal expected, BigDecimal actual){
        if(expected == null){
            Assertions.assertNull(actual);
            return;
        }
        Assertions.assertNotNull(expected);
        Assertions.assertEquals(expected.doubleValue(), actual.doubleValue(), 0.001);
    }

    public void assertEquals(double expected, BigDecimal actual){
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(expected, actual.doubleValue(), 0.001);
    }

}
