package com.noyes.jogakbo.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noyes.jogakbo.album.AlbumService;
import com.noyes.jogakbo.album.DTO.AlbumInfo;
import com.noyes.jogakbo.user.DTO.Friend;
import com.noyes.jogakbo.user.DTO.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MyPageService {

  private final UserService userService;
  private final AlbumService albumService;

  /**
   * Id에 해당하는 User의 Profile 반환
   * 
   * @param userUUID
   * @return
   */
  public UserProfile getUserProfile(String userUUID) {

    User user = userService.getUser(userUUID);

    // 친구들 정보 불러오기
    List<String> friendsUUIDs = user.getFriends();
    List<Friend> friends = userService.getFriends(friendsUUIDs);

    // 친구 요청을 보낸 유저 정보 불러오기
    List<String> friendRequestersUUIDs = user.getFriendRequesters();
    List<Friend> friendRequesters = userService.getFriends(friendRequestersUUIDs);

    // 소유하고 있는 앨범 정보 불러오기
    List<String> albumsUUIDs = user.getAlbums();
    List<AlbumInfo> albums = albumService.getAlbumInfo(albumsUUIDs);

    // 공동 작업 중인 앨범 정보 불러오기
    List<String> collaboAlbumsUUIDs = user.getCollaboAlbums();
    List<AlbumInfo> collaboAlbums = albumService.getAlbumInfo(collaboAlbumsUUIDs);

    // 초대받은 앨범 정보 불러오기
    List<String> albumInvitersUUIDs = user.getAlbumInviters();
    List<AlbumInfo> albumInviters = albumService.getAlbumInfo(albumInvitersUUIDs);

    return UserProfile.builder()
        .nickname(user.getNickname())
        .profileImageUrl(user.getProfileImageUrl())
        .friends(friends)
        .receivedFriendRequest(friendRequesters)
        .albums(albums)
        .collaboAlbums(collaboAlbums)
        .receivedAlbumInvitations(albumInviters)
        .build();
  }
}
