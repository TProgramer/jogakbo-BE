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
import com.noyes.jogakbo.album.DTO.EditMessage;
import com.noyes.jogakbo.album.DTO.EntryMessage;
import com.noyes.jogakbo.album.DTO.ImageInfo;
import com.noyes.jogakbo.album.DTO.ImagesInPage;
import com.noyes.jogakbo.global.redis.RedisService;
import com.noyes.jogakbo.global.s3.AwsS3Service;
import com.noyes.jogakbo.global.websocket.WebSocketSessionHolder;
import com.noyes.jogakbo.user.UserService;

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

  public EntryMessage getEntryMessage(String socialID, String albumID) throws JsonProcessingException {

    Album album = userService.getAlbumByUser(socialID, albumID);
    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();

    return EntryMessage.builder()
        .albumName(album.getAlbumName())
        .imagesInfo(imagesInfo)
        .thumbnailImage(album.getThumbnailImage())
        .createdDate(album.getCreatedDate())
        .build();
  }

  public List<Album> getAllAlbumByUser(String socialID) {

    return userService.getAlbumsByUser(socialID);
  }

  public String createAlbum(String albumName, String socialID) throws JsonProcessingException {

    String albumID = UUID.randomUUID().toString();
    List<List<ImagesInPage>> blankImagesProp = new ArrayList<>();
    blankImagesProp.add(new ArrayList<>());

    Album newAlbum = Album.builder()
        .albumID(albumID)
        .albumName(albumName)
        .images(blankImagesProp)
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

  public List<List<ImagesInPage>> addNewPage(String albumID) throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();
    imagesInfo.add(new ArrayList<>());
    redisService.setAlbumRedisValue(albumID, targetInfo);

    return imagesInfo;
  }

  public List<List<ImagesInPage>> uploadImages(String albumID, List<MultipartFile> multipartFiles, String fileInfos)
      throws JsonProcessingException {

    // S3에 업로드 시도 후, 업로드 된 S3 파일명 리스트로 받아오기
    List<String> uploadFileNames = awsS3Service.uploadFiles(multipartFiles, albumID);

    ObjectMapper objectMapper = new ObjectMapper();
    List<ImageInfo> imageInfos = objectMapper.readValue(fileInfos, new TypeReference<List<ImageInfo>>() {
    });

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();

    for (int i = 0; i < uploadFileNames.size(); i++) {

      int pageNum = imageInfos.get(i).getPage();
      List<ImagesInPage> targetPageInfo = imagesInfo.get(pageNum);
      ImagesInPage tmp = ImagesInPage.builder()
          .imageUUID(uploadFileNames.get(i))
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

  public List<List<ImagesInPage>> unloadImage(String albumID, int pageNum, String imageUUID)
      throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();
    List<ImagesInPage> targetList = imagesInfo.get(pageNum);

    for (ImagesInPage tmp : targetList) {

      if (tmp.getImageUUID().equals(imageUUID)) {

        targetList.remove(tmp);
        break;
      }
    }
    redisService.setAlbumRedisValue(albumID, targetInfo);

    awsS3Service.deleteFile(imageUUID, albumID);

    return imagesInfo;
  }

  public List<List<ImagesInPage>> editImage(String albumID, List<EditMessage> payload) throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();

    for (EditMessage target : payload) {

      int pageNum = target.getImageInfo().getPage();
      List<ImagesInPage> targetPageInfo = imagesInfo.get(pageNum);

      // To-Do: imageUUID가 같은지 확인하는 효율적인 로직 찾아보기
      for (ImagesInPage tmp : targetPageInfo) {

        if (tmp.getImageUUID().equals(target.getImageUUID())) {

          tmp.setLocation(target.getImageInfo().getLocation());
          tmp.setSize(target.getImageInfo().getSize());
          tmp.setRotation(target.getImageInfo().getRotation());

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
    List<List<ImagesInPage>> imagesInfo = redisService
        .getAlbumRedisValue(albumID, AlbumImagesInfo.class)
        .getImagesInfo();

    // 앨범에 업로드된 이미지들을 순회하며 S3 이미지 삭제
    // To-do: S3 SDK의 deleteObjects 메소드로 한번에 삭제 방식으로 전환 및 예외처리 추가
    for (List<ImagesInPage> imagesInfoOfIndex : imagesInfo) {
      for (ImagesInPage imageInfo : imagesInfoOfIndex) {

        awsS3Service.deleteFile(imageInfo.getImageUUID(), albumID);
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
}