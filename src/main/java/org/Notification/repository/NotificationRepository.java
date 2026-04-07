package org.Notification.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.Notification.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class NotificationRepository {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    // ✅ SAVE
    public Notification save(Notification notification) {
        dynamoDBMapper.save(notification);
        return notification;
    }

    // ✅ GET BY ID
    public Notification getById(String id) {
        return dynamoDBMapper.load(Notification.class, id);
    }

    // ✅ FIND BY ID (FIXED)
    public Optional<Notification> findById(String id) {
        Notification notification = dynamoDBMapper.load(Notification.class, id);
        return Optional.ofNullable(notification);
    }

    // ✅ FIND ALL
    public List<Notification> findAll() {
        return dynamoDBMapper.scan(Notification.class, new DynamoDBScanExpression());
    }

    // ✅ FIND BY USER ID (GSI)
    public List<Notification> findByUserId(String userId) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":userId", new AttributeValue().withS(userId));

        DynamoDBQueryExpression<Notification> query =
                new DynamoDBQueryExpression<Notification>()
                        .withIndexName("userId-index")
                        .withConsistentRead(false)
                        .withKeyConditionExpression("userId = :userId")
                        .withExpressionAttributeValues(values);

        return dynamoDBMapper.query(Notification.class, query);
    }

    // ✅ FIND BY STATUS (for scheduler)
    public List<Notification> findByStatus(String status) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":status", new AttributeValue().withS(status));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("status = :status")
                .withExpressionAttributeValues(values);

        return dynamoDBMapper.scan(Notification.class, scanExpression);
    }

    // ✅ DELETE
    public void delete(Notification notification) {
        dynamoDBMapper.delete(notification);
    }
}