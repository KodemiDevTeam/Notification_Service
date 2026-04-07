package org.Notification.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;

@Data
@DynamoDBTable(tableName = "user_preferences")
public class UserPreference {

    // ✅ Primary Key
    @DynamoDBHashKey(attributeName = "userId")
    private String userId;

    // ✅ Preferences (can be null = allowed)
    @DynamoDBAttribute(attributeName = "studentFeedback")
    private Boolean studentFeedback;

    @DynamoDBAttribute(attributeName = "liveClassReminder")
    private Boolean liveClassReminder;

    @DynamoDBAttribute(attributeName = "payoutUpdate")
    private Boolean payoutUpdate;

    @DynamoDBAttribute(attributeName = "streakUpdate")
    private Boolean streakUpdate;

    @DynamoDBAttribute(attributeName = "newEnrollment")
    private Boolean newEnrollment;
}