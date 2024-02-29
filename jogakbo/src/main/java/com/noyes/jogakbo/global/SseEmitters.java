package com.noyes.jogakbo.global;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.noyes.jogakbo.album.Album;
import com.noyes.jogakbo.user.DTO.Friend;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SseEmitters {

  // onCompletion가 다른 쓰레드에서 실행되기에 thread-safe한 ConcurrentHashMap 활용
  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter add(String userUUID, SseEmitter emitter) {

    this.emitters.put(userUUID, emitter);

    emitter.onCompletion(() -> this.emitters.values().remove(emitter));

    emitter.onTimeout(() -> emitter.complete());

    return emitter;
  }

  public String sendFriendRequestAlarm(String userUUID, Friend requestUser) {

    try {

      this.emitters.get(userUUID).send(SseEmitter.event()
          .name("friendRequest")
          .data(requestUser));

    } catch (IOException e) {

      throw new RuntimeException(e);

    } catch (NullPointerException npe) {

      return "해당 유저가 오프라인 상태입니다.";
    }

    return "친구 추가 요청을 완료했습니다.";
  }

  /**
   * send SSE alarm to user corresponding `collaboUserID` with `requestAlbum` info
   * 
   * @param userUUID
   * @param requestAlbum
   * @return Result info in String
   */
  public String sendAlbumInvitation(String collaboUserID, Album requestAlbum) {

    try {

      this.emitters.get(collaboUserID).send(SseEmitter.event()
          .name("albumInvitation")
          .data(requestAlbum));

    } catch (IOException e) {

      throw new RuntimeException(e);

    } catch (NullPointerException npe) {

      return "해당 유저가 오프라인 상태입니다.";
    }

    return "앨범 초대 요청을 완료했습니다.";
  }
}