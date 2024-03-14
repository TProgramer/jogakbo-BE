package com.noyes.jogakbo.user.DTO;

import java.util.List;

import com.noyes.jogakbo.album.DTO.AlbumInfo;
import com.noyes.jogakbo.album.DTO.AlbumInvitationMessage;
import com.noyes.jogakbo.user.Role;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserProfile {

  private String userUUID;
  private String nickname;
  private String profileImageURL;
  private Role role;
  private List<UserInfo> friends;
  private List<UserInfo> friendRequesters;
  private List<AlbumInfo> albums;
  private List<AlbumInfo> collaboAlbums;
  private List<AlbumInvitationMessage> albumInviters;
}
