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

import com.gridnine.platform.elsa.common.meta.remoting.RemotingMetaRegistry;
import com.gridnine.platform.elsa.common.rest.core.GetSubscriptionDescriptionRequest;
import com.gridnine.platform.elsa.common.rest.core.GetSubscriptionDescriptionResponse;
import com.gridnine.platform.elsa.core.remoting.RemotingCallContext;
import com.gridnine.platform.elsa.core.remoting.RestHandler;
import com.gridnine.platform.elsa.server.core.CoreRemotingConstants;
import org.springframework.beans.factory.annotation.Autowired;

public class GetSubscriptionDescriptionHandler implements RestHandler<GetSubscriptionDescriptionRequest, GetSubscriptionDescriptionResponse> {

    @Autowired
    private RemotingMetaRegistry remotingMetaRegistry;

    @Override
    public String getId() {
        return CoreRemotingConstants.CORE_META_GET_SUBSCRIPTION_DESCRIPTION;
    }

    @Override
    public GetSubscriptionDescriptionResponse service(GetSubscriptionDescriptionRequest request, RemotingCallContext context) {
        var subscriptionDescription = remotingMetaRegistry.getRemotings().get(request.getRemotingId()).getGroups().get(request.getGroupId()).getSubscriptions().get(request.getSubscriptionId());
        var result = new GetSubscriptionDescriptionResponse();
        result.setParameterClassName(subscriptionDescription.getParameterClassName());
        result.setEventClassName(subscriptionDescription.getEventClassName());
        return result;
    }
}
