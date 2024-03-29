package com.noyes.jogakbo.album.DTO;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlbumInitInfo {

  private String albumName;
  private List<List<AlbumImageInfo>> imagesInfo;
}
