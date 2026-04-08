package org.notification.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.notification.model.enums.NotificationType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@DynamoDBTable(tableName = "inapp_notifications")
public class InAppNotification {

    // ==============================
    // CONSTANTS (Prevents Magic Strings)
    // ==============================

    public static final String TABLE_NAME = "inapp_notifications";
    public static final String USER_ID_INDEX = "userId-index";

    // ==============================
    // PRIMARY KEY
    // ==============================

    @DynamoDBHashKey(attributeName = "id")
    private String id;

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

    // ==============================
    // OPTIONAL / SYSTEM FIELDS
    // ==============================

    // Default value prevents null issues
    @DynamoDBAttribute(attributeName = "isRead")
    private Boolean isRead = Boolean.FALSE;

    @DynamoDBAttribute(attributeName = "createdAt")
    private Long createdAt;

    @DynamoDBAttribute(attributeName = "redirectUrl")
    private String redirectUrl;

    public void setMessage(String message) {
    }

    public void setStatus(String statusSent) {
    }
}