package com.noyes.jogakbo.album;

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
    List<String> uploadFileNames = awsS3Service.uploadFiles(multipartFiles);

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

  public void unloadImage(String albumID, int pageNum, String imageUUID) throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<ImagesInPage> targetList = targetInfo.getImagesInfo().get(pageNum);

    for (ImagesInPage tmp : targetList) {

      if (tmp.getImageUUID().equals(imageUUID)) {

        targetList.remove(tmp);
        break;
      }
    }
    redisService.setAlbumRedisValue(albumID, targetInfo);

    awsS3Service.deleteFile(imageUUID);
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
    String thumbnailOriginalName = album.getThumbnailOriginalName();
    if (thumnailImage != null && !thumnailImage.getOriginalFilename().equals(thumbnailOriginalName)) {

      // S3에 업로드 시도 후, 업로드 된 S3 파일명 받아오기
      String uploadFileName = awsS3Service.uploadFile(thumnailImage);

      album.setThumbnailImage(uploadFileName);
      album.setThumbnailOriginalName(thumbnailOriginalName);
    }

    albumRepository.save(album);

    return "프로필을 성공적으로 변경했습니다.";
  }
}