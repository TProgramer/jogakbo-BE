package com.noyes.jogakbo.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noyes.jogakbo.album.Album;
import com.noyes.jogakbo.album.AlbumService;
import com.noyes.jogakbo.album.DTO.AlbumInfo;
import com.noyes.jogakbo.album.DTO.AlbumInvitationMessage;
import com.noyes.jogakbo.user.DTO.UserInfo;
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
    List<UserInfo> friends = userService.getUserInfos(friendsUUIDs);

    // 친구 요청을 보낸 유저 정보 불러오기
    List<String> friendRequestersUUIDs = user.getFriendRequesters();
    List<UserInfo> friendRequesters = userService.getUserInfos(friendRequestersUUIDs);

    // 소유하고 있는 앨범 정보 불러오기
    List<String> albumsUUIDs = user.getAlbums();
    List<AlbumInfo> albums = albumService.getAlbumInfo(albumsUUIDs);

    // 공동 작업 중인 앨범 정보 불러오기
    List<String> collaboAlbumsUUIDs = user.getCollaboAlbums();
    List<AlbumInfo> collaboAlbums = albumService.getAlbumInfo(collaboAlbumsUUIDs);

    // 초대받은 앨범 정보 불러오기
    List<String> albumInvitersUUIDs = user.getAlbumInviters();
    List<AlbumInvitationMessage> albumInviters = new ArrayList<>();
    for (String albumInviterUUID : albumInvitersUUIDs) {

      Album album = albumService.getAlbum(albumInviterUUID);
      String albumOwnerName = userService.getUser(album.getAlbumOwner()).getNickname();
      AlbumInvitationMessage albumInvitationMessage = AlbumInvitationMessage.builder()
          .albumUUID(album.getAlbumUUID())
          .albumName(album.getAlbumName())
          .albumOwnerName(albumOwnerName)
          .build();

      albumInviters.add(albumInvitationMessage);
    }

    return UserProfile.builder()
        .userUUID(userUUID)
        .nickname(user.getNickname())
        .profileImageURL(user.getProfileImageUrl())
        .friends(friends)
        .friendRequesters(friendRequesters)
        .albums(albums)
        .collaboAlbums(collaboAlbums)
        .albumInviters(albumInviters)
        .build();
  }
}
