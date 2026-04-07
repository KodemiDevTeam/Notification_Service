package org.Notification.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;

import org.Notification.model.enums.NotificationType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@DynamoDBTable(tableName = "inapp_notifications")
public class InAppNotification {

    // ✅ Primary Key
    @DynamoDBHashKey(attributeName = "id")
    private String id;

    // ✅ GSI for user-based query
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "userId-index", attributeName = "userId")
    private String userId;

    // ✅ REQUIRED (UI)
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

    // ✅ SYSTEM / OPTIONAL
    @DynamoDBAttribute(attributeName = "isRead")
    private Boolean isRead;

    @DynamoDBAttribute(attributeName = "createdAt")
    private Long createdAt;

    @DynamoDBAttribute(attributeName = "redirectUrl")
    private String redirectUrl;

    public void setMessage(String message) {
    }

    public void setStatus(String sent) {
    }
}