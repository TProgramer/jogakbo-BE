package com.noyes.jogakbo.user.DTO;

import java.util.List;

import com.noyes.jogakbo.album.Album;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfile {

  private String nickname;
  private String profileImageUrl;
  private List<Friend> friends;
  private List<Friend> sentFriendRequest;
  private List<Friend> receivedFriendRequest;
  private List<Album> albums;
}
