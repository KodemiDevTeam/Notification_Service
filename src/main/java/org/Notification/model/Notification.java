package org.Notification.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;
import org.Notification.model.enums.NotificationType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@DynamoDBTable(tableName = "notifications")
public class Notification {

    // ✅ Primary Key
    @DynamoDBHashKey(attributeName = "notificationId")
    private String notificationId;

    // ✅ GSI (for querying by user)
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "userId-index", attributeName = "userId")
    private String userId;

    // ✅ REQUIRED FIELDS
    @NotBlank
    @DynamoDBAttribute(attributeName = "title")
    private String title;

    @NotBlank
    @DynamoDBAttribute(attributeName = "description")
    private String description;

    @NotNull
    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "type")
    private NotificationType type;

    @NotBlank
    @DynamoDBAttribute(attributeName = "channel")
    private String channel; // EMAIL / SMS / PUSH / IN_APP

    // ✅ OPTIONAL (based on channel)
    @DynamoDBAttribute(attributeName = "email")
    private String email;

    @DynamoDBAttribute(attributeName = "phoneNumber")
    private String phoneNumber;

    @DynamoDBAttribute(attributeName = "deviceToken")
    private String deviceToken;

    // ✅ SYSTEM FIELDS
    @DynamoDBAttribute(attributeName = "isRead")
    private Boolean isRead;

    @DynamoDBAttribute(attributeName = "isScheduled")
    private Boolean isScheduled;

    @DynamoDBAttribute(attributeName = "scheduledTime")
    private Long scheduledTime;

    @DynamoDBAttribute(attributeName = "createdAt")
    private Long createdAt;

    @DynamoDBAttribute(attributeName = "status")
    private String status; // PENDING / SENT / FAILED

    @DynamoDBAttribute(attributeName = "retryCount")
    private Integer retryCount;

    @DynamoDBAttribute(attributeName = "redirectUrl")
    private String redirectUrl;

    public String getMessage() {
        return "";
    }
}