package org.notification.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationStatus;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NotificationRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private final com.amazonaws.services.dynamodbv2.AmazonDynamoDB amazonDynamoDB;

    public NotificationRepository(DynamoDBMapper dynamoDBMapper, com.amazonaws.services.dynamodbv2.AmazonDynamoDB amazonDynamoDB) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.amazonDynamoDB = amazonDynamoDB;
    }

    public Notification save(Notification notification) {
        dynamoDBMapper.save(notification);
        return notification;
    }

    public boolean saveIdempotent(Notification notification) {
        try {
            com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression saveExpression = new com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression();
            Map<String, com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue> expected = new HashMap<>();
            expected.put("notificationId", new com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue().withExists(false));
            saveExpression.setExpected(expected);
            
            dynamoDBMapper.save(notification, saveExpression);
            return true;
        } catch (com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException e) {
            return false;
        }
    }

    public Notification findById(String id) {
        return dynamoDBMapper.load(Notification.class, id);
    }

    public List<Notification> findByUserId(String userId) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":userId", new AttributeValue().withS(userId));

        DynamoDBQueryExpression<Notification> query = new DynamoDBQueryExpression<Notification>()
                .withIndexName(Notification.USER_INDEX)
                .withConsistentRead(false)
                .withKeyConditionExpression("userId = :userId")
                .withExpressionAttributeValues(values)
                .withScanIndexForward(false); // Newest first based on createdAt

        return dynamoDBMapper.query(Notification.class, query);
    }

    public List<Notification> findDueNotifications(long now, int limit, NotificationStatus status) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":status", new AttributeValue().withS(status.name()));
        values.put(":now", new AttributeValue().withN(String.valueOf(now)));

        DynamoDBQueryExpression<Notification> query = new DynamoDBQueryExpression<Notification>()
                .withIndexName(Notification.STATUS_SCHEDULED_INDEX)
                .withConsistentRead(false)
                .withKeyConditionExpression("#st = :status and scheduledAt <= :now")
                .withExpressionAttributeNames(Collections.singletonMap("#st", "status"))
                .withExpressionAttributeValues(values)
                .withLimit(limit);

        return dynamoDBMapper.queryPage(Notification.class, query).getResults();
    }

    public boolean existsByRecurrenceKey(String recurrenceKey) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":rk", new AttributeValue().withS(recurrenceKey));

        DynamoDBQueryExpression<Notification> query = new DynamoDBQueryExpression<Notification>()
                .withIndexName(Notification.RECURRENCE_KEY_INDEX)
                .withConsistentRead(false)
                .withKeyConditionExpression("recurrenceKey = :rk")
                .withExpressionAttributeValues(values)
                .withLimit(1);

        List<Notification> results = dynamoDBMapper.queryPage(Notification.class, query).getResults();
        return !results.isEmpty();
    }

    public List<Notification> findByBatchId(String batchId) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":batchId", new AttributeValue().withS(batchId));

        DynamoDBQueryExpression<Notification> query = new DynamoDBQueryExpression<Notification>()
                .withIndexName(Notification.BATCH_INDEX)
                .withConsistentRead(false)
                .withKeyConditionExpression("batchId = :batchId")
                .withExpressionAttributeValues(values);

        return dynamoDBMapper.query(Notification.class, query);
    }

    public boolean acquireLock(String notificationId, NotificationStatus currentStatus, NotificationStatus newStatus) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("notificationId", new AttributeValue().withS(notificationId));

            Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate> updates = new HashMap<>();
            updates.put("status", new com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate()
                    .withAction(com.amazonaws.services.dynamodbv2.model.AttributeAction.PUT)
                    .withValue(new AttributeValue().withS(newStatus.name())));
            updates.put("updatedAt", new com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate()
                    .withAction(com.amazonaws.services.dynamodbv2.model.AttributeAction.PUT)
                    .withValue(new AttributeValue().withN(String.valueOf(System.currentTimeMillis()))));

            Map<String, com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue> expected = new HashMap<>();
            expected.put("status", new com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue()
                    .withComparisonOperator(com.amazonaws.services.dynamodbv2.model.ComparisonOperator.EQ)
                    .withValue(new AttributeValue().withS(currentStatus.name())));

            com.amazonaws.services.dynamodbv2.model.UpdateItemRequest request = new com.amazonaws.services.dynamodbv2.model.UpdateItemRequest()
                    .withTableName(Notification.TABLE_NAME)
                    .withKey(key)
                    .withAttributeUpdates(updates)
                    .withExpected(expected);

            amazonDynamoDB.updateItem(request);
            return true;
        } catch (com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException e) {
            return false;
        }
    }

    public void delete(Notification notification) {
        dynamoDBMapper.delete(notification);
    }
}
