package ch.niceideas.bigdata.proxy;

import ch.niceideas.bigdata.types.ServiceWebId;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Handles establishment and tracking of next 'hop', and
 * copies data from the current session to the next hop.
 */
public interface WebSocketProxyServer extends WebSocketHandler {

    @Override
    void handleMessage(WebSocketSession webSocketServerSession, WebSocketMessage<?> webSocketMessage) throws Exception;

    @Override
    void afterConnectionClosed(WebSocketSession session, CloseStatus status);

    void removeForwardersForService(ServiceWebId serviceId);

    WebSocketProxyForwarder createForwarder(ServiceWebId serviceId, WebSocketSession webSocketServerSession, String targetPath);
}
