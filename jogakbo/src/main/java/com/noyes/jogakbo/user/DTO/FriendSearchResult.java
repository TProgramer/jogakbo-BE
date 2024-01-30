package com.noyes.jogakbo.user.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendSearchResult {

  private Friend friend;
  private FriendStatus friendStatus;
}
