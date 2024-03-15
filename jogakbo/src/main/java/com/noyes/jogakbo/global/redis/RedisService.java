package com.noyes.jogakbo.global.redis;

import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  // 직접 만든 redisTemplate 사용
  @SuppressWarnings("null")
  public void setAlbumRedisValue(String albumUUID, AlbumImagesInfo albumImagesInfo) {

    albumImagesInfo.setLastModifiedDate(LocalDateTime.now());
    try {

      redisTemplate.opsForValue().set(albumUUID, objectMapper.writeValueAsString(albumImagesInfo));
    } catch (JsonProcessingException e) {

      e.printStackTrace();
    }
  }

  public <T> T getAlbumRedisValue(String albumUUID, Class<T> classType) {

    @SuppressWarnings("null")
    String redisValue = (String) redisTemplate.opsForValue().get(albumUUID);
    T albumRedisValue = null;
    try {

      albumRedisValue = objectMapper.readValue(redisValue, classType);
    } catch (JsonProcessingException e) {

      e.printStackTrace();
    }
    return albumRedisValue;
  }

  /**
   * Album data of albumUUID will be removed in redis.
   * 
   * @param
   */
  @SuppressWarnings("null")
  public void removeAlbumRedisValue(String albumUUID) {

    redisTemplate.delete(albumUUID);
  }
}
