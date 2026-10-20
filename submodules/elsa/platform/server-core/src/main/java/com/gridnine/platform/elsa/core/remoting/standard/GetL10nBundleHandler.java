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

package com.gridnine.platform.elsa.core.remoting.standard;

import com.gridnine.platform.elsa.common.core.utils.LocaleUtils;
import com.gridnine.platform.elsa.common.meta.l10n.L10nMessageDescription;
import com.gridnine.platform.elsa.common.meta.l10n.L10nMessageParameterDescription;
import com.gridnine.platform.elsa.common.meta.l10n.L10nMetaRegistry;
import com.gridnine.platform.elsa.common.remoting.core.GetL10nBundleRequest;
import com.gridnine.platform.elsa.common.remoting.core.GetL10nBundleResponse;
import com.gridnine.platform.elsa.common.remoting.core.RL10nMessageDescription;
import com.gridnine.platform.elsa.common.remoting.core.RL10nMessageParameterDescription;
import com.gridnine.platform.elsa.common.rest.core.RemotingEntityValueType;
import com.gridnine.platform.elsa.core.remoting.RemotingCallContext;
import com.gridnine.platform.elsa.core.remoting.RestHandler;
import com.gridnine.platform.elsa.server.core.CoreRemotingConstants;
import org.springframework.beans.factory.annotation.Autowired;

public class GetL10nBundleHandler implements RestHandler<GetL10nBundleRequest, GetL10nBundleResponse> {
    @Autowired
    private L10nMetaRegistry registry;

    @Override
    public String getId() {
        return CoreRemotingConstants.CORE_L10N_GET_BUNDLE;
    }

    @Override
    public GetL10nBundleResponse service(GetL10nBundleRequest request, RemotingCallContext context) {
        var bundle = registry.getBundles().get(request.getBundleId());
        var result = new GetL10nBundleResponse();
        if (bundle != null) {
            var locale = LocaleUtils.getLocale(request.getLanguage() == null ? "en" : request.getLanguage(), null);
            for (L10nMessageDescription message : bundle.getMessages().values()) {
                var item = new RL10nMessageDescription();
                item.setDisplayName(LocaleUtils.getLocalizedName(message.getDisplayNames(), locale, message.getId()));
                item.setId(message.getId());
                message.getParameters().values().forEach(p ->{
                    var param = new RL10nMessageParameterDescription();
                    param.setCollection(p.isCollection());
                    param.setId(p.getId());
                    param.setValueType(RemotingEntityValueType.valueOf(p.getType().name()));
                    item.getParameters().add(param);
                });
                result.getMessages().add(item);
            }
        }
        return result;
    }
}
