/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.remoting;

import com.gridnine.platform.elsa.common.core.model.common.HasPriority;
import com.gridnine.platform.elsa.common.core.model.common.RunnableWithExceptionAnd2Arguments;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

public interface SubscriptionAdvice extends HasPriority {

    default void onOpen(Session session, EndpointConfig config, RunnableWithExceptionAnd2Arguments<Session, EndpointConfig> callback) throws Exception{
        callback.run(session, config);
    }

    default void onClose(Session session, CloseReason closeReason, RunnableWithExceptionAnd2Arguments<Session, CloseReason> callback) throws Exception{
        callback.run(session, closeReason);
    }

    default void onError(Session session, Throwable throwable, RunnableWithExceptionAnd2Arguments<Session, Throwable> callback) throws Exception{
        callback.run(session, throwable);
    }

    default void onSubscribe(String message, RemotingSubscriptionContext context, RunnableWithExceptionAnd2Arguments<String, RemotingSubscriptionContext> callback) throws Exception{
        callback.run(message, context);
    }

    default void onUnsubscribe(long subscriptionId, Session session, RunnableWithExceptionAnd2Arguments<Long, Session> callback) throws Exception{
        callback.run(subscriptionId, session);
    }
}
