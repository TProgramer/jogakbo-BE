package com.noyes.jogakbo.global.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

public class WebSocketSessionHolder {

  static {
    sessionIDsByDest = new ConcurrentHashMap<>();
    userSessions = new ConcurrentHashMap<>();
  }

  // key - username, value - List of user's sessions
  private static Map<String, List<String>> sessionIDsByDest;
  private static Map<String, WebSocketSession> userSessions;

  public static void registSession(WebSocketSession session) {

    userSessions.put(session.getId(), session);
  }

  public static void addSessionByDestination(String sessionID, String destination) {

    var sessionIDs = sessionIDsByDest.get(destination);

    if (sessionIDs == null)
      sessionIDs = new ArrayList<String>();

    sessionIDs.add(sessionID);
    sessionIDsByDest.put(sessionID, sessionIDs);
  }

  public static void closeSessionByDestination(String destination) throws IOException {

    var sessionIDs = sessionIDsByDest.get(destination);

    if (sessionIDs != null) {
      // sessionIDs를 순회하며 해당하는 WebSocketSession을 close()
      for (var sessionID : sessionIDs) {
        // CloseStatus로 퇴장 상태 알리고 userSessions 에서 제거
        userSessions.get(sessionID).close(CloseStatus.GOING_AWAY);
        userSessions.remove(sessionID);
      }
      // 마지막으로 sessionIDsByDest 에서 제거
      sessionIDsByDest.remove(destination);
    }
  }
}