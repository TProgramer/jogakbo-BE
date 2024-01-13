package com.noyes.jogakbo.album;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
public interface AlbumRepository extends MongoRepository<Album, String> {
}
