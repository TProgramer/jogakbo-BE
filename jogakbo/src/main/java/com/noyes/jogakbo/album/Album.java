package com.noyes.jogakbo.album;

import java.util.HashSet;
import java.util.Set;

import org.springframework.web.socket.WebSocketSession;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Album {
  private String albumId;
  private String name;
  private Set<WebSocketSession> sessions = new HashSet<>();

  @Builder
  public Album(String albumId, String name) {
    this.albumId = albumId;
    this.name = name;
  }
}
