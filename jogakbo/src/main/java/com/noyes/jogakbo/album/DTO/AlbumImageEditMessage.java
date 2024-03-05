package com.noyes.jogakbo.album.DTO;

import lombok.Data;

@Data
public class AlbumImageEditMessage {

  private String albumImageUUID;
  private AlbumImageEditInfo albumImageEditInfo;
}
