package org.notification.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import lombok.extern.slf4j.Slf4j;
import org.notification.model.Notification;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DynamoDBTableInitializer {

    private static final String ATTR_USER_ID = "userId";

    private final AmazonDynamoDB amazonDynamoDB;

    public DynamoDBTableInitializer(AmazonDynamoDB amazonDynamoDB) {
        this.amazonDynamoDB = amazonDynamoDB;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        createTableIfMissing();
        createDeviceTokenTableIfMissing();
        waitForTableAndIndexesToBecomeActive(Notification.TABLE_NAME);
        waitForTableAndIndexesToBecomeActive(org.notification.model.DeviceToken.TABLE_NAME);
    }

    private void createTableIfMissing() {
        try {
            DescribeTableResult result = amazonDynamoDB.describeTable(Notification.TABLE_NAME);
            log.info("Table {} already exists. Status: {}", Notification.TABLE_NAME, result.getTable().getTableStatus());

        } catch (ResourceNotFoundException e) {
            log.warn("Table {} not found. Creating it now...", Notification.TABLE_NAME);

            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(Notification.TABLE_NAME)
                    .withAttributeDefinitions(
                            new AttributeDefinition("notificationId", ScalarAttributeType.S),
                            new AttributeDefinition("status", ScalarAttributeType.S),
                            new AttributeDefinition("scheduledAt", ScalarAttributeType.N),
                            new AttributeDefinition("batchId", ScalarAttributeType.S),
                            new AttributeDefinition(ATTR_USER_ID, ScalarAttributeType.S),
                            new AttributeDefinition("createdAt", ScalarAttributeType.N),
                            new AttributeDefinition("recurrenceKey", ScalarAttributeType.S)
                    )
                    .withKeySchema(
                            new KeySchemaElement("notificationId", KeyType.HASH)
                    )
                    .withGlobalSecondaryIndexes(
                            new GlobalSecondaryIndex()
                                    .withIndexName(Notification.STATUS_SCHEDULED_INDEX)
                                    .withKeySchema(
                                            new KeySchemaElement("status", KeyType.HASH),
                                            new KeySchemaElement("scheduledAt", KeyType.RANGE)
                                    )
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)),

                            new GlobalSecondaryIndex()
                                    .withIndexName(Notification.USER_INDEX)
                                    .withKeySchema(
                                            new KeySchemaElement(ATTR_USER_ID, KeyType.HASH),
                                            new KeySchemaElement("createdAt", KeyType.RANGE)
                                    )
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)),

                            new GlobalSecondaryIndex()
                                    .withIndexName(Notification.BATCH_INDEX)
                                    .withKeySchema(
                                            new KeySchemaElement("batchId", KeyType.HASH)
                                    )
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)),

                            new GlobalSecondaryIndex()
                                    .withIndexName(Notification.RECURRENCE_KEY_INDEX)
                                    .withKeySchema(
                                            new KeySchemaElement("recurrenceKey", KeyType.HASH)
                                    )
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                    )
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

            amazonDynamoDB.createTable(request);
            log.info("Creation request sent for table: {}", Notification.TABLE_NAME);
        }
    }

    private void createDeviceTokenTableIfMissing() {
        try {
            DescribeTableResult result = amazonDynamoDB.describeTable(org.notification.model.DeviceToken.TABLE_NAME);
            log.info("Table {} already exists. Status: {}", org.notification.model.DeviceToken.TABLE_NAME, result.getTable().getTableStatus());

        } catch (ResourceNotFoundException e) {
            log.warn("Table {} not found. Creating it now...", org.notification.model.DeviceToken.TABLE_NAME);

            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(org.notification.model.DeviceToken.TABLE_NAME)
                    .withAttributeDefinitions(
                            new AttributeDefinition("token", ScalarAttributeType.S),
                            new AttributeDefinition(ATTR_USER_ID, ScalarAttributeType.S)
                    )
                    .withKeySchema(
                            new KeySchemaElement("token", KeyType.HASH)
                    )
                    .withGlobalSecondaryIndexes(
                            new GlobalSecondaryIndex()
                                    .withIndexName(org.notification.model.DeviceToken.USER_INDEX)
                                    .withKeySchema(
                                            new KeySchemaElement(ATTR_USER_ID, KeyType.HASH)
                                    )
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                    )
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

            amazonDynamoDB.createTable(request);
            log.info("Creation request sent for table: {}", org.notification.model.DeviceToken.TABLE_NAME);
        }
    }

    private void waitForTableAndIndexesToBecomeActive(String tableName) {
        boolean active = false;
        int maxRetries = 20;
        int retries = 0;

        log.info("Waiting for table {} and its GSIs to become ACTIVE...", tableName);

        while (!active && retries < maxRetries) {
            active = isTableAndIndexesActive(tableName);
            if (!active) {
                retries++;
                sleepBriefly();
            }
        }

        if (active) {
            log.info("Table {} and all its GSIs are completely ACTIVE and ready.", tableName);
        } else {
            log.error("Timed out waiting for table {} to become active.", tableName);
        }
    }

    private boolean isTableAndIndexesActive(String tableName) {
        try {
            DescribeTableResult result = amazonDynamoDB.describeTable(tableName);
            TableDescription table = result.getTable();

            if (!TableStatus.ACTIVE.toString().equals(table.getTableStatus())) {
                log.info("Table {} is currently {}. Waiting...", tableName, table.getTableStatus());
                return false;
            }

            return areAllGsisActive(table);
        } catch (ResourceNotFoundException e) {
            log.info("Table {} not yet visible in AWS...", tableName);
            return false;
        }
    }

    private boolean areAllGsisActive(TableDescription table) {
        if (table.getGlobalSecondaryIndexes() == null) {
            return true;
        }

        for (GlobalSecondaryIndexDescription gsi : table.getGlobalSecondaryIndexes()) {
            if (!TableStatus.ACTIVE.toString().equals(gsi.getIndexStatus())) {
                log.info("GSI {} is currently {}. Waiting...", gsi.getIndexName(), gsi.getIndexStatus());
                return false;
            }
        }
        return true;
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(5000); // 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}