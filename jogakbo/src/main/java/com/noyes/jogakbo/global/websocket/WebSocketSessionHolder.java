package com.noyes.jogakbo.global.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

import org.springframework.web.socket.WebSocketSession;

public class WebSocketSessionHolder {

  static {
    userSessions = new ConcurrentHashMap<>();
  }

  // Key 값 으로 sessionID, Value로 WebSocketInfo 를 가지는 Map 선언
  private static Map<String, WebSocketSessionInfo> userSessions;

  /**
   * <pre>
   * regist session into ConcurrentHashMap, userSessions.
   * expect destination will be updated when client subscribe album.
   * </pre>
   * 
   * @param session
   */
  public static void registSession(WebSocketSession session) {

    WebSocketSessionInfo webSocketSessionInfo = WebSocketSessionInfo.builder().session(session).build();
    userSessions.put(session.getId(), webSocketSessionInfo);
  }

  /**
   * update WebSocketSessionInfo's destination field with sessionID
   * 
   * @param sessionID
   * @param destination
   */
  public static void updateSessionWithDestination(String sessionID, String destination) {

    // sessionID 에 해당하는 WebSocketSessionInfo 객체에 destination 필드 업데이트
    var webSocketSessionInfo = userSessions.get(sessionID);
    webSocketSessionInfo.setDestination(destination);
  }

  /**
   * remove WebSocketSesssionInfo in ConcurrentHashMap, userSessions
   * 
   * @param sessionID
   */
  public static void removeSession(String sessionID) {

    userSessions.remove(sessionID);
  }

  /**
   * close WebSocketSession corresponding destination field
   * 
   * @param destination
   * @throws IOException
   */
  public static void closeSessionByDestination(String destination) throws IOException {

    // sessionIDs를 순회하며 destination 에 해당하는 WebSocketSession을 close()
    for (var entry : userSessions.entrySet()) {

      String targetDest = entry.getValue().getDestination();
      if (targetDest != null && targetDest.equals(destination))
        entry.getValue().getSession().close();
    }
  }
}