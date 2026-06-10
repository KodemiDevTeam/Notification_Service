package org.notification.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationStatus;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NotificationRepository {

    private static final String STATUS = "status";

    private final DynamoDBMapper dynamoDBMapper;
    private final AmazonDynamoDB amazonDynamoDB;

    public NotificationRepository(DynamoDBMapper dynamoDBMapper, AmazonDynamoDB amazonDynamoDB) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.amazonDynamoDB = amazonDynamoDB;
    }

    public Notification save(Notification notification) {
        dynamoDBMapper.save(notification);
        return notification;
    }

    public boolean saveIdempotent(Notification notification) {
        try {
            DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
            Map<String, ExpectedAttributeValue> expected = new HashMap<>();
            expected.put("notificationId", new ExpectedAttributeValue().withExists(false));
            saveExpression.setExpected(expected);
            dynamoDBMapper.save(notification, saveExpression);
            return true;
        } catch (ConditionalCheckFailedException e) {
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
                .withScanIndexForward(false);

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
                .withExpressionAttributeNames(Collections.singletonMap("#st", STATUS))
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

        return !dynamoDBMapper.queryPage(Notification.class, query).getResults().isEmpty();
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

            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put(STATUS, new AttributeValueUpdate()
                    .withAction(AttributeAction.PUT)
                    .withValue(new AttributeValue().withS(newStatus.name())));
            updates.put("updatedAt", new AttributeValueUpdate()
                    .withAction(AttributeAction.PUT)
                    .withValue(new AttributeValue().withN(String.valueOf(System.currentTimeMillis()))));

            Map<String, ExpectedAttributeValue> expected = new HashMap<>();
            expected.put(STATUS, new ExpectedAttributeValue()
                    .withComparisonOperator(com.amazonaws.services.dynamodbv2.model.ComparisonOperator.EQ)
                    .withValue(new AttributeValue().withS(currentStatus.name())));

            UpdateItemRequest request = new UpdateItemRequest()
                    .withTableName(Notification.TABLE_NAME)
                    .withKey(key)
                    .withAttributeUpdates(updates)
                    .withExpected(expected);

            amazonDynamoDB.updateItem(request);
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public void delete(Notification notification) {
        dynamoDBMapper.delete(notification);
    }
}
