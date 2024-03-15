package com.noyes.jogakbo.global.redis;

import java.util.List;
import java.util.ArrayList;

import org.springframework.data.redis.core.RedisHash;

import com.noyes.jogakbo.album.DTO.AlbumImageInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("AlbumImagesInfo")
public class AlbumImagesInfo {

  @Id
  private String id;
  @Builder.Default
  private List<List<AlbumImageInfo>> imagesInfo = List.of(new ArrayList<>());
}
