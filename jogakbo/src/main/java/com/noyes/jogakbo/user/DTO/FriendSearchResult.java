package com.noyes.jogakbo.user.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendSearchResult {

  private UserInfo friend;
  private FriendStatus friendStatus;
}
