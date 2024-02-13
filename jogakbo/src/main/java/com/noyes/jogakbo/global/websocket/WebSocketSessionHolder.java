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

    // 처음 등록되는 destination 이면, 새로운 ArrayList 할당 후 등록
    var sessionIDs = sessionIDsByDest.get(destination);

    if (sessionIDs == null)
      sessionIDs = new ArrayList<String>();

    // destination 을 키 값으로 sessionID를 추가
    sessionIDs.add(sessionID);
    sessionIDsByDest.put(destination, sessionIDs);
  }

  public static void removeSession(WebSocketSession session) {

    userSessions.remove(session.getId());
  }

  public static void closeSessionByDestination(String destination) throws IOException {

    var sessionIDs = sessionIDsByDest.get(destination);

    if (sessionIDs != null) {
      // sessionIDs를 순회하며 해당하는 WebSocketSession을 close()
      for (var sessionID : sessionIDs) {
        // session 이 살아있다면, userSessions 에서 제거
        var session = userSessions.get(sessionID);

        if (session != null) {

          session.close(CloseStatus.GOING_AWAY);

          // userSessions 에서도 제거
          userSessions.remove(sessionID);
        }
      }
      // 마지막으로 sessionIDsByDest 에서 제거
      sessionIDsByDest.remove(destination);
    }
  }
}