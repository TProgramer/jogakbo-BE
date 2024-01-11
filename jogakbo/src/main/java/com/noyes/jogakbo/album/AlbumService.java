package com.noyes.jogakbo.album;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noyes.jogakbo.album.DTO.EditMessage;
import com.noyes.jogakbo.album.DTO.ImageInfo;
import com.noyes.jogakbo.album.DTO.ImageSize;
import com.noyes.jogakbo.album.DTO.ImagesInPage;
import com.noyes.jogakbo.global.redis.RedisService;
import com.noyes.jogakbo.global.s3.AwsS3Service;
import com.noyes.jogakbo.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

  private final UserService userService;
  private final AwsS3Service awsS3Service;
  // 2D Array Mapping Issue로 인해 임시 폐기
  // private final AlbumImagesInfoRepository albumImagesInfoRepository;
  private final RedisService redisService;

  public Album findAlbumByUser(String socialID, String albumID) {

    return userService.getAlbum(socialID, albumID);
  }

  public List<Album> findAllAlbumByUser(String socialID) {

    return userService.getAlbums(socialID);
  }

  public String createAlbum(String albumName, String socialID) throws JsonProcessingException {

    String albumID = UUID.randomUUID().toString();
    List<List<ImagesInPage>> blankImagesProp = new ArrayList<>();
    blankImagesProp.add(new ArrayList<>());

    // ImagesInPage test = ImagesInPage.builder()
    // .imageUUID(albumID)
    // .rotation(4.0)
    // .build();
    // blankImagesProp.get(0).add(test);

    Album newAlbum = Album.builder()
        .albumID(albumID)
        .albumName(albumName)
        .images(blankImagesProp)
        .albumEditors(new ArrayList<>())
        .build();

    userService.addAlbum(newAlbum, socialID);

    // ObjectMapper objectMapper = new ObjectMapper();
    // String imagesInfo = "";
    // try {

    // imagesInfo = objectMapper.writeValueAsString(blankImagesProp);
    // } catch (IOException e) {

    // e.printStackTrace();
    // }
    // ValueOperations<String, Object> operations = redisTemplate.opsForValue();
    // operations.set(albumID, imagesInfo);

    AlbumImagesInfo albumImagesInfo = AlbumImagesInfo.builder()
        .id(albumID)
        .imagesInfo(blankImagesProp)
        .build();

    redisService.setAlbumRedisValue(albumID, albumImagesInfo);

    // albumImagesInfoRepository.save(albumImagesInfo);

    return albumID;
  }

  public List<List<ImagesInPage>> addNewPage(String albumID) throws JsonProcessingException {

    // Album targetAlbum = albumRepository.findById(albumID).get();
    // List<List<ImagesInPage>> imagesInfo = targetAlbum.getImages();

    // ValueOperations<String, Object> operations = redisTemplate.opsForValue();
    // String imagesInfoJson = (String) operations.get(albumID);
    // ObjectMapper objectMapper = new ObjectMapper();
    // List<List<ImagesInPage>> imagesInfo = objectMapper.readValue(imagesInfoJson,
    // new TypeReference<>() {
    // });
    // imagesInfo.add(new ArrayList<>());

    // AlbumImagesInfo targetInfo =
    // albumImagesInfoRepository.findById(albumID).get();

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();
    imagesInfo.add(new ArrayList<>());
    // albumImagesInfoRepository.save(targetInfo);
    redisService.setAlbumRedisValue(albumID, targetInfo);

    // targetAlbum.setImages(imagesInfo);
    // albumRepository.save(targetAlbum);

    return imagesInfo;
  }

  public List<List<ImagesInPage>> uploadImages(String albumID, List<MultipartFile> multipartFiles, String fileInfos)
      throws JsonProcessingException {

    // S3에 업로드 시도 후, 업로드 된 S3 파일명 리스트로 받아오기
    List<String> uploadFileNames = awsS3Service.uploadFile(multipartFiles);

    ObjectMapper objectMapper = new ObjectMapper();
    List<ImageInfo> imageInfos = objectMapper.readValue(fileInfos, new TypeReference<List<ImageInfo>>() {
    });

    // TO-DO: Repository로 부터 현재 앨범에 등록된 이미지 배열 받아오기

    // Album targetAlbum = albumRepository.findById(albumID).get();
    // List<List<ImagesInPage>> imagesInfo = targetAlbum.getImages();

    // AlbumImagesInfo targetInfo =
    // albumImagesInfoRepository.findById(albumID).get();
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
    // albumRepository.save(targetAlbum);
    // albumImagesInfoRepository.save(targetInfo);
    redisService.setAlbumRedisValue(albumID, targetInfo);

    return imagesInfo;
  }

  public void unloadImage(String albumID, int pageNum, String imageUUID) throws JsonProcessingException {

    // Album targetAlbum = albumRepository.findById(albumID).get();
    // List<ImagesInPage> targetList = targetAlbum.getImages().get(pageNum);

    // AlbumImagesInfo targetInfo =
    // albumImagesInfoRepository.findById(albumID).get();
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

    // TO-DO: Repository로 부터 현재 앨범에 등록된 이미지 배열 받아오기
    // Album targetAlbum = albumRepository.findById(albumID).get();
    // List<List<ImagesInPage>> imagesInfo = targetAlbum.getImages();

    // AlbumImagesInfo targetInfo =
    // albumImagesInfoRepository.findById(albumID).get();
    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();

    for (EditMessage target : payload) {

      int pageNum = target.getImageInfo().getPage();
      List<ImagesInPage> targetPageInfo = imagesInfo.get(pageNum);

      // To-Do: imageUUID가 같은지 확인하는 효율적인 로직 찾아보기
      // for (int targetIdx = 0; targetIdx < targetPageInfo.size(); targetIdx++) {

      // if
      // (targetPageInfo.get(targetIdx).getImageUUID().equals(target.getImageUUID()))
      // {

      // ImagesInPage tmp = targetPageInfo.get(targetIdx);
      // tmp.setLocation(target.getImageInfo().getLocation());
      // tmp.setSize(target.getImageInfo().getSize());
      // tmp.setRotation(target.getImageInfo().getRotation());

      // targetPageInfo.remove(targetIdx);
      // targetPageInfo.add(tmp);
      // log.info("변경사항 적용 완료");
      // break;
      // }
      // }
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
}