/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.remoting;

public interface RestHandler<RQ, RP> {
    RP service(RQ request, RemotingCallContext context) throws Exception;

    String getId();
}
