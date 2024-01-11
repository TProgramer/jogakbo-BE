package com.noyes.jogakbo.album;

import java.util.List;

import org.springframework.data.redis.core.RedisHash;

import com.noyes.jogakbo.album.DTO.ImagesInPage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nonapi.io.github.classgraph.json.Id;

@Data
@Builder
@AllArgsConstructor
@RedisHash("AlbumImagesInfo")
public class AlbumImagesInfo {

  @Id
  private String id;

  private List<List<ImagesInPage>> imagesInfo;
}
