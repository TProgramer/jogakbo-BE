package com.noyes.jogakbo.album.DTO;

import java.util.List;

import com.noyes.jogakbo.user.DTO.UserInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlbumMemberInfo {

  private UserInfo albumOwnerInfo;
  private List<UserInfo> albumEditorsInfos;
  private List<UserInfo> albumInviteesInfos;
}
