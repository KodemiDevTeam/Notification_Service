package org.notification.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import org.notification.model.InAppNotification;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InAppRepository {

    // ==============================
    // CONSTANTS (Prevents Magic Strings)
    // ==============================

    private static final String USER_ID_INDEX = "userId-index";
    private static final String USER_ID_KEY = ":userId";
    private static final String KEY_CONDITION = "userId = :userId";

    private final DynamoDBMapper dynamoDBMapper;

    // Constructor Injection
    public InAppRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    // SAVE
    public void save(InAppNotification notif) {

        if (notif == null) {
            throw new IllegalArgumentException(
                    "Notification cannot be null"
            );
        }

        dynamoDBMapper.save(notif);
    }

    // FIND BY ID
    public Optional<InAppNotification> findById(String id) {

        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        InAppNotification notification =
                dynamoDBMapper.load(
                        InAppNotification.class,
                        id
                );

        return Optional.ofNullable(notification);
    }

    // GET BY USER
    public List<InAppNotification> getByUserId(String userId) {

        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }

        Map<String, AttributeValue> values =
                new HashMap<>();

        values.put(USER_ID_KEY,
                new AttributeValue().withS(userId));

        DynamoDBQueryExpression<InAppNotification> query =
                new DynamoDBQueryExpression<InAppNotification>()
                        .withIndexName(USER_ID_INDEX)
                        .withConsistentRead(false)
                        .withKeyConditionExpression(KEY_CONDITION)
                        .withExpressionAttributeValues(values);

        return dynamoDBMapper.query(
                InAppNotification.class,
                query
        );
    }

    // GET UNREAD
    public List<InAppNotification> getUnread(String userId) {

        List<InAppNotification> all =
                getByUserId(userId);

        List<InAppNotification> unread =
                new ArrayList<>();

        for (InAppNotification n : all) {

            if (Boolean.FALSE.equals(n.getIsRead())) {
                unread.add(n);
            }
        }

        return unread;
    }

    // DELETE
    public void delete(InAppNotification notif) {

        if (notif != null) {
            dynamoDBMapper.delete(notif);
        }
    }

    // DELETE BY ID
    public void deleteById(String id) {

        Optional<InAppNotification> notification =
                findById(id);

        notification.ifPresent(
                dynamoDBMapper::delete
        );
    }

}