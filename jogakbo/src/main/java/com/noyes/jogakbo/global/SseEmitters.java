package com.noyes.jogakbo.global;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.noyes.jogakbo.user.DTO.Friend;

@Component
public class SseEmitters {

  // onCompletion가 다른 쓰레드에서 실행되기에 thread-safe한 ConcurrentHashMap 활용
  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter add(String socialID, SseEmitter emitter) {

    this.emitters.put(socialID, emitter);

    emitter.onCompletion(() -> this.emitters.values().remove(emitter));

    emitter.onTimeout(() -> emitter.complete());

    return emitter;
  }

  public void sendFriendRequestAlarm(String socialID, Friend requestUser) {

    try {

      this.emitters.get(socialID).send(SseEmitter.event()
          .name("friendRequest")
          .data(requestUser));

    } catch (IOException e) {

      throw new RuntimeException(e);
    }
  }
}