package com.noyes.jogakbo.global.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noyes.jogakbo.album.AlbumImagesInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  // 직접 만든 redisTemplate 사용
  public void setAlbumRedisValue(String albumID, AlbumImagesInfo albumImagesInfo) throws JsonProcessingException {

    redisTemplate.opsForValue().set(albumID, objectMapper.writeValueAsString(albumImagesInfo));
  }

  public <T> T getAlbumRedisValue(String albumID, Class<T> classType) throws JsonProcessingException {

    String redisValue = (String) redisTemplate.opsForValue().get(albumID);

    return objectMapper.readValue(redisValue, classType);
  }
}
