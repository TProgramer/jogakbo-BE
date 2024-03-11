package com.noyes.jogakbo.album.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlbumInvitationMessage {

  private String albumUUID;
  private String albumName;
  private String albumOwnerName;
}
