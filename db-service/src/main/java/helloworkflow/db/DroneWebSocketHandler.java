package helloworkflow.db;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class DroneWebSocketHandler extends TextWebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(DroneWebSocketHandler.class);

  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    sessions.add(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessions.remove(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    sessions.remove(session);
    logger.warn("WebSocket transport error for session {}", session.getId(), exception);
  }

  public void notifySaved(long id) {
    TextMessage message = new TextMessage("message saved to db " + id);
    for (WebSocketSession session : sessions) {
      synchronized (session) {
        if (!session.isOpen()) {
          sessions.remove(session);
          continue;
        }

        try {
          session.sendMessage(message);
        } catch (IOException exception) {
          sessions.remove(session);
          logger.warn("Failed to notify WebSocket session {}", session.getId(), exception);
          try {
            session.close(CloseStatus.SERVER_ERROR);
          } catch (IOException closeException) {
            logger.warn("Failed to close WebSocket session {}", session.getId(), closeException);
          }
        }
      }
    }
  }
}
