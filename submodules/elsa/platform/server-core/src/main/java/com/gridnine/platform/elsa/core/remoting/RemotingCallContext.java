/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.remoting;

import com.gridnine.platform.elsa.common.core.utils.TypedParameterId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

public class RemotingCallContext {

    private HttpServletRequest httpRequest;

    private HttpServletResponse httpResponse;

    private final Map<String, Object> parameters = new HashMap<>();

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public <V> void setParameter(TypedParameterId<V> id, V value) {
        parameters.put(id.getId(), value);
    }

    public <V> V getParameter(TypedParameterId<V> id) {
        return (V) parameters.get(id.getId());
    }

    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }
}
