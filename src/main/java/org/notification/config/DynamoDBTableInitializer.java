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
            
            CreateTableRequest request = new CreateTableRequest()
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
                                            new KeySchemaElement("userId", KeyType.HASH),
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

    private void waitForTableAndIndexesToBecomeActive() {
        boolean active = false;
        int maxRetries = 20;
        int retries = 0;

        log.info("Waiting for table {} and its GSIs to become ACTIVE...", Notification.TABLE_NAME);

        while (!active && retries < maxRetries) {
            try {
                DescribeTableResult result = amazonDynamoDB.describeTable(Notification.TABLE_NAME);
                String tableStatus = result.getTable().getTableStatus();

                if (TableStatus.ACTIVE.toString().equals(tableStatus)) {
                    boolean allIndexesActive = true;
                    if (result.getTable().getGlobalSecondaryIndexes() != null) {
                        for (GlobalSecondaryIndexDescription gsi : result.getTable().getGlobalSecondaryIndexes()) {
                            if (!TableStatus.ACTIVE.toString().equals(gsi.getIndexStatus())) {
                                allIndexesActive = false;
                                log.info("GSI {} is currently {}. Waiting...", gsi.getIndexName(), gsi.getIndexStatus());
                                break;
                            }
                        }
                    }

                    if (allIndexesActive) {
                        log.info("Table {} and all its GSIs are completely ACTIVE and ready.", Notification.TABLE_NAME);
                        active = true;
                        continue;
                    }
                } else {
                    log.info("Table {} is currently {}. Waiting...", Notification.TABLE_NAME, tableStatus);
                }
            } catch (ResourceNotFoundException e) {
                log.info("Table {} not yet visible in AWS...", Notification.TABLE_NAME);
            }

            retries++;
            try {
                Thread.sleep(5000); // 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!active) {
            log.error("Timed out waiting for table {} to become active.", Notification.TABLE_NAME);
        }
    }
}
