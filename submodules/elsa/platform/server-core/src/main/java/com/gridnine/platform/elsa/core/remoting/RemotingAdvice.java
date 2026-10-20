/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.remoting;

import com.gridnine.platform.elsa.common.core.model.common.HasPriority;
import com.gridnine.platform.elsa.common.core.model.common.RunnableWithExceptionAndArgument;

public interface RemotingAdvice extends HasPriority {
    default void onServerCall(RemotingCallContext context, RunnableWithExceptionAndArgument<RemotingCallContext> callback) throws Exception {
        callback.run(context);
    }

    default void onDownload(RemotingCallContext context, RunnableWithExceptionAndArgument<RemotingCallContext> callback) throws Exception {
        callback.run(context);
    }

    default void onUpload(RemotingCallContext context, RunnableWithExceptionAndArgument<RemotingCallContext> callback) throws Exception {
        callback.run(context);
    }
}
