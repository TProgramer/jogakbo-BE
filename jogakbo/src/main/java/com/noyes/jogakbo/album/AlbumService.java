package com.noyes.jogakbo.album;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noyes.jogakbo.album.DTO.AlbumImageEditMessage;
import com.noyes.jogakbo.album.DTO.AlbumDetailInfo;
import com.noyes.jogakbo.album.DTO.AlbumEntryInfo;
import com.noyes.jogakbo.album.DTO.AlbumInitInfo;
import com.noyes.jogakbo.album.DTO.AlbumInvitationMessage;
import com.noyes.jogakbo.album.DTO.AlbumImageEditInfo;
import com.noyes.jogakbo.album.DTO.AlbumImageInfo;
import com.noyes.jogakbo.album.DTO.AlbumInfo;
import com.noyes.jogakbo.album.DTO.AlbumMemberInfo;
import com.noyes.jogakbo.global.redis.AlbumImagesInfo;
import com.noyes.jogakbo.global.redis.RedisService;
import com.noyes.jogakbo.global.s3.AwsS3Service;
import com.noyes.jogakbo.global.websocket.WebSocketSessionHolder;
import com.noyes.jogakbo.user.User;
import com.noyes.jogakbo.user.UserService;
import com.noyes.jogakbo.user.DTO.UserInfo;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

  private final AlbumRepository albumRepository;
  private final UserService userService;
  private final AwsS3Service awsS3Service;
  private final RedisService redisService;

  @SuppressWarnings("null")
  public Album getAlbum(String albumUUID) {

    return albumRepository.findById(albumUUID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 앨범 ID 입니다."));
  }

  public AlbumInitInfo getEntryMessage(String userUUID, String albumUUID) throws JsonProcessingException {

    // 앨범 ID로 앨범 가져오기
    Album album = getAlbum(albumUUID);

    // 앨범 주인이거나 공동 작업자인지 확인
    String albumOwner = album.getAlbumOwner();
    List<String> albumEditors = album.getAlbumEditors();
    if (!albumOwner.equals(userUUID) && !albumEditors.contains(userUUID))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "앨범을 조회할 권한이 없습니다.");

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumUUID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();

    return AlbumInitInfo.builder()
        .albumName(album.getAlbumName())
        .imagesInfo(imagesInfo)
        .build();
  }

  @SuppressWarnings("null")
  public String createAlbum(String albumName, String userUUID) throws JsonProcessingException {

    String albumUUID = UUID.randomUUID().toString();

    Album newAlbum = Album.builder()
        .albumUUID(albumUUID)
        .albumName(albumName)
        .albumOwner(userUUID)
        .build();

    albumRepository.save(newAlbum);

    userService.addAlbum(albumUUID, userUUID);

    AlbumImagesInfo albumImagesInfo = AlbumImagesInfo.builder()
        .id(albumUUID)
        .build();

    redisService.setAlbumRedisValue(albumUUID, albumImagesInfo);

    return albumUUID;
  }

  public List<List<AlbumImageInfo>> addNewPage(String albumUUID) throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumUUID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();
    imagesInfo.add(new ArrayList<>());
    redisService.setAlbumRedisValue(albumUUID, targetInfo);

    return imagesInfo;
  }

  public List<List<AlbumImageInfo>> uploadImages(String albumUUID, List<MultipartFile> multipartFiles, String fileInfos)
      throws JsonProcessingException {

    // S3에 업로드 시도 후, 업로드 된 S3 파일명 리스트로 받아오기
    List<String> uploadFileNames = awsS3Service.uploadFiles(multipartFiles, albumUUID);

    ObjectMapper objectMapper = new ObjectMapper();
    List<AlbumImageEditInfo> imageInfos = objectMapper.readValue(fileInfos,
        new TypeReference<List<AlbumImageEditInfo>>() {
        });

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumUUID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();

    for (int i = 0; i < uploadFileNames.size(); i++) {

      int pageNum = imageInfos.get(i).getPageNum();
      List<AlbumImageInfo> targetPageInfo = imagesInfo.get(pageNum);
      AlbumImageInfo tmp = AlbumImageInfo.builder()
          .albumImageUUID(uploadFileNames.get(i))
          .size(imageInfos.get(i).getSize())
          .location(imageInfos.get(i).getLocation())
          .rotation(imageInfos.get(i).getRotation())
          .build();

      targetPageInfo.add(tmp);
      imagesInfo.set(pageNum, targetPageInfo);
    }

    log.info(imagesInfo.toString());
    redisService.setAlbumRedisValue(albumUUID, targetInfo);

    return imagesInfo;
  }

  public List<List<AlbumImageInfo>> unloadImage(String albumUUID, String imageUUID)
      throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumUUID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();

    for (List<AlbumImageInfo> imagesInfoByPage : imagesInfo) {
      for (AlbumImageInfo imageInfo : imagesInfoByPage) {

        if (imageInfo.getAlbumImageUUID().equals(imageUUID)) {

          imagesInfoByPage.remove(imageInfo);
          break;
        }
      }
    }
    redisService.setAlbumRedisValue(albumUUID, targetInfo);

    awsS3Service.deleteFile(imageUUID, albumUUID);

    return imagesInfo;
  }

  public List<List<AlbumImageInfo>> editImage(String albumUUID, List<AlbumImageEditMessage> payload)
      throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumUUID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();

    for (AlbumImageEditMessage target : payload) {

      int pageNum = target.getAlbumImageEditInfo().getPageNum();
      List<AlbumImageInfo> targetPageInfo = imagesInfo.get(pageNum);

      // To-Do: imageUUID가 같은지 확인하는 효율적인 로직 찾아보기
      for (AlbumImageInfo tmp : targetPageInfo) {

        if (tmp.getAlbumImageUUID().equals(target.getAlbumImageUUID())) {

          tmp.setLocation(target.getAlbumImageEditInfo().getLocation());
          tmp.setSize(target.getAlbumImageEditInfo().getSize());
          tmp.setRotation(target.getAlbumImageEditInfo().getRotation());

          targetPageInfo.add(tmp);
          targetPageInfo.remove(tmp);
          log.info("변경사항 적용 완료");
          break;
        }
      }
    }

    // albumImagesInfoRepository.save(targetInfo);
    redisService.setAlbumRedisValue(albumUUID, targetInfo);

    return imagesInfo;
  }

  /**
   * albumID에 해당하는 앨범의 albumName과 thumbnailImage 수정하기
   * 요청자의 socialID가 albumOwner 인지 검증해야함
   * 
   * @param
   * @return 실행 결과
   */
  @SuppressWarnings("null")
  public String updateProfile(String albumUUID, String newAlbumName, MultipartFile thumnailImage,
      @NonNull String userUUID) {

    Album album = getAlbum(albumUUID);

    // 요청자가 albumOwner 인지 검증
    if (!userUUID.equals(album.getAlbumOwner()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "앨범 소유자만 앨범 프로필을 수정할 수 있습니다.");

    // 기존 albumName과 동일한지 확인 후 수정
    if (!newAlbumName.equals(album.getAlbumName()))
      album.setAlbumName(newAlbumName);

    // 기존 thumbnailImage와 동일한지 확인 후 수정
    String oldThumbnailOriginalName = album.getThumbnailOriginalName();

    if (thumnailImage != null && !thumnailImage.getOriginalFilename().equals(oldThumbnailOriginalName)) {

      // S3에 업로드 시도 후, 업로드 된 S3 파일명 받아오기
      String uploadFileName = awsS3Service.uploadFile(thumnailImage, albumUUID);

      album.setThumbnailImageURL(uploadFileName);
      album.setThumbnailOriginalName(thumnailImage.getOriginalFilename());

      // 기존 thumbnailImage 삭제
      awsS3Service.deleteFile(oldThumbnailOriginalName, albumUUID);
    }

    albumRepository.save(album);

    return "프로필을 성공적으로 변경했습니다.";
  }

  @SuppressWarnings("null")
  public String removeAlbum(String albumUUID, String userUUID) throws IOException {

    // albumID로 앨범 조회
    Album album = getAlbum(albumUUID);

    // 앨범 소유자의 요청인지 검증
    if (!album.getAlbumOwner().equals(userUUID))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "앨범 소유자만 앨범을 삭제할 수 있습니다.");

    // 앨범에 조회를 막아 추가 입장을 막기 위해, User Owner의 albums 필드와 Collabo User의 collboAlbums
    // 필드에서 해당 albumUUID 삭제
    userService.removeAlbum(albumUUID, userUUID);
    List<String> collaboEditorsUUIDs = album.getAlbumEditors();
    for (String collaboEditorUUID : collaboEditorsUUIDs) {

      userService.removeCollaboAlbum(albumUUID, collaboEditorUUID);
    }

    // mongoDB에서 Album Entity 삭제
    albumRepository.deleteById(albumUUID);

    // 모두를 앨범에서 강제 추방함을 알리고 소켓 연결 종료
    WebSocketSessionHolder.closeSessionByDestination(albumUUID);

    // 최신 데이터를 참조하기 위해 redis를 기준으로 이미지 정보 불러오기
    List<List<AlbumImageInfo>> imagesInfo = redisService
        .getAlbumRedisValue(albumUUID, AlbumImagesInfo.class)
        .getImagesInfo();

    // 앨범에 업로드된 이미지들을 순회하며 S3 이미지 삭제
    // To-do: S3 SDK의 deleteObjects 메소드로 한번에 삭제 방식으로 전환 및 예외처리 추가
    for (List<AlbumImageInfo> imagesInfoOfIndex : imagesInfo) {
      for (AlbumImageInfo imageInfo : imagesInfoOfIndex) {

        awsS3Service.deleteFile(imageInfo.getAlbumImageUUID(), albumUUID);
      }
    }

    // redis에서 AlbumImagesInfo 삭제
    redisService.removeAlbumRedisValue(albumUUID);

    return "앨범 삭제 작업을 완료했습니다.";
  }

  /**
   * 유저가 albumEditors 에 속해있는지 검증
   * 
   * @param albumUUID
   * @param userUUID
   * @return
   */
  public Boolean isValidAlbumEditor(String albumUUID, String userUUID) {

    // DB에서 albumID로 Album 객체 접근 후, albumOwner와 albumEditors 필드 추출
    Album album = getAlbum(albumUUID);

    String albumOwnerID = album.getAlbumOwner();
    List<String> albumEditors = album.getAlbumEditors();

    // albumOwner 인 경우, true 반환
    if (albumOwnerID.equals(userUUID))
      return true;

    // 순회를 돌며 인자로 받은 socialID가 List에 포함되어 있다면 true를 반환
    for (String albumEditor : albumEditors) {

      if (albumEditor.equals(userUUID))
        return true;
    }

    // List에 포함되어 있지 않았으므로 false를 반환
    return false;
  }

  /**
   * collaboUserUUID에 해당하는 유저에게 앨범 초대 보내기
   * 
   * @param
   * @return
   */
  public AlbumInvitationMessage sendAlbumInvitation(String albumUUID, String collaboUserUUID, String userUUID) {

    // 요청자가 album의 소유자인지 검증
    Album album = getAlbum(albumUUID);
    if (!userUUID.equals(album.getAlbumOwner()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");

    // 이미 요청을 보낸 대상일 경우 예외처리
    List<String> albumInvitees = album.getAlbumInvitees();

    if (isUserInList(albumInvitees, collaboUserUUID) != null)
      return null;

    // 이미 초대된 경우도 예외처리
    List<String> albumEditors = album.getAlbumEditors();
    if (isUserInList(albumEditors, collaboUserUUID) != null)
      return null;

    // 앨범 초대 리스트에 등록
    albumInvitees.add(collaboUserUUID);
    userService.addAlbumInviters(collaboUserUUID, albumUUID);

    albumRepository.save(album);

    return AlbumInvitationMessage.builder()
        .albumUUId(album.getAlbumUUID())
        .albumName(album.getAlbumName())
        .albumOwner(album.getAlbumOwner())
        .build();
  }

  /**
   * 응답 메세지에 따라 앨범 작업자로 추가 후, 초대 요청 리스트에서 이전 요청 제거
   * 
   * @param albumUUID
   * @param resUserID
   * @param reply
   * @return result in String
   */
  public String replyAlbumInvitation(@NonNull String albumUUID, @NonNull String resUserID, String reply) {

    // 보낸|받은 요청 유효성 판별 변수 추가
    boolean isValidRequest = true;

    // 초대 요청을 보낸 앨범인지 확인
    Album requestAlbum = getAlbum(albumUUID);

    List<String> albumInvitees = requestAlbum.getAlbumInvitees();

    String targetUser = isUserInList(albumInvitees, resUserID);

    if (targetUser == null)
      isValidRequest = false;

    // 초대 요청을 받은 유저인지도 확인
    User responseUser = userService.getUser(resUserID);

    List<String> albumInviters = responseUser.getAlbumInviters();

    if (!isAlbumInList(albumInviters, albumUUID))
      isValidRequest = false;

    // 서로의 요청, 승인 대기열에서 삭제
    albumInvitees.remove(targetUser);
    userService.removeAlbumInvitation(resUserID, albumUUID);

    albumRepository.save(requestAlbum);

    if (!isValidRequest)
      return "더 이상 유효하지 않은 앨범 초대 요청입니다.";

    // 서로의 목록에 추가
    if (reply.equals("accept")) {

      requestAlbum.getAlbumEditors().add(targetUser);
      albumRepository.save(requestAlbum);

      userService.addCollaboAlbum(resUserID, albumUUID);
    }

    return "요청이 성공적으로 반영되었습니다.";
  }

  /**
   * List에 collaboUserUUID가 존재하는지 판별
   * 
   * @param
   */
  public String isUserInList(List<String> stringList, String userUUID) {

    for (String stringPiece : stringList) {

      if (stringPiece.equals(userUUID))
        return stringPiece;
    }
    return null;
  }

  /**
   * List에 동일한 albumID를 가지는 album이 존재하는지 TF 판별
   * 
   * @param
   * @return
   */
  public boolean isAlbumInList(List<String> albumList, String albumUUID) {

    for (String albumPiece : albumList) {

      if (albumPiece.equals(albumUUID))
        return true;
    }
    return false;
  }

  /**
   * 앨범에 참여 중인 앨범 주인, 공동 작업자, 초대 받은 유저들의 UserInfo를 모아 AlbumMemberInfo를 반환
   * 
   * @param albumUUID
   * @param userUUID
   * @return
   */
  public AlbumMemberInfo getAlbumMemberInfo(String albumUUID, String userUUID) {

    // 유저가 album editor 인지 검증
    if (!isValidAlbumEditor(albumUUID, userUUID))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");

    // 앨범 주인 정보 추출
    Album album = getAlbum(albumUUID);

    String albumOwnerUUID = album.getAlbumOwner();
    UserInfo albumOwnerInfo = userService.getUserInfo(albumOwnerUUID);

    // 앨범 공동 작업자 정보 추출
    List<String> albumEditorsUUIDs = album.getAlbumEditors();
    List<UserInfo> albumEditorsInfos = new ArrayList<>();

    for (String albumEditorUUID : albumEditorsUUIDs) {

      UserInfo albumEditorInfo = userService.getUserInfo(albumEditorUUID);
      albumEditorsInfos.add(albumEditorInfo);
    }

    // 앨범 초대 대상자 정보 추출
    List<String> albumInviteesUUIDs = album.getAlbumInvitees();
    List<UserInfo> albumInviteesInfos = new ArrayList<>();

    for (String albumInviteeUUID : albumInviteesUUIDs) {

      UserInfo albumInviteeInfo = userService.getUserInfo(albumInviteeUUID);
      albumInviteesInfos.add(albumInviteeInfo);
    }

    return AlbumMemberInfo.builder()
        .albumOwnerInfo(albumOwnerInfo)
        .albumEditorsInfos(albumEditorsInfos)
        .albumInviteesInfos(albumInviteesInfos)
        .build();
  }

  /**
   * albumUUID를 가진 앨범의 상세정보를 AlbumDetailInfo로 반환
   * 
   * @param albumUUID
   * @param userUUID
   * @return
   */
  public AlbumDetailInfo getAlbumDetailInfo(String albumUUID, String userUUID) {

    // 유저가 album editor 인지 검증
    if (!isValidAlbumEditor(albumUUID, userUUID))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");

    // 앨범 정보 추출
    Album album = getAlbum(albumUUID);

    return AlbumDetailInfo.builder()
        .albumName(album.getAlbumName())
        .thumbnailImageURL(album.getThumbnailImageURL())
        .createdDate(album.getCreatedDate())
        .isPublic(false)
        .build();
  }

  /**
   * 이미 인증된 유저가 요청한 albumUUIDs에 해당하는 List<AlbumInfo> 반환
   * 
   * @param albumUUID
   * @return
   */
  public List<AlbumInfo> getAlbumInfo(List<String> albumUUIDs) {

    // Authenication 객체의 userUUID로 이미 인증된 유저이므로 검증 생략

    List<AlbumInfo> albumInfos = new ArrayList<>();

    for (String albumUUID : albumUUIDs) {

      Album album = getAlbum(albumUUID);

      AlbumInfo albumInfo = AlbumInfo.builder()
          .albumUUID(album.getAlbumUUID())
          .albumName(album.getAlbumName())
          .thumbnailImageURL(album.getThumbnailImageURL())
          .createdDate(album.getCreatedDate())
          .lastModifiedDate(album.getLastModifiedDate())
          .build();

      albumInfos.add(albumInfo);
    }

    return albumInfos;
  }

  public AlbumEntryInfo getAlbumEntryInfo(String userUUID, String albumUUID) throws JsonProcessingException {

    // 유저가 album editor 인지 검증
    if (!isValidAlbumEditor(albumUUID, userUUID))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");

    // 앨범 정보 추출
    Album album = getAlbum(albumUUID);
    int memberCount = album.getAlbumEditors().size() + 1;
    int imageCount = 0;

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumUUID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();
    for (List<AlbumImageInfo> imagesInfoByPage : imagesInfo) {

      imageCount += imagesInfoByPage.size();
    }

    return AlbumEntryInfo.builder()
        .albumUUID(album.getAlbumUUID())
        .albumName(album.getAlbumName())
        .thumbnailImageURL(album.getThumbnailImageURL())
        .createdDate(album.getCreatedDate())
        .memberCount(memberCount)
        .imageCount(imageCount)
        .build();
  }
}