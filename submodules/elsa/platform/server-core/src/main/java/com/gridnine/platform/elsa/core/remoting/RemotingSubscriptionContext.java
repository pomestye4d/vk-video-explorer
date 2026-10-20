/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.remoting;

import com.google.gson.JsonObject;
import com.gridnine.platform.elsa.common.core.utils.TypedParameterId;
import jakarta.websocket.Session;

import java.util.HashMap;
import java.util.Map;

public class RemotingSubscriptionContext {

    private Session session;

    private JsonObject parsedRequest;

    public JsonObject getParsedRequest() {
        return parsedRequest;
    }

    public void setParsedRequest(JsonObject parsedRequest) {
        this.parsedRequest = parsedRequest;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    private final Map<String, Object> parameters = new HashMap<>();

    public <V> void setParameter(TypedParameterId<V> id, V value) {
        parameters.put(id.getId(), value);
    }

    public <V> V getParameter(TypedParameterId<V> id) {
        return (V) parameters.get(id.getId());
    }

}
