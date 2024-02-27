package com.noyes.jogakbo.user.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfo {

  private String socialID;
  private String nickname;
  private String profileImageURL;
}
