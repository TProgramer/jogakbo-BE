package com.noyes.jogakbo.user.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Friend {

  private String nickname;
  private String userUUID;
  private String profileImageURL;
}
