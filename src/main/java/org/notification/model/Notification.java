package org.notification.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.notification.model.enums.NotificationType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@DynamoDBTable(tableName = Notification.TABLE_NAME)
public class Notification {

    // ==============================
    // CONSTANTS (Prevents Magic Strings)
    // ==============================

    public static final String TABLE_NAME = "notifications";
    public static final String USER_ID_INDEX = "userId-index";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    // ==============================
    // PRIMARY KEY
    // ==============================

    @DynamoDBHashKey(attributeName = "notificationId")
    private String notificationId;

    // ==============================
    // GLOBAL SECONDARY INDEX
    // ==============================

    @DynamoDBIndexHashKey(
            globalSecondaryIndexName = USER_ID_INDEX,
            attributeName = "userId"
    )
    private String userId;

    // ==============================
    // REQUIRED FIELDS
    // ==============================

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
    private String channel;

    // ==============================
    // OPTIONAL FIELDS
    // ==============================

    @DynamoDBAttribute(attributeName = "email")
    private String email;

    @DynamoDBAttribute(attributeName = "phoneNumber")
    private String phoneNumber;

    @DynamoDBAttribute(attributeName = "deviceToken")
    private String deviceToken;

    // ==============================
    // SYSTEM FIELDS
    // ==============================

    @DynamoDBAttribute(attributeName = "isRead")
    private Boolean isRead = Boolean.FALSE;

    @DynamoDBAttribute(attributeName = "isScheduled")
    private Boolean isScheduled = Boolean.FALSE;

    @DynamoDBAttribute(attributeName = "scheduledTime")
    private Long scheduledTime;

    @DynamoDBAttribute(attributeName = "createdAt")
    private Long createdAt;

    @DynamoDBAttribute(attributeName = "status")
    private String status = STATUS_PENDING;

    @DynamoDBAttribute(attributeName = "retryCount")
    private Integer retryCount = 0;

    @DynamoDBAttribute(attributeName = "redirectUrl")
    private String redirectUrl;

    public String getMessage() {
        // Returns description as the message content for channel compatibility
        return description != null ? description : "";
    }
}