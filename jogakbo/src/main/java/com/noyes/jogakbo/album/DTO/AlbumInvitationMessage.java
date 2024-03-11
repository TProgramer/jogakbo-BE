package com.noyes.jogakbo.album.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlbumInvitationMessage {

  private String albumUUId;
  private String albumName;
  private String albumOwner;
}
