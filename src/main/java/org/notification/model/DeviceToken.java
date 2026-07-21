package org.notification.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = DeviceToken.TABLE_NAME)
public class DeviceToken {

    public static final String TABLE_NAME = "device_tokens";
    public static final String USER_INDEX = "userId-index";

    @DynamoDBHashKey(attributeName = "token")
    private String token;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = USER_INDEX, attributeName = "userId")
    private String userId;

    @DynamoDBAttribute(attributeName = "platform")
    private String platform;

    @DynamoDBAttribute(attributeName = "createdAt")
    private Long createdAt;
}
