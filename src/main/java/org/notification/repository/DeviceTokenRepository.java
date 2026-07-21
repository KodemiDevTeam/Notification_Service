package org.notification.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.notification.model.DeviceToken;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DeviceTokenRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public DeviceTokenRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(DeviceToken token) {
        dynamoDBMapper.save(token);
    }

    public void delete(DeviceToken token) {
        dynamoDBMapper.delete(token);
    }

    public DeviceToken findByToken(String token) {
        return dynamoDBMapper.load(DeviceToken.class, token);
    }

    public List<DeviceToken> findByUserId(String userId) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":userId", new AttributeValue().withS(userId));

        DynamoDBQueryExpression<DeviceToken> query = new DynamoDBQueryExpression<DeviceToken>()
                .withIndexName(DeviceToken.USER_INDEX)
                .withConsistentRead(false)
                .withKeyConditionExpression("userId = :userId")
                .withExpressionAttributeValues(values);

        return dynamoDBMapper.query(DeviceToken.class, query);
    }
}
