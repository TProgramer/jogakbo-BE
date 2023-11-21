package com.noyes.jogakbo.album;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AlbumService {

  private final ObjectMapper objectMapper;
  private Map<String, Album> albums;

  @PostConstruct
  private void init() {
    albums = new LinkedHashMap<>();
  }

  public List<Album> findAllAlbum() {
    return new ArrayList<>(albums.values());
  }

  public Album findAlbumById(String albumId) {
    return albums.get(albumId);
  }

  public Album createAlbum(String name) {
    String randomId = UUID.randomUUID().toString();
    Album chatRoom = Album.builder()
        .albumId(randomId)
        .name(name)
        .build();
    albums.put(randomId, chatRoom);
    return chatRoom;
  }
}