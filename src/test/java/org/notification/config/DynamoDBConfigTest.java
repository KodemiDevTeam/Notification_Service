package org.notification.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DynamoDBConfigTest {

    @Test
    void testDynamoDBConfigBeans() {
        DynamoDBConfig config = new DynamoDBConfig("us-west-2");
        assertNotNull(config);

        AmazonDynamoDB mockDynamoDB = mock(AmazonDynamoDB.class);
        DynamoDBMapper mapper = config.dynamoDBMapper(mockDynamoDB);
        assertNotNull(mapper);
    }
}
