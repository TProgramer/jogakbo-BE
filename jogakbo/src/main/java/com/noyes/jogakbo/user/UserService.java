package com.noyes.jogakbo.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.noyes.jogakbo.global.jwt.JwtService;
import com.noyes.jogakbo.global.s3.AwsS3Service;
import com.noyes.jogakbo.user.DTO.UserSearchResult;
import com.noyes.jogakbo.user.DTO.FriendStatus;
import com.noyes.jogakbo.user.DTO.UserInfo;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final AwsS3Service awsS3Service;

  @SuppressWarnings("null")
  public Optional<User> checkUser(HttpServletResponse response, String accessToken) {

    String userUUID = jwtService.extractUserUUID(accessToken).get();
    Optional<User> targetUser = userRepository.findById(userUUID);

    if (targetUser.isPresent())
      return updateToken(response, userUUID, targetUser);

    return registUser(response, userUUID, accessToken);
  }

  public Optional<User> updateToken(HttpServletResponse response, String userUUID, Optional<User> targetUser) {

    String newAccessToken = jwtService.createAccessToken(userUUID);
    String newRefreshToken = jwtService.createRefreshToken();

    // 응답 헤더에 AccessToken, RefreshToken 실어서 응답
    jwtService.sendAccessAndRefreshToken(response, newAccessToken, newRefreshToken);

    User user = targetUser.get();
    user.updateRefreshToken(newRefreshToken);
    userRepository.save(user);
    log.info("=============================================");
    log.info("로그인에 성공하였습니다. userUUID : {}", userUUID);
    log.info("AccessToken : {}", newAccessToken);

    return targetUser;
  }

  public Optional<User> registUser(HttpServletResponse response, String userUUID, String accessToken) {

    String newAccessToken = jwtService.createAccessToken(userUUID);
    String newRefreshToken = jwtService.createRefreshToken();

    // 응답 헤더에 AccessToken, RefreshToken 실어서 응답
    jwtService.sendAccessAndRefreshToken(response, newAccessToken, newRefreshToken);

    // 응답 헤더에 신규 유저임을 표시
    response.setHeader("userStatus", "newUser");

    String nickname = jwtService.extractName(accessToken).get();
    String provider = jwtService.extractProvider(accessToken).get();

    User user = User.builder()
        .userUUID(userUUID)
        .nickname(nickname)
        .provider(provider)
        .build();

    user.updateRefreshToken(newRefreshToken);
    userRepository.save(user);
    log.info("=============================================");
    log.info("로그인에 성공하였습니다. userUUID : {}", userUUID);
    log.info("AccessToken : {}", newAccessToken);

    return Optional.ofNullable(user);
  }

  /**
   * <pre>
   * Refresh Token을 재발급하고 DB에 Refresh Token을 업데이트하는 메소드
   * jwtService.createRefreshToken()으로 Refresh Token을 재발급 후
   * DB에 재발급한 Refresh Token 업데이트 후 Flush
   * </pre>
   */
  public void reIssueRefreshToken(HttpServletResponse response, String refreshToken) {

    userRepository.findByRefreshToken(refreshToken)
        .ifPresent(user -> {
          String reIssuedRefreshToken = jwtService.createRefreshToken();
          user.updateRefreshToken(reIssuedRefreshToken);
          userRepository.save(user);
          jwtService.sendAccessAndRefreshToken(response, jwtService.createAccessToken(user.getUserUUID()),
              reIssuedRefreshToken);
        });
  }

  public void deleteRefreshToken(HttpServletResponse response, String userUUID) {

    User user = getUser(userUUID);
    user.updateRefreshToken(null);
    userRepository.save(user);
    log.info("리프레시 토큰 삭제 완료!");
    response.setStatus(HttpServletResponse.SC_OK);
  }

  public void addAlbum(String albumName, @NonNull String userUUID) {

    User targetUser = userRepository.findById(userUUID).get();
    List<String> albums = targetUser.getAlbums();
    albums.add(albumName);
    userRepository.save(targetUser);
  }

  /**
   * Id에 해당하는 User 조회
   * JPA Repository의 findBy Method를 사용하여 특정 User를 조회
   * find 메소드는 NULL 값일 수도 있으므로 Optional<T>를 반환하지만,
   * Optional 객체의 get() 메소드를 통해 Entity로 변환해서 반환함.
   * 
   * @param id
   * @return
   */
  public User getUser(@NonNull String userUUID) {

    return userRepository.findById(userUUID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지않은 유저 ID 입니다."));
  }

  /**
   * userUUID 에 해당하는 UserInfo 반환
   * JPA Repository의 findBy Method를 사용하여 특정 User를 조회
   * find 메소드는 NULL 값일 수도 있으므로 Optional<T>를 반환하지만,
   * Optional 객체의 get() 메소드를 통해 Entity로 변환해서 반환함.
   * 
   * @param
   * @return
   */
  public UserInfo getUserInfo(@NonNull String userUUID) {

    User user = getUser(userUUID);

    return UserInfo.builder()
        .userUUID(user.getUserUUID())
        .nickname(user.getNickname())
        .profileImageURL(user.getProfileImageUrl()).build();
  }

  /**
   * Id에 해당하는 User 삭제
   * JPA Repository의 deleteBy Method를 사용하여 특정 User를 삭제
   * 
   * @param id
   */
  public void deleteUser(String userUUID) {

    userRepository.deleteByUserUUID(userUUID);
  }

  /**
   * nickname에 해당하는 List<User> 조회
   * JPA Repository의 findBy Method를 사용하여 특정 User 리스트 조회
   * 
   * @param id
   */
  public List<UserSearchResult> searchFriend(String nickname, @NonNull String userUUID) {

    // nickname을 기준으로 본인을 제외한 유저 불러오기
    List<User> targetUsers = userRepository.findAllByNicknameContainsAndUserUUIDNot(nickname, userUUID).get();

    // 이미 친구인 유저나 이미 요청을 보낸 유저ID 불러오기
    User user = userRepository.findById(userUUID).get();

    List<String> filterFriendUserUUID = user.getFriends();

    List<String> filterWaitingUserUUID = user.getFriendRequestees();

    // 필터링한 Friend 목록 추출하기
    List<UserSearchResult> searchResult = new ArrayList<>();

    for (User target : targetUsers) {

      FriendStatus friendStatus;

      if (filterFriendUserUUID.contains(target.getUserUUID()))
        friendStatus = FriendStatus.FRIEND;
      else if (filterWaitingUserUUID.contains(target.getUserUUID()))
        friendStatus = FriendStatus.WAITING;
      else
        friendStatus = FriendStatus.STRANGER;

      UserInfo friend = UserInfo.builder()
          .nickname(target.getNickname())
          .userUUID(target.getUserUUID())
          .profileImageURL(target.getProfileImageUrl())
          .build();

      UserSearchResult friendSearchResult = UserSearchResult.builder()
          .friend(friend)
          .friendStatus(friendStatus)
          .build();

      searchResult.add(friendSearchResult);
    }

    return searchResult;
  }

  /**
   * socialID에 해당하는 User에게 친구 신청 보내기
   * JPA Repository의 findBy Method를 사용하여 특정 User 조회
   * 
   * @param userUUID
   */
  public UserInfo sendFriendRequest(@NonNull String reqUserUUID, @NonNull String resUserUUID) {

    User requestUser = userRepository.findById(reqUserUUID).get();

    // 이미 요청을 보낸 대상일 경우 예외처리
    List<String> friendRequestees = requestUser.getFriendRequestees();
    if (isUserInFriendList(friendRequestees, resUserUUID))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 친구 요청을 보냈습니다.");

    // 이미 친구인 경우도 예외처리
    List<String> friends = requestUser.getFriends();
    if (isUserInFriendList(friends, resUserUUID))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 친구인 유저입니다.");

    // 서로의 보낸|받은 친구 요청 리스트에 등록
    friendRequestees.add(resUserUUID);
    userRepository.save(requestUser);

    User responseUser = userRepository.findById(resUserUUID).get();
    List<String> friendRequesters = responseUser.getFriendRequesters();
    friendRequesters.add(reqUserUUID);
    userRepository.save(responseUser);

    return UserInfo.builder()
        .nickname(requestUser.getNickname())
        .userUUID(requestUser.getUserUUID())
        .profileImageURL(requestUser.getProfileImageUrl())
        .build();
  }

  /**
   * socialID에 해당하는 User에게 친구 신청 보내기
   * JPA Repository의 findBy Method를 사용하여 특정 User 조회
   * 
   * @param userUUID
   */
  public String replyFriendRequest(@NonNull String reqUserUUID, @NonNull String resUserUUID, String reply) {

    // reqUserUUID에 해당하는 유저가 존재할 때
    User requestUser = getUser(reqUserUUID);

    // 보낸|받은 요청 유효성 판별 변수 초기화
    boolean isValidRequest = true;

    // 친구 요청을 보낸 유저인지 확인
    List<String> friendRequestees = requestUser.getFriendRequestees();
    if (!isUserInFriendList(friendRequestees, resUserUUID))
      isValidRequest = false;

    // 친구 요청을 받은 유저인지도 확인
    User responseUser = getUser(resUserUUID);

    List<String> friendRequesters = responseUser.getFriendRequesters();
    if (!isUserInFriendList(friendRequesters, reqUserUUID))
      isValidRequest = false;

    // 서로의 요청, 승인 대기열에서 삭제
    friendRequestees.remove(resUserUUID);
    friendRequesters.remove(reqUserUUID);

    userRepository.save(requestUser);
    userRepository.save(responseUser);

    if (!isValidRequest)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "더 이상 유효하지 않은 친구 요청입니다.");

    // 서로의 친구 목록에 추가
    if (reply.equals("accept")) {

      requestUser.getFriends().add(resUserUUID);
      responseUser.getFriends().add(reqUserUUID);
    }

    // 서로 친구 추가 요청을 보냈을 경우 예외처리
    List<String> doubleCheckFriendRequestees = responseUser.getFriendRequestees();
    if (isUserInFriendList(doubleCheckFriendRequestees, reqUserUUID))
      doubleCheckFriendRequestees.remove(reqUserUUID);

    List<String> doubleCheckFriendRequesters = requestUser.getFriendRequesters();
    if (isUserInFriendList(doubleCheckFriendRequesters, resUserUUID))
      doubleCheckFriendRequesters.remove(resUserUUID);

    // Entity 저장
    userRepository.save(requestUser);
    userRepository.save(responseUser);

    return "성공적으로 친구를 추가했습니다.";
  }

  /**
   * friendUUIDs에 userUUID가 있는지 TF를 반환
   * 
   * @param userUUID
   */
  public boolean isUserInFriendList(List<String> friendUUIDs, String userUUID) {

    for (String friendUUID : friendUUIDs) {

      if (friendUUID.equals(userUUID))
        return true;
    }
    return false;
  }

  /**
   * userUUID에 해당하는 User를 친구 목록에서 삭제
   * 
   * @param userUUID
   */
  public String removeFriend(@NonNull String targetUserUUID, @NonNull String userUUID) {

    // 친구 목록에서 삭제 작업
    User user = getUser(userUUID);
    List<String> friends = user.getFriends();

    if (isUserInFriendList(friends, targetUserUUID))
      friends.remove(targetUserUUID);
    else
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "친구가 아닌 유저입니다.");

    userRepository.save(user);

    // 상대방의 친구 목록에서도 삭제
    User targetUser = getUser(targetUserUUID);
    List<String> targetUserFriends = targetUser.getFriends();

    if (isUserInFriendList(targetUserFriends, userUUID))
      targetUserFriends.remove(userUUID);
    else
      return "대상 유저의 친구 목록에 존재하지 않습니다.";

    userRepository.save(targetUser);

    return "정상적으로 친구 삭제 작업이 완료되었습니다.";
  }

  /**
   * socialID에 해당하는 유저의 nickname과 profileImage 수정하기
   * 유저 간 nickname은 중복가능하며
   * 기존 profileImage는 삭제 처리
   * 
   * @param
   * @return 실행 결과
   */
  public String updateProfile(String newNickname, MultipartFile profileImage, @NonNull String userUUID) {

    User user = getUser(userUUID);

    // 기존 nickname과 동일한지 확인 후 수정
    if (!user.getNickname().equals(newNickname))
      user.setNickname(newNickname);

    // 기존 profileImage와 동일한지 확인 후 수정
    String profileImageOriginalName = user.getProfileImageOriginalName();
    if (profileImage != null && !profileImageOriginalName.equals(profileImage.getOriginalFilename())) {

      // S3에 업로드 시도 후, 업로드 된 S3 파일명 리스트로 받아오기
      String uploadFileName = awsS3Service.uploadFile(profileImage, userUUID);

      // 새로운 profileImage 정보로 User entity 업데이트
      user.setProfileImageUrl(uploadFileName);
      user.setProfileImageOriginalName(profileImage.getOriginalFilename());

      // 기존의 profileImage 삭제
      awsS3Service.deleteFile(profileImageOriginalName, userUUID);
    }

    userRepository.save(user);

    return "프로필을 성공적으로 변경했습니다.";
  }

  /**
   * collaboUserUUID에 해당하는 유저 Entity의 albumInviters 필드에 albumUUID를 추가
   * 
   * @param collaboUserUUID
   * @param album
   */
  public void addAlbumInviters(@NonNull String collaboUserUUID, String albumUUID) {

    User user = getUser(collaboUserUUID);
    user.getAlbumInviters().add(albumUUID);
    userRepository.save(user);
  }

  /**
   * resUserID에 해당하는 유저 Entity의 albumInviters 필드에서 일치하는 albumUUID 제거
   * 
   * @param collaboUserUUID
   * @param album
   */
  public void removeAlbumInvitation(@NonNull String resUserUUID, String albumUUID) {

    User user = getUser(resUserUUID);
    user.getAlbumInviters().remove(albumUUID);
    userRepository.save(user);
  }

  /**
   * collaboUserUUID에 해당하는 유저 Entity의 receivedAlbumInvitations 필드에 album을 추가
   * 
   * @param collaboUserUUID
   * @param album
   */
  public void addCollaboAlbum(@NonNull String collaboUserUUID, String albumUUID) {

    User user = getUser(collaboUserUUID);
    user.getCollaboAlbums().add(albumUUID);
    userRepository.save(user);
  }

  /**
   * userUUID 배열에 해당하는 모든 유저 정보를 List<Friend>로 반환
   * 
   * @param userUUIDs
   * @return
   */
  public List<UserInfo> getUserInfos(List<String> userUUIDs) {

    List<UserInfo> friends = List.of();
    for (String userUUID : userUUIDs) {

      User user = getUser(userUUID);

      UserInfo friend = UserInfo.builder()
          .userUUID(user.getUserUUID())
          .nickname(user.getNickname())
          .profileImageURL(user.getProfileImageUrl())
          .build();

      friends.add(friend);
    }

    return friends;
  }
}
