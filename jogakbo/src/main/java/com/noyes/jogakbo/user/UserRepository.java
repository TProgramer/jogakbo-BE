package com.noyes.jogakbo.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserDocument, Long> {

  Optional<UserDocument> findByRefreshToken(String refreshToken);

  Optional<UserDocument> findBySocialId(String socialId);

  void deleteBySocialId(String socialId);
}
