package com.noyes.jogakbo.album.DTO;

import lombok.Data;

@Data
public class AlbumImageEditMessage {

  private String imageUUID;
  private ImageInfo imageInfo;
}
