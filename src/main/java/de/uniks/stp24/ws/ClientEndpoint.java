package de.uniks.stp24.ws;

import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@jakarta.websocket.ClientEndpoint
public class ClientEndpoint {
    private final URI endpointURI;
    private final List<Consumer<String>> messageHandlers = Collections.synchronizedList(new ArrayList<>());

    Session userSession;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private Instant lastPong = Instant.now();

    public ClientEndpoint(URI endpointURI) {
        this.endpointURI = endpointURI;
    }

    public boolean isOpen() {
        return this.userSession != null && this.userSession.isOpen();
    }

    public void open() {
        if (isOpen()) {
            return;
        }

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;

        executorService.scheduleAtFixedRate(() -> {
                    if (Instant.now().minusSeconds(30).isBefore(lastPong)) {
                        return;
                    }

                    try {
                        userSession.getAsyncRemote().sendPing(null);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                },
                30, 30, TimeUnit.SECONDS);
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.executorService.shutdownNow();
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        this.lastPong = Instant.now();
        for (final Consumer<String> handler : this.messageHandlers) {
            handler.accept(message);
        }
    }

    @OnError
    public void onError(Throwable error) {
        System.err.println("WS onError: " + error.getMessage());
    }

    public void addMessageHandler(Consumer<String> msgHandler) {
        this.messageHandlers.add(msgHandler);
    }

    public void removeMessageHandler(Consumer<String> msgHandler) {
        this.messageHandlers.remove(msgHandler);
    }

    public void sendMessage(String message) {
        if (this.userSession == null) {
            return;
        }

        this.userSession.getAsyncRemote().sendText(message);
    }

    public void close() {
        if (this.userSession == null) {
            return;
        }

        try {
            this.userSession.close();
        } catch (IOException e) {
            System.err.println("WS Close: " + e.getMessage());
        }
    }

    public boolean hasMessageHandlers() {
        return !this.messageHandlers.isEmpty();
    }
}
