package com.noyes.jogakbo.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.noyes.jogakbo.album.Album;
import com.noyes.jogakbo.global.jwt.JwtService;
import com.noyes.jogakbo.global.s3.AwsS3Service;
import com.noyes.jogakbo.user.DTO.Friend;
import com.noyes.jogakbo.user.DTO.FriendSearchResult;
import com.noyes.jogakbo.user.DTO.FriendStatus;
import com.noyes.jogakbo.user.DTO.UserInfo;
import com.noyes.jogakbo.user.DTO.UserProfile;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final AwsS3Service awsS3Service;

  /**
   * User 생성
   * JPA Repository의 save Method를 사용하여 객체를 생성
   * Entity인 Model 객체에 @Id로 설정한 키 값이 없을 경우 해당하는 데이터를 추가
   * 만약 추가하려는 Entity인 Model 객체에 @Id 값이 이미 존재하면 갱신되기 때문에
   * 아래와 같이 추가하고자 하는 User가 존재하는지 체크하는 로직을 추가
   * 
   * @param model
   * @return
   */
  public void signUp(UserSignUpDTO userSignUpDto) throws Exception {

    if (userRepository.findById(userSignUpDto.getSocialId()).isPresent())
      throw new Exception("이미 존재하는 이메일입니다.");

    User user = User.builder()
        .socialID(userSignUpDto.getSocialId())
        .nickname(userSignUpDto.getNickname())
        .role(Role.USER)
        .build();

    userRepository.save(user);
  }

  public Optional<User> checkUser(HttpServletResponse response, String accessToken) {

    String socialID = jwtService.extractSocialId(accessToken).get();
    Optional<User> targetUser = userRepository.findById(socialID);

    if (targetUser.isPresent())
      return updateToken(response, socialID, targetUser);

    return registUser(response, socialID, accessToken);
  }

  public Optional<User> updateToken(HttpServletResponse response, String socialID, Optional<User> targetUser) {

    String newAccessToken = jwtService.createAccessToken(socialID);
    String newRefreshToken = jwtService.createRefreshToken();

    // 응답 헤더에 AccessToken, RefreshToken 실어서 응답
    jwtService.sendAccessAndRefreshToken(response, newAccessToken, newRefreshToken);

    User user = targetUser.get();
    user.updateRefreshToken(newRefreshToken);
    userRepository.save(user);
    log.info("=============================================");
    log.info("로그인에 성공하였습니다. socialID : {}", socialID);
    log.info("AccessToken : {}", newAccessToken);

    return targetUser;
  }

  public Optional<User> registUser(HttpServletResponse response, String socialID, String accessToken) {

    String newAccessToken = jwtService.createAccessToken(socialID);
    String newRefreshToken = jwtService.createRefreshToken();

    // 응답 헤더에 AccessToken, RefreshToken 실어서 응답
    jwtService.sendAccessAndRefreshToken(response, newAccessToken, newRefreshToken);

    // 응답 헤더에 신규 유저임을 표시
    response.setHeader("userStatus", "newUser");

    String nickname = jwtService.extractName(accessToken).get();
    String provider = jwtService.extractProvider(accessToken).get();

    User user = User.builder()
        .socialID(socialID)
        .nickname(nickname)
        .provider(provider)
        .profileImageOriginalName("")
        .albums(new ArrayList<>())
        .friends(new ArrayList<>())
        .sentFriendRequests(new ArrayList<>())
        .receivedFriendRequests(new ArrayList<>())
        .role(Role.USER)
        .build();

    user.updateRefreshToken(newRefreshToken);
    userRepository.save(user);
    log.info("=============================================");
    log.info("로그인에 성공하였습니다. socialID : {}", socialID);
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
          jwtService.sendAccessAndRefreshToken(response, jwtService.createAccessToken(user.getSocialID()),
              reIssuedRefreshToken);
        });
  }

  public void deleteRefreshToken(HttpServletResponse response, String socialID) {

    Optional<User> targetUser = getUser(socialID);

    if (!targetUser.isPresent()) {

      log.info("잘못된 요청값");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

      return;
    }

    User user = targetUser.get();
    user.updateRefreshToken(null);
    userRepository.save(user);
    log.info("리프레시 토큰 삭제 완료!");
    response.setStatus(HttpServletResponse.SC_OK);
  }

  public Album getAlbumByUser(@NonNull String socialID, String albumID) {

    List<Album> albums = userRepository.findById(socialID).get().getAlbums();
    for (Album album : albums) {

      if (album.getAlbumUUID().equals(albumID))
        return album;
    }

    return null;
  }

  public void addAlbum(Album newAlbum, @NonNull String socialID) {

    User targetUser = userRepository.findById(socialID).get();
    List<Album> albums = targetUser.getAlbums();
    albums.add(newAlbum);
    userRepository.save(targetUser);
  }

  /**
   * User 수정
   * JPA Repository의 save Method를 사용하여 객체를 갱신
   * Entity인 Model 객체에 @Id로 설정한 키 값이 존재할 경우 해당하는 데이터를 갱신
   * 만약 수정하려는 Entity인 Model 객체에 @Id 값이 존재하지 않으면 데이터가 추가되기 때문에
   * 아래와 같이 갱신하고자 하는 User가 존재하는지 체크하는 로직을 추가
   *
   * @param model
   * @return
   */
  public User updateUserInfo(UserDetails token, UserUpdateDTO updateData) {

    User updatedUser = null;

    try {

      if (updateData.isUserUpdateEmpty())
        throw new Exception("Required info is not qualified");

      User existUser = getUser(token.getUsername()).get();

      existUser.setNickname(updateData.getNickname());

      // if (!ObjectUtils.isEmpty(existUser))
      // updatedUser = userRepository.save(model);

    } catch (Exception e) {

      log.info("[Fail] e: " + e.toString());
    }

    return updatedUser;
  }

  /**
   * User List 조회
   * JPA Repository의 findAll Method를 사용하여 전체 User를 조회
   * 
   * @return
   */
  public List<User> getUsers() {
    return userRepository.findAll();
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
  public Optional<User> getUser(@NonNull String socialID) {

    return userRepository.findById(socialID);
  }

  /**
   * socialID 에 해당하는 UserInfo 반환
   * JPA Repository의 findBy Method를 사용하여 특정 User를 조회
   * find 메소드는 NULL 값일 수도 있으므로 Optional<T>를 반환하지만,
   * Optional 객체의 get() 메소드를 통해 Entity로 변환해서 반환함.
   * 
   * @param
   * @return
   */
  public UserInfo getUserInfo(@NonNull String socialID) {

    User user = userRepository.findById(socialID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지않은 유저 ID 입니다."));

    return UserInfo.builder()
        .socialID(user.getSocialID())
        .nickname(user.getNickname())
        .profileImageURL(user.getProfileImageUrl()).build();
  }

  /**
   * Id에 해당하는 User의 Profile 반환
   * 클래스 내의 getUser() Method를 사용하여 특정 User 객체를 얻어 처리함
   * 
   * @param socialID
   * @return
   */
  public UserProfile getUserProfile(String socialID) {

    User user = getUser(socialID).get();

    return UserProfile.builder()
        .nickname(user.getNickname())
        .profileImageUrl(user.getProfileImageUrl())
        .friends(user.getFriends())
        .sentFriendRequest(user.getSentFriendRequests())
        .receivedFriendRequest(user.getReceivedFriendRequests())
        .albums(user.getAlbums())
        .collaboAlbums(user.getCollaboAlbums())
        .receivedAlbumInvitations(user.getReceivedAlbumInvitations())
        .build();
  }

  /**
   * Id에 해당하는 User 삭제
   * JPA Repository의 deleteBy Method를 사용하여 특정 User를 삭제
   * 
   * @param id
   */
  public void deleteUser(String socialID) {

    userRepository.deleteBySocialID(socialID);
  }

  /**
   * nickname에 해당하는 List<User> 조회
   * JPA Repository의 findBy Method를 사용하여 특정 User 리스트 조회
   * 
   * @param id
   */
  public List<FriendSearchResult> searchFriend(String nickname, @NonNull String socialID) {

    // nickname을 기준으로 본인을 제외한 유저 불러오기
    List<User> targetUsers = userRepository.findAllByNicknameContainsAndSocialIDNot(nickname, socialID).get();

    // 이미 친구인 유저나 이미 요청을 보낸 유저ID 불러오기
    User user = userRepository.findById(socialID).get();

    List<String> filterFriendUsername = user.getFriends()
        .stream()
        .map(Friend::getSocialID)
        .collect(Collectors.toList());

    List<String> filterWaitingUsername = user.getSentFriendRequests()
        .stream()
        .map(Friend::getSocialID)
        .collect(Collectors.toList());

    // 필터링한 Friend 목록 추출하기
    List<FriendSearchResult> searchResult = new ArrayList<>();

    for (User target : targetUsers) {

      FriendStatus friendStatus;

      if (filterFriendUsername.contains(target.getSocialID()))
        friendStatus = FriendStatus.FRIEND;
      else if (filterWaitingUsername.contains(target.getSocialID()))
        friendStatus = FriendStatus.WAITING;
      else
        friendStatus = FriendStatus.STRANGER;

      Friend friend = Friend.builder()
          .nickname(target.getNickname())
          .socialID(target.getSocialID())
          .profileImageURL(target.getProfileImageUrl())
          .build();

      FriendSearchResult friendSearchResult = FriendSearchResult.builder()
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
   * @param socialID
   */
  public Friend sendFriendRequest(@NonNull String reqUserID, @NonNull String resUserID) {

    // 이미 요청을 보낸 대상일 경우 예외처리
    User requestUser = userRepository.findById(reqUserID).get();
    List<Friend> sentFriendRequest = requestUser.getSentFriendRequests();

    if (isUserInFriendList(sentFriendRequest, resUserID) != null)
      return null;

    // 이미 친구인 경우도 예외처리
    if (isUserInFriendList(requestUser.getFriends(), resUserID) != null)
      return null;

    // 친구 요청 리스트에 등록
    User responseUser = userRepository.findById(resUserID).get();
    List<Friend> receivedFriendRequest = responseUser.getReceivedFriendRequests();

    Friend responseFreind = Friend.builder()
        .nickname(responseUser.getNickname())
        .socialID(responseUser.getSocialID())
        .profileImageURL(responseUser.getProfileImageUrl())
        .build();

    sentFriendRequest.add(responseFreind);

    Friend requestFreind = Friend.builder()
        .nickname(requestUser.getNickname())
        .socialID(requestUser.getSocialID())
        .profileImageURL(requestUser.getProfileImageUrl())
        .build();

    receivedFriendRequest.add(requestFreind);

    userRepository.save(requestUser);
    userRepository.save(responseUser);

    return requestFreind;
  }

  /**
   * socialID에 해당하는 User에게 친구 신청 보내기
   * JPA Repository의 findBy Method를 사용하여 특정 User 조회
   * 
   * @param socialID
   */
  public String replyFriendRequest(@NonNull String reqUserID, @NonNull String resUserID, String reply) {

    // 보낸 | 받은 요청 유효성 판별 변수 추가
    boolean isValidRequest = true;

    // 친구 요청을 보낸 유저인지 확인
    User requestUser = userRepository.findById(reqUserID).get();
    List<Friend> sentFriendRequest = requestUser.getSentFriendRequests();

    Friend targetUser = isUserInFriendList(sentFriendRequest, resUserID);

    if (targetUser == null)
      isValidRequest = false;

    // 친구 요청을 받은 유저인지도 확인
    User responseUser = userRepository.findById(resUserID).get();
    List<Friend> receivedFriendRequest = responseUser.getReceivedFriendRequests();

    Friend callUser = isUserInFriendList(receivedFriendRequest, reqUserID);

    if (callUser == null)
      isValidRequest = false;

    // 서로의 요청, 승인 대기열에서 삭제
    sentFriendRequest.remove(targetUser);
    receivedFriendRequest.remove(callUser);

    userRepository.save(requestUser);
    userRepository.save(responseUser);

    if (!isValidRequest)
      return "더 이상 유효하지 않은 친구 요청입니다.";

    // 서로의 친구 목록에 추가
    if (reply.equals("accept")) {

      requestUser.getFriends().add(targetUser);
      responseUser.getFriends().add(callUser);
    }

    // 서로 친구 추가 요청을 보냈을 경우 예외처리
    List<Friend> doubleCheckSentList = responseUser.getSentFriendRequests();
    Friend doubleCheckSentUser = isUserInFriendList(doubleCheckSentList, reqUserID);

    if (doubleCheckSentUser != null)
      doubleCheckSentList.remove(doubleCheckSentUser);

    List<Friend> doubleCheckReceivedList = requestUser.getSentFriendRequests();
    Friend doubleCheckReceivedUser = isUserInFriendList(doubleCheckReceivedList, resUserID);

    if (doubleCheckReceivedUser != null)
      doubleCheckReceivedList.remove(doubleCheckReceivedUser);

    // Entity 저장
    userRepository.save(requestUser);
    userRepository.save(responseUser);

    return "성공적으로 친구를 추가했습니다.";
  }

  /**
   * socialID에 해당하는 Friend를 List에서 찾기
   * 
   * @param socialID
   */
  public Friend isUserInFriendList(List<Friend> friendList, String socialID) {

    for (Friend friend : friendList) {

      if (friend.getSocialID().equals(socialID))
        return friend;
    }
    return null;
  }

  /**
   * socialID에 해당하는 User를 친구 목록에서 삭제하기
   * JPA Repository의 findBy Method를 사용하여 특정 User에 접근하여 삭제
   * 
   * @param socialID
   */
  public String removeFriend(@NonNull String targetUserID, @NonNull String socialID) {

    // 친구 목록에서 삭제 작업
    User user = userRepository.findById(socialID).get();
    List<Friend> friends = user.getFriends();

    Friend target = isUserInFriendList(friends, targetUserID);

    if (target != null)
      friends.remove(target);
    else
      return "이미 친구가 아닌 유저입니다.";

    userRepository.save(user);

    // 상대방의 친구 목록에서도 삭제
    User targetUser = userRepository.findById(targetUserID).get();
    List<Friend> targetFriends = targetUser.getFriends();

    Friend targetFriend = isUserInFriendList(targetFriends, socialID);

    if (targetFriend != null)
      targetFriends.remove(targetFriend);
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
  public String updateProfile(String newNickname, MultipartFile profileImage, @NonNull String socialID) {

    User user = userRepository.findById(socialID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 유저 ID 입니다."));

    // 기존 nickname과 동일한지 확인 후 수정
    if (!user.getNickname().equals(newNickname))
      user.setNickname(newNickname);

    // 기존 profileImage와 동일한지 확인 후 수정
    String profileImageOriginalName = user.getProfileImageOriginalName();
    if (profileImage != null && !profileImageOriginalName.equals(profileImage.getOriginalFilename())) {

      // S3에 업로드 시도 후, 업로드 된 S3 파일명 리스트로 받아오기
      String uploadFileName = awsS3Service.uploadFile(profileImage, socialID);

      // 새로운 profileImage 정보로 User entity 업데이트
      user.setProfileImageUrl(uploadFileName);
      user.setProfileImageOriginalName(profileImage.getOriginalFilename());

      // 기존의 profileImage 삭제
      awsS3Service.deleteFile(profileImageOriginalName, socialID);
    }

    userRepository.save(user);

    return "프로필을 성공적으로 변경했습니다.";
  }

  /**
   * collaboUserID에 해당하는 유저 Entity의 receivedAlbumInvitations 필드에 album을 추가
   * 
   * @param collaboUserID
   * @param album
   */
  public void addReceivedAlbumInvitations(@NonNull String collaboUserID, Album album) {

    User user = userRepository.findById(collaboUserID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 대상입니다."));

    user.getReceivedAlbumInvitations().add(album);

    userRepository.save(user);
  }

  /**
   * resUserID에 해당하는 유저 Entity의 receivedAlbumInvitations 필드에서 albumID에 해당하는 album
   * 제거
   * 
   * @param collaboUserID
   * @param album
   */
  public void removeAlbumInvitation(@NonNull String resUserID, String albumID) {

    User user = userRepository.findById(resUserID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 대상입니다."));

    user.getReceivedAlbumInvitations().removeIf(album -> album.getAlbumUUID().equals(albumID));

    userRepository.save(user);
  }

  /**
   * collaboUserID에 해당하는 유저 Entity의 receivedAlbumInvitations 필드에 album을 추가
   * 
   * @param collaboUserID
   * @param album
   */
  public void addCollaboAlbum(@NonNull String collaboUserID, Album album) {

    User user = userRepository.findById(collaboUserID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 대상입니다."));

    user.getCollaboAlbums().add(album);

    userRepository.save(user);
  }
}
