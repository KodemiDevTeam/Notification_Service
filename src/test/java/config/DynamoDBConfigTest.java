package config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import org.junit.jupiter.api.Test;
import org.notification.config.DynamoDBConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DynamoDBConfigTest {

    // Test 1: with local endpoint (covers the if-branch)
    @Test
    void testAmazonDynamoDB_withLocalEndpoint() {
        DynamoDBConfig config = new DynamoDBConfig("us-east-1", "http://localhost:8000");
        AmazonDynamoDB dynamoDB = config.amazonDynamoDB();
        assertNotNull(dynamoDB);
    }

    // Test 2: without endpoint (covers the else-branch)
    @Test
    void testAmazonDynamoDB_withoutEndpoint() {
        DynamoDBConfig config = new DynamoDBConfig("us-east-1", "");
        AmazonDynamoDB dynamoDB = config.amazonDynamoDB();
        assertNotNull(dynamoDB);
    }

    // Test 3: null endpoint (covers null check)
    @Test
    void testAmazonDynamoDB_withNullEndpoint() {
        DynamoDBConfig config = new DynamoDBConfig("us-east-1", null);
        AmazonDynamoDB dynamoDB = config.amazonDynamoDB();
        assertNotNull(dynamoDB);
    }
}
