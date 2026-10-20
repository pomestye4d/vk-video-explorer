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

package com.gridnine.platform.elsa.core.remoting;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class RemotingHandlersRegistry {

    private volatile Map<String, RestHandler<?, ?>> serviceHandlersMap;

    private volatile Map<String, SubscriptionHandler<?, ?>> subscriptionHandlersMap;

    private volatile List<RemotingAdvice> advices;

    private volatile boolean initialized;
    @Autowired
    private ListableBeanFactory factory;

    public List<RemotingAdvice> getAdvices() {
        init();
        return advices;
    }


    private List<SubscriptionAdvice> subscriptionAdvices;

    public List<SubscriptionAdvice> getSubscriptionAdvices() {
        init();
        return subscriptionAdvices;
    }

    private void init(){
        if(!initialized){
            synchronized (this) {
                if(!initialized){
                    serviceHandlersMap = new HashMap<>();
                    subscriptionAdvices = new ArrayList<>();
                    advices = new ArrayList<>();
                    subscriptionHandlersMap = new HashMap<>();
                    factory.getBeansOfType(RestHandler.class).values().forEach(h -> serviceHandlersMap.put(h.getId(), h));
                    factory.getBeansOfType(SubscriptionHandler.class).values().forEach(h -> subscriptionHandlersMap.put(h.getId(), h));
                    factory.getBeansOfType(RemotingAdvice.class).values().stream().sorted(Comparator.comparingDouble(RemotingAdvice::getPriority)).forEach(a -> advices.add(a));
                    factory.getBeansOfType(SubscriptionAdvice.class).values().stream().sorted(Comparator.comparingDouble(SubscriptionAdvice::getPriority)).forEach(a -> subscriptionAdvices.add(a));
                    initialized = true;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <RQ, RS> RestHandler<RQ, RS> getServiceHandler(String id) {
        init();
        return (RestHandler<RQ, RS>) serviceHandlersMap.get(id);
    }

    @SuppressWarnings("unchecked")
    public <RP, RE> SubscriptionHandler<RP, RE> getSubscriptionHandler(String id) {
        init();
        return (SubscriptionHandler<RP, RE>) subscriptionHandlersMap.get(id);
    }
}
