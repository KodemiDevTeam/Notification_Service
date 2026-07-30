package org.notification.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DynamoDBTableInitializerTest {

    private AmazonDynamoDB amazonDynamoDB;
    private DynamoDBTableInitializer initializer;

    @BeforeEach
    void setUp() {
        amazonDynamoDB = mock(AmazonDynamoDB.class);
        initializer = new DynamoDBTableInitializer(amazonDynamoDB);
    }

    @Test
    void testOnApplicationReady_TablesAlreadyExistAndActive() {
        TableDescription activeTableDesc = new TableDescription()
                .withTableStatus(TableStatus.ACTIVE)
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndexDescription()
                                .withIndexName("status-scheduledAt-index")
                                .withIndexStatus(IndexStatus.ACTIVE)
                );

        DescribeTableResult activeResult = new DescribeTableResult().withTable(activeTableDesc);

        when(amazonDynamoDB.describeTable(anyString())).thenReturn(activeResult);

        assertDoesNotThrow(() -> initializer.onApplicationReady());

        verify(amazonDynamoDB, never()).createTable(any(CreateTableRequest.class));
    }

    @Test
    void testOnApplicationReady_TablesMissing_CreatesTables() {
        TableDescription activeTableDesc = new TableDescription()
                .withTableStatus(TableStatus.ACTIVE);

        DescribeTableResult activeResult = new DescribeTableResult().withTable(activeTableDesc);

        when(amazonDynamoDB.describeTable(anyString()))
                .thenThrow(new ResourceNotFoundException("Table 1 not found"))
                .thenThrow(new ResourceNotFoundException("Table 2 not found"))
                .thenReturn(activeResult);

        assertDoesNotThrow(() -> initializer.onApplicationReady());

        verify(amazonDynamoDB, times(2)).createTable(any(CreateTableRequest.class));
    }


    @Test
    void testOnApplicationReady_GsiInactiveThenActive() {
        TableDescription inactiveGsiDesc = new TableDescription()
                .withTableStatus(TableStatus.ACTIVE)
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndexDescription()
                                .withIndexName("idx1")
                                .withIndexStatus(IndexStatus.CREATING)
                );

        TableDescription activeGsiDesc = new TableDescription()
                .withTableStatus(TableStatus.ACTIVE)
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndexDescription()
                                .withIndexName("idx1")
                                .withIndexStatus(IndexStatus.ACTIVE)
                );

        when(amazonDynamoDB.describeTable(anyString()))
                .thenReturn(new DescribeTableResult().withTable(inactiveGsiDesc))
                .thenReturn(new DescribeTableResult().withTable(activeGsiDesc));

        assertDoesNotThrow(() -> initializer.onApplicationReady());
    }

    @Test
    void testTableNotActive_ReturnsFalseInWait() {
        TableDescription creatingTableDesc = new TableDescription()
                .withTableStatus(TableStatus.CREATING);

        TableDescription activeTableDesc = new TableDescription()
                .withTableStatus(TableStatus.ACTIVE)
                .withGlobalSecondaryIndexes((List<GlobalSecondaryIndexDescription>) null);

        when(amazonDynamoDB.describeTable(anyString()))
                .thenThrow(new ResourceNotFoundException("Table not visible yet"))
                .thenReturn(new DescribeTableResult().withTable(creatingTableDesc))
                .thenReturn(new DescribeTableResult().withTable(activeTableDesc));

        assertDoesNotThrow(() -> initializer.onApplicationReady());
    }
}

