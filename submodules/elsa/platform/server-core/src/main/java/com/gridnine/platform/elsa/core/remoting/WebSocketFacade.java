package com.gridnine.platform.elsa.core.remoting;

public interface WebSocketFacade {
    void publishMessage(String id, Object event);
}
