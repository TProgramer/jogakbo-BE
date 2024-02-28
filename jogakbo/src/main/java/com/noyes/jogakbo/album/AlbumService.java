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
import com.noyes.jogakbo.album.DTO.AlbumEntryMessage;
import com.noyes.jogakbo.album.DTO.AlbumImageEditInfo;
import com.noyes.jogakbo.album.DTO.AlbumImageInfo;
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

  public AlbumEntryMessage getEntryMessage(String socialID, String albumID) throws JsonProcessingException {

    // 앨범 ID로 앨범 가져오기
    Album album = albumRepository.findById(albumID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 앨범 ID 입니다."));

    // 앨범 주인이거나 공동 작업자인지 확인
    String albumOwner = album.getAlbumOwner();
    List<String> albumEditors = album.getAlbumEditors();
    if (!albumOwner.equals(socialID) && !albumEditors.contains(socialID))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "앨범을 조회할 권한이 없습니다.");

    // 앨범 주인과 공동 작업자들의 정보 추출
    UserInfo albumOwnerInfo = userService.getUserInfo(albumOwner);
    List<UserInfo> albumEditorsInfo = new ArrayList<>();

    for (String albumEditor : albumEditors) {

      albumEditorsInfo.add(userService.getUserInfo(albumEditor));
    }

    // 앨범에 초대된 유저 정보 추출
    List<String> sentAlbumInvitations = album.getSentAlbumInvitations();
    List<UserInfo> sentAlbumInvitationsInfo = new ArrayList<>();
    for (String sentAlbumInvitation : sentAlbumInvitations) {

      sentAlbumInvitationsInfo.add(userService.getUserInfo(sentAlbumInvitation));
    }

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();

    return AlbumEntryMessage.builder()
        .albumName(album.getAlbumName())
        .imagesInfo(imagesInfo)
        .thumbnailImage(album.getThumbnailImage())
        .createdDate(album.getCreatedDate())
        .albumOwnerInfo(albumOwnerInfo)
        .albumEditorsInfo(albumEditorsInfo)
        .sentAlbumInvitationsInfo(sentAlbumInvitationsInfo)
        .build();
  }

  public String createAlbum(String albumName, String socialID) throws JsonProcessingException {

    String albumID = UUID.randomUUID().toString();
    List<List<AlbumImageInfo>> blankImagesProp = new ArrayList<>();
    blankImagesProp.add(new ArrayList<>());

    Album newAlbum = Album.builder()
        .albumID(albumID)
        .albumName(albumName)
        .albumImages(blankImagesProp)
        .albumOwner(socialID)
        .albumEditors(new ArrayList<>())
        .build();

    albumRepository.save(newAlbum);

    userService.addAlbum(newAlbum, socialID);

    AlbumImagesInfo albumImagesInfo = AlbumImagesInfo.builder()
        .id(albumID)
        .imagesInfo(blankImagesProp)
        .build();

    redisService.setAlbumRedisValue(albumID, albumImagesInfo);

    return albumID;
  }

  public List<List<AlbumImageInfo>> addNewPage(String albumID) throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();
    imagesInfo.add(new ArrayList<>());
    redisService.setAlbumRedisValue(albumID, targetInfo);

    return imagesInfo;
  }

  public List<List<AlbumImageInfo>> uploadImages(String albumID, List<MultipartFile> multipartFiles, String fileInfos)
      throws JsonProcessingException {

    // S3에 업로드 시도 후, 업로드 된 S3 파일명 리스트로 받아오기
    List<String> uploadFileNames = awsS3Service.uploadFiles(multipartFiles, albumID);

    ObjectMapper objectMapper = new ObjectMapper();
    List<AlbumImageEditInfo> imageInfos = objectMapper.readValue(fileInfos,
        new TypeReference<List<AlbumImageEditInfo>>() {
        });

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
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
    redisService.setAlbumRedisValue(albumID, targetInfo);

    return imagesInfo;
  }

  public List<List<AlbumImageInfo>> unloadImage(String albumID, int pageNum, String imageUUID)
      throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<AlbumImageInfo>> imagesInfo = targetInfo.getImagesInfo();
    List<AlbumImageInfo> targetList = imagesInfo.get(pageNum);

    for (AlbumImageInfo tmp : targetList) {

      if (tmp.getAlbumImageUUID().equals(imageUUID)) {

        targetList.remove(tmp);
        break;
      }
    }
    redisService.setAlbumRedisValue(albumID, targetInfo);

    awsS3Service.deleteFile(imageUUID, albumID);

    return imagesInfo;
  }

  public List<List<AlbumImageInfo>> editImage(String albumID, List<AlbumImageEditMessage> payload)
      throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
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
    redisService.setAlbumRedisValue(albumID, targetInfo);

    return imagesInfo;
  }

  /**
   * albumID에 해당하는 앨범의 albumName과 thumbnailImage 수정하기
   * 요청자의 socialID가 albumOwner 인지 검증해야함
   * 
   * @param
   * @return 실행 결과
   */
  public String updateProfile(String albumID, String newAlbumName, MultipartFile thumnailImage,
      @NonNull String socialID) {

    Album album = albumRepository.findById(albumID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 앨범 ID 입니다."));

    // 요청자가 albumOwner 인지 검증
    if (!socialID.equals(album.getAlbumOwner()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "앨범 소유자만 앨범 프로필을 수정할 수 있습니다.");

    // 기존 albumName과 동일한지 확인 후 수정
    if (!newAlbumName.equals(album.getAlbumName()))
      album.setAlbumName(newAlbumName);

    // 기존 thumbnailImage와 동일한지 확인 후 수정
    String oldThumbnailOriginalName = album.getThumbnailOriginalName();

    if (thumnailImage != null && !thumnailImage.getOriginalFilename().equals(oldThumbnailOriginalName)) {

      // S3에 업로드 시도 후, 업로드 된 S3 파일명 받아오기
      String uploadFileName = awsS3Service.uploadFile(thumnailImage, albumID);

      album.setThumbnailImage(uploadFileName);
      album.setThumbnailOriginalName(thumnailImage.getOriginalFilename());

      // 기존 thumbnailImage 삭제
      awsS3Service.deleteFile(oldThumbnailOriginalName, albumID);
    }

    albumRepository.save(album);

    return "프로필을 성공적으로 변경했습니다.";
  }

  public String removeAlbum(String albumID, String socialID) throws IOException {

    // albumID로 앨범 조회
    Album album = albumRepository.findById(albumID).get();

    // 앨범 소유자의 요청인지 검증
    if (!album.getAlbumOwner().equals(socialID))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "앨범 소유자만 앨범을 삭제할 수 있습니다.");

    // 앨범에 조회를 막아 추가 입장을 막기 위해 mongoDB에서 Album Entity 삭제
    albumRepository.deleteById(albumID);

    // 모두를 앨범에서 강제 추방함을 알리고 소켓 연결 종료
    WebSocketSessionHolder.closeSessionByDestination(albumID);

    // 최신 데이터를 참조하기 위해 redis를 기준으로 이미지 정보 불러오기
    List<List<AlbumImageInfo>> imagesInfo = redisService
        .getAlbumRedisValue(albumID, AlbumImagesInfo.class)
        .getImagesInfo();

    // 앨범에 업로드된 이미지들을 순회하며 S3 이미지 삭제
    // To-do: S3 SDK의 deleteObjects 메소드로 한번에 삭제 방식으로 전환 및 예외처리 추가
    for (List<AlbumImageInfo> imagesInfoOfIndex : imagesInfo) {
      for (AlbumImageInfo imageInfo : imagesInfoOfIndex) {

        awsS3Service.deleteFile(imageInfo.getAlbumImageUUID(), albumID);
      }
    }

    // redis에서 AlbumImagesInfo 삭제
    redisService.removeAlbumRedisValue(albumID);

    return "앨범 삭제 작업을 완료했습니다.";
  }

  /**
   * 유저가 albumEditors 에 속해있는지 검증
   * 
   * @param albumID
   * @param socialID
   * @return
   */
  public Boolean validAlbumEditor(String albumID, String socialID) {

    // DB에서 albumID로 Album 객체 접근 후, albumOwner와 albumEditors 필드 추출
    Album album = albumRepository.findById(albumID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 앨범입니다."));

    String albumOwnerID = album.getAlbumOwner();
    List<String> albumEditors = album.getAlbumEditors();

    // albumOwner 인 경우, true 반환
    if (albumOwnerID.equals(socialID))
      return true;

    // 순회를 돌며 인자로 받은 socialID가 List에 포함되어 있다면 true를 반환
    for (String albumEditor : albumEditors) {

      if (albumEditor.equals(socialID))
        return true;
    }

    // List에 포함되어 있지 않았으므로 false를 반환
    return false;
  }

  /**
   * collaboUserID에 해당하는 유저에게 앨범 초대 보내기
   * 
   * @param
   * @return
   */
  public Album sendAlbumInvitation(String albumID, String collaboUserID, String socialID) {

    // 요청자가 album의 소유자인지 검증
    Album album = albumRepository.findById(albumID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 앨범 ID 입니다."));

    // 이미 요청을 보낸 대상일 경우 예외처리
    List<String> sentAlbumInvitations = album.getSentAlbumInvitations();

    if (isUserInList(sentAlbumInvitations, collaboUserID) != null)
      return null;

    // 이미 초대된 경우도 예외처리
    List<String> albumEditors = album.getAlbumEditors();
    if (isUserInList(albumEditors, collaboUserID) != null)
      return null;

    // 앨범 초대 리스트에 등록
    sentAlbumInvitations.add(collaboUserID);
    userService.addReceivedAlbumInvitations(collaboUserID, album);

    albumRepository.save(album);

    return album;
  }

  /**
   * 응답 메세지에 따라 앨범 작업자로 추가 후, 초대 요청 리스트에서 이전 요청 제거
   * 
   * @param albumID
   * @param resUserID
   * @param reply
   * @return result in String
   */
  public String replyAlbumInvitation(@NonNull String albumID, @NonNull String resUserID, String reply) {

    // 보낸|받은 요청 유효성 판별 변수 추가
    boolean isValidRequest = true;

    // 초대 요청을 보낸 앨범인지 확인
    Album requestAlbum = albumRepository.findById(albumID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 앨범 ID 입니다."));

    List<String> sentAlbumInvitations = requestAlbum.getSentAlbumInvitations();

    String targetUser = isUserInList(sentAlbumInvitations, resUserID);

    if (targetUser == null)
      isValidRequest = false;

    // 초대 요청을 받은 유저인지도 확인
    User responseUser = userService.getUser(resUserID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저 ID 입니다."));

    List<Album> receivedAlbumInvitations = responseUser.getReceivedAlbumInvitations();

    Album callAlbum = isAlbumInList(receivedAlbumInvitations, albumID);

    if (callAlbum == null)
      isValidRequest = false;

    // 서로의 요청, 승인 대기열에서 삭제
    sentAlbumInvitations.remove(targetUser);
    userService.removeAlbumInvitation(resUserID, callAlbum.getAlbumID());

    albumRepository.save(requestAlbum);

    if (!isValidRequest)
      return "더 이상 유효하지 않은 앨범 초대 요청입니다.";

    // 서로의 목록에 추가
    if (reply.equals("accept")) {

      requestAlbum.getAlbumEditors().add(targetUser);
      albumRepository.save(requestAlbum);

      userService.addCollaboAlbum(resUserID, callAlbum);
    }

    return "요청이 성공적으로 반영되었습니다.";
  }

  /**
   * List에 collaboUserID가 존재하는지 판별
   * 
   * @param
   */
  public String isUserInList(List<String> stringList, String socialID) {

    for (String stringPiece : stringList) {

      if (stringPiece.equals(socialID))
        return stringPiece;
    }
    return null;
  }

  /**
   * List에 동일한 albumID를 가지는 album이 존재하는지 판별
   * 
   * @param
   * @return
   */
  public Album isAlbumInList(List<Album> albumList, String albumID) {

    for (Album albumPiece : albumList) {

      if (albumPiece.getAlbumID().equals(albumID))
        return albumPiece;
    }
    return null;
  }
}