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

    private final AmazonDynamoDB amazonDynamoDB;

    public DynamoDBTableInitializer(AmazonDynamoDB amazonDynamoDB) {
        this.amazonDynamoDB = amazonDynamoDB;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        createTableIfMissing();
        waitForTableAndIndexesToBecomeActive();
    }

    private void createTableIfMissing() {
        try {
            DescribeTableResult result = amazonDynamoDB.describeTable(Notification.TABLE_NAME);
            log.info("Table {} already exists. Status: {}", Notification.TABLE_NAME, result.getTable().getTableStatus());
        } catch (ResourceNotFoundException e) {
            log.warn("Table {} not found. Creating it now...", Notification.TABLE_NAME);
            amazonDynamoDB.createTable(buildCreateTableRequest());
            log.info("Creation request sent for table: {}", Notification.TABLE_NAME);
        }
    }

    private CreateTableRequest buildCreateTableRequest() {
        return new CreateTableRequest()
                .withTableName(Notification.TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition("notificationId", ScalarAttributeType.S),
                        new AttributeDefinition("status", ScalarAttributeType.S),
                        new AttributeDefinition("scheduledAt", ScalarAttributeType.N),
                        new AttributeDefinition("batchId", ScalarAttributeType.S),
                        new AttributeDefinition("userId", ScalarAttributeType.S),
                        new AttributeDefinition("createdAt", ScalarAttributeType.N),
                        new AttributeDefinition("recurrenceKey", ScalarAttributeType.S)
                )
                .withKeySchema(new KeySchemaElement("notificationId", KeyType.HASH))
                .withGlobalSecondaryIndexes(
                        buildGsi(Notification.STATUS_SCHEDULED_INDEX, "status", "scheduledAt"),
                        buildGsi(Notification.USER_INDEX, "userId", "createdAt"),
                        buildHashOnlyGsi(Notification.BATCH_INDEX, "batchId"),
                        buildHashOnlyGsi(Notification.RECURRENCE_KEY_INDEX, "recurrenceKey")
                )
                .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
    }

    private GlobalSecondaryIndex buildGsi(String indexName, String hashKey, String rangeKey) {
        return new GlobalSecondaryIndex()
                .withIndexName(indexName)
                .withKeySchema(
                        new KeySchemaElement(hashKey, KeyType.HASH),
                        new KeySchemaElement(rangeKey, KeyType.RANGE)
                )
                .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
    }

    private GlobalSecondaryIndex buildHashOnlyGsi(String indexName, String hashKey) {
        return new GlobalSecondaryIndex()
                .withIndexName(indexName)
                .withKeySchema(new KeySchemaElement(hashKey, KeyType.HASH))
                .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
    }

    private void waitForTableAndIndexesToBecomeActive() {
        log.info("Waiting for table {} and its GSIs to become ACTIVE...", Notification.TABLE_NAME);

        int maxRetries = 20;
        for (int retries = 0; retries < maxRetries; retries++) {
            if (isTableAndIndexesActive()) {
                log.info("Table {} and all its GSIs are completely ACTIVE and ready.", Notification.TABLE_NAME);
                return;
            }
            sleepFor(5000);
        }

        log.error("Timed out waiting for table {} to become active.", Notification.TABLE_NAME);
    }

    private boolean isTableAndIndexesActive() {
        try {
            DescribeTableResult result = amazonDynamoDB.describeTable(Notification.TABLE_NAME);
            String tableStatus = result.getTable().getTableStatus();

            if (!TableStatus.ACTIVE.toString().equals(tableStatus)) {
                log.info("Table {} is currently {}. Waiting...", Notification.TABLE_NAME, tableStatus);
                return false;
            }

            return areAllIndexesActive(result);
        } catch (ResourceNotFoundException e) {
            log.info("Table {} not yet visible in AWS...", Notification.TABLE_NAME);
            return false;
        }
    }

    private boolean areAllIndexesActive(DescribeTableResult result) {
        if (result.getTable().getGlobalSecondaryIndexes() == null) {
            return true;
        }
        for (GlobalSecondaryIndexDescription gsi : result.getTable().getGlobalSecondaryIndexes()) {
            if (!TableStatus.ACTIVE.toString().equals(gsi.getIndexStatus())) {
                log.info("GSI {} is currently {}. Waiting...", gsi.getIndexName(), gsi.getIndexStatus());
                return false;
            }
        }
        return true;
    }

    private void sleepFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
