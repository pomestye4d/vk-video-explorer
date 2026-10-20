package com.gridnine.platform.elsa.core.remoting;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.gridnine.platform.elsa.common.core.serialization.JsonMarshaller;
import com.gridnine.platform.elsa.common.core.serialization.JsonUnmarshaller;
import com.gridnine.platform.elsa.common.core.serialization.SerializationParameters;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import com.gridnine.platform.elsa.common.core.utils.Lazy;
import com.gridnine.platform.elsa.common.core.utils.Pair;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingDescription;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingGroupDescription;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingMetaRegistry;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingSubscriptionDescription;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.apache.tomcat.websocket.server.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class WsFilter implements Filter, WebSocketFacade {

    public final static String KEY_HANDSHAKE_REQUEST = "KEY_HANDSHAKE_REQUEST";

    private final static AtomicLong counter = new AtomicLong(0);
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private RemotingMetaRegistry registry;

    @Autowired
    private JsonMarshaller jsonMarshaller;

    @Autowired
    private JsonUnmarshaller jsonUnmarshaller;

    @Autowired
    private RemotingHandlersRegistry remotingHandlersRegistry;

    private final Map<String, Map<Session, Map<Long, Pair<RemotingSubscriptionContext, Object>>>> subscriptions = new ConcurrentHashMap<>();

    private final Map<Session, Set<String>> inversedSubscriptions = new ConcurrentHashMap<>();

    private final Map<String, Pair<Lazy<SubscriptionHandler<Object, Object>>, SubscriptionParams>> handlersCache = new HashMap<>();

    private final SerializationParameters serializationParameters = new SerializationParameters().setClassSerializationStrategy(SerializationParameters.ClassSerializationStrategy.NAME)
            .setEntityReferenceCaptionSerializationStrategy(SerializationParameters.EntityReferenceCaptionSerializationStrategy.ALL)
            .setEnumSerializationStrategy(SerializationParameters.EnumSerializationStrategy.NAME)
            .setEmptyListSerializationStrategy(SerializationParameters.EmptyListSerializationStrategy.INCLUDE).setPrettyPrint(false);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        var sc = (ServerContainer) filterConfig.getServletContext().getAttribute(Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE);
        ExceptionUtils.wrapException(() -> {
            sc.addEndpoint(ServerEndpointConfig.Builder
                    .create(ElsaWebSocketEndpoint.class, getWebSocketPath())
                    .configurator(getEndpointConfigurator()).build());
        });
    }

    @PostConstruct
    private void populateCaches() {
        registry.getRemotings().values().forEach(remoting -> remoting.getGroups().values().forEach(group -> {
            group.getSubscriptions().values().forEach(subscription -> {
                String key = "%s:%s:%s".formatted(remoting.getId(), group.getId(), subscription.getId());
                handlersCache.put(key, Pair.of(new Lazy<>(() ->
                                Objects.requireNonNull(remotingHandlersRegistry.getSubscriptionHandler("%s:%s:%s".formatted(remoting.getId(), group.getId(), subscription.getId())))),
                        new SubscriptionParams(remoting, group, subscription)));
            });
        }));
    }

    protected ServerEndpointConfig.Configurator getEndpointConfigurator() {
        return new ServerEndpointConfig.Configurator() {
            @Override
            public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
                super.modifyHandshake(sec, request, response);
                sec.getUserProperties().put(KEY_HANDSHAKE_REQUEST, request);
            }

            @Override
            public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
                //noinspection unchecked
                return (T) new ElsaWebSocketEndpoint();
            }
        };
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response);
    }

    @Override
    public void publishMessage(String id, Object event) {
        ExceptionUtils.wrapException(() -> {
            var subscription = subscriptions.get(id);
            if (subscription == null) {
                return;
            }
            var pair = handlersCache.get(id);
            ByteArrayOutputStream baos = null;
            if (event != null) {
                baos = new ByteArrayOutputStream();
                jsonMarshaller.marshal(event, baos, false, serializationParameters);
            }
            var content = baos == null ? null : baos.toString(StandardCharsets.UTF_8);
            subscription.forEach((session, subscriptions) -> {
                subscriptions.forEach((sid, params) -> {
                    try {
                        if (pair.getLeft().getObject().isApplicable(event, params.getRight(), params.getLeft())) {
                            synchronized (session) {
                                session.getBasicRemote().sendText("{\"id\":\"%s\",\"subscriptionId\":%s%s}".formatted(id, sid,
                                        content == null ? "" : ",\"event\":%s".formatted(content)));
                            }
                        }
                    } catch (Throwable e) {
                        log.error("unable to send data to subscription %s: %s".formatted(id, sid), e);
                    }
                });
            });
        });
    }


    protected String getWebSocketPath() {
        return "/websocket";
    }


    public class ElsaWebSocketEndpoint extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            try {
                onOpen(session, config, remotingHandlersRegistry.getSubscriptionAdvices(), 0);
            } catch (Throwable t) {
                log.error("unable open session", t);
                try {
                    session.close();
                } catch (IOException e) {
                    log.error("unable close session", t);
                }
            }
        }


        private void onOpen(Session session, EndpointConfig config, List<SubscriptionAdvice> advices, int idx) throws Exception {
            if (idx == advices.size()) {
                onOpenInternal(session, config);
                return;
            }
            remotingHandlersRegistry.getSubscriptionAdvices().get(idx).onOpen(session, config, (session2, config2) ->
                    onOpen(session2, config2, remotingHandlersRegistry.getSubscriptionAdvices(), idx + 1)
            );
        }

        private void onOpenInternal(Session session, EndpointConfig config) {
            synchronized (session) {
                inversedSubscriptions.put(session, Collections.synchronizedSet(new HashSet<>()));
            }
            session.addMessageHandler(new WebSocketMessageHandler(session));
        }


        @Override
        public void onClose(Session session, CloseReason closeReason) {
            try {
                onClose(session, closeReason, remotingHandlersRegistry.getSubscriptionAdvices(), 0);
            } catch (Throwable t) {
                log.error("unable close session", t);
            }
        }

        private void onClose(Session session, CloseReason closeReason, List<SubscriptionAdvice> advices, int idx) throws Exception {
            if (idx == advices.size()) {
                onCloseInternal(session);
                return;
            }
            remotingHandlersRegistry.getSubscriptionAdvices().get(idx).onClose(session, closeReason, (session2, closeReason2) ->
                    onClose(session2, closeReason2, remotingHandlersRegistry.getSubscriptionAdvices(), idx + 1)
            );
        }

        private void onCloseInternal(Session session) {
            synchronized (session) {
                var subids = inversedSubscriptions.get(session);
                if (subids != null) {
                    for (var subid : subids) {
                        subscriptions.get(subid).remove(session);
                    }
                }
                inversedSubscriptions.remove(session);
            }
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            try {
                onError(session, throwable, remotingHandlersRegistry.getSubscriptionAdvices(), 0);
            } catch (Throwable t) {
                log.error("unable perform onError", t);
            }
        }

        private void onError(Session session, Throwable throwable, List<SubscriptionAdvice> advices, int idx) throws Exception {
            if (idx == advices.size()) {
                onErrorInternal(session);
                return;
            }
            remotingHandlersRegistry.getSubscriptionAdvices().get(idx).onError(session, throwable, (session2, throwable2) ->
                    onError(session2, throwable2, remotingHandlersRegistry.getSubscriptionAdvices(), idx + 1)
            );
        }

        private void onErrorInternal(Session session) {
            onCloseInternal(session);
        }
    }

    class WebSocketMessageHandler implements MessageHandler.Whole<String> {

        private final Session session;

        WebSocketMessageHandler(Session session) {
            this.session = session;
        }

        @Override
        public void onMessage(String message) {
            var obj = new Gson().fromJson(message, JsonObject.class);
            var operation = obj.get("operation").getAsString();
            var callId = obj.get("callId").getAsLong();
            if ("subscribe".equals(operation)) {
                try {
                    var context = new RemotingSubscriptionContext();
                    context.setSession(session);
                    context.setParsedRequest(obj);
                    subscribe(message, callId, context, remotingHandlersRegistry.getSubscriptionAdvices(), 0);
                } catch (Throwable e) {
                    try {
                        session.close();
                    } catch (IOException ex) {
                        log.error("unable to close session");
                    }
                }
                return;
            }
            if ("unsubscribe".equals(operation)) {
                try {
                    var subscriptionId = obj.get("subscriptionId").getAsLong();
                    unsubscribe(subscriptionId, callId, session, remotingHandlersRegistry.getSubscriptionAdvices(), 0);
                } catch (Throwable e) {
                    try {
                        session.close();
                    } catch (IOException ex) {
                        log.error("unable to close session");
                    }
                }
            }
        }

        private void subscribe(String message, long callId, RemotingSubscriptionContext context, List<SubscriptionAdvice> advices, int idx) throws Exception {
            if (idx == advices.size()) {
                subscribeInternal(context, callId);
                return;
            }
            remotingHandlersRegistry.getSubscriptionAdvices().get(idx).onSubscribe(message, context, (message2, context2) ->
                    subscribe(message2, callId, context2, remotingHandlersRegistry.getSubscriptionAdvices(), idx + 1)
            );
        }

        private void subscribeInternal(RemotingSubscriptionContext context, long callId) throws Exception{
            JsonObject obj = context.getParsedRequest();
            var remoting = obj.get("remoting").getAsString();
            var group = obj.get("group").getAsString();
            var subscription = obj.get("subscription").getAsString();
            var jsonData = obj.get("parameter");
            String key = "%s:%s:%s".formatted(remoting, group, subscription);
            Object parameter = null;
            if (jsonData != null) {
                var pair = handlersCache.get(key);
                var str = jsonData.toString();
                parameter = jsonUnmarshaller.unmarshal(pair.getRight().subscription().getParameterClassName(), new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)), serializationParameters);
            }
            var subscriptionId = counter.incrementAndGet();
            synchronized (session) {
                subscriptions.computeIfAbsent(key, (i) -> new ConcurrentHashMap<>()).computeIfAbsent(session, (i) -> new ConcurrentHashMap<>()).put(subscriptionId, Pair.of(context, parameter));
                inversedSubscriptions.get(session).add(key);
            }
            session.getBasicRemote().sendText("{\"callId\":%s, \"subscriptionId\":%s}".formatted(callId, subscriptionId));

        }

        private void unsubscribe(long subscriptionId, long callId, Session session, List<SubscriptionAdvice> advices, int idx) throws Exception {
            if (idx == advices.size()) {
                unsubscribeInternal(subscriptionId, callId);
                return;
            }
            remotingHandlersRegistry.getSubscriptionAdvices().get(idx).onUnsubscribe(subscriptionId, session, (subscriptionId2, session2) ->
                    unsubscribe(subscriptionId2, callId, session2, remotingHandlersRegistry.getSubscriptionAdvices(), idx + 1)
            );
        }

        private void unsubscribeInternal(long subscriptionId, long callId) throws Exception{
            synchronized (session) {
                for (var subId : new ArrayList<>(inversedSubscriptions.get(session))) {
                    var map = subscriptions.get(subId).get(session);
                    map.remove(subscriptionId);
                    if(map.isEmpty()){
                        inversedSubscriptions.get(session).remove(subId);
                    }
                }
            }
            session.getBasicRemote().sendText("{\"callId\":%s}".formatted(callId));
        }
    }


    record SubscriptionParams(RemotingDescription remoting, RemotingGroupDescription group,
                              RemotingSubscriptionDescription subscription) {
    }

}
