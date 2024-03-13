package com.noyes.jogakbo.global;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test의 일관성을 위해, 매 Test마다 DB를 초기화 하는 클래스
 */
public class AcceptanceTestExecutionListener extends AbstractTestExecutionListener {

  @SuppressWarnings("null")
  @Override
  public void beforeTestClass(final TestContext testContext) {

    final MongoTemplate mongoTemplate = testContext.getApplicationContext().getBean(MongoTemplate.class);
    mongoTemplate.getDb().drop();
  }

  @SuppressWarnings("null")
  @Override
  public void afterTestMethod(final TestContext testContext) {

    final MongoTemplate mongoTemplate = testContext.getApplicationContext().getBean(MongoTemplate.class);
    mongoTemplate.getDb().drop();
  }
}