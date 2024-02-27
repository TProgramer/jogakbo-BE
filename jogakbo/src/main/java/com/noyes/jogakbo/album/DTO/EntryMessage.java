package com.noyes.jogakbo.album.DTO;

import java.time.LocalDateTime;
import java.util.List;

import com.noyes.jogakbo.user.DTO.UserInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntryMessage {

  private String albumName;
  private List<List<ImagesInPage>> imagesInfo;
  private String thumbnailImage;
  private LocalDateTime createdDate;
  private UserInfo albumOwnerInfo;
  private List<UserInfo> albumEditorsInfo;
  private List<UserInfo> sentAlbumInvitationsInfo;
}
