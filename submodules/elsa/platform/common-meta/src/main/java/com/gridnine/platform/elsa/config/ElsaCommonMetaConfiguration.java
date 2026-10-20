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

package com.gridnine.platform.elsa.config;

import com.gridnine.platform.elsa.common.meta.adminUi.AdminUiMetaRegistry;
import com.gridnine.platform.elsa.common.meta.custom.CustomMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.common.meta.l10n.L10nMetaRegistry;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingMetaRegistry;
import com.gridnine.platform.elsa.common.meta.webApp.WebAppMetaRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElsaCommonMetaConfiguration {
    @Bean
    public CustomMetaRegistry customMetaRegistry() {
        return new CustomMetaRegistry();
    }

    @Bean
    public DomainMetaRegistry domainMetaRegistry() {
        return new DomainMetaRegistry();
    }

    @Bean
    public L10nMetaRegistry l10nMetaRegistry() {
        return new L10nMetaRegistry();
    }

    @Bean
    public RemotingMetaRegistry remotingMetaRegistry() {
        return new RemotingMetaRegistry();
    }

    @Bean
    public WebAppMetaRegistry webAppMetaRegistry() {
        return new WebAppMetaRegistry();
    }

    @Bean
    public AdminUiMetaRegistry adminUiMetaRegistry() {
        return new AdminUiMetaRegistry();
    }
}
