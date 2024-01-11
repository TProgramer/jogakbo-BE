package com.noyes.jogakbo.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findByRefreshToken(String refreshToken);

  void deleteBySocialID(String socialID);
}
