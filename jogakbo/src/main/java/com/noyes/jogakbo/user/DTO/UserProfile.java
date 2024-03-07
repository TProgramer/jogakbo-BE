package com.noyes.jogakbo.user.DTO;

import java.util.List;

import com.noyes.jogakbo.album.DTO.AlbumInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfile {

  private String userUUID;
  private String nickname;
  private String profileImageURL;
  private List<UserInfo> friends;
  private List<UserInfo> friendRequesters;
  private List<AlbumInfo> albums;
  private List<AlbumInfo> collaboAlbums;
  private List<AlbumInfo> albumInviters;
}
