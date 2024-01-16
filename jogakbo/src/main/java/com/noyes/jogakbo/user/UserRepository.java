package com.noyes.jogakbo.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findByRefreshToken(String refreshToken);

  Optional<List<User>> findAllByNicknameContainsAndSocialIDNot(String nickname, String socialID);

  void deleteBySocialID(String socialID);
}
