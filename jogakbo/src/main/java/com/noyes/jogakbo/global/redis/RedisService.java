package com.noyes.jogakbo.global.redis;

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
  public void setAlbumRedisValue(String albumUUID, AlbumImagesInfo albumImagesInfo)
      throws JsonProcessingException {

    redisTemplate.opsForValue().set(albumUUID, objectMapper.writeValueAsString(albumImagesInfo));
  }

  public <T> T getAlbumRedisValue(String albumUUID, Class<T> classType)
      throws JsonProcessingException {

    String redisValue = (String) redisTemplate.opsForValue().get(albumUUID);

    return objectMapper.readValue(redisValue, classType);
  }

  /**
   * Album data of albumUUID will be removed in redis.
   * 
   * @param
   */
  public void removeAlbumRedisValue(String albumUUID) {

    redisTemplate.delete(albumUUID);
  }
}
