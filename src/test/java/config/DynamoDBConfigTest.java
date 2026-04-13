package config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.notification.config.DynamoDBConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DynamoDBConfigTest {

    @ParameterizedTest
    @CsvSource({
        "us-east-1, http://localhost:8000",  // local endpoint (if-branch)
        "us-east-1, ''",                     // empty endpoint (else-branch)
        "us-east-1, "                        // null endpoint (null check)
    })
    void testAmazonDynamoDB(String region, String endpoint) {
        DynamoDBConfig config = new DynamoDBConfig(region, endpoint);
        AmazonDynamoDB dynamoDB = config.amazonDynamoDB();
        assertNotNull(dynamoDB);
    }
}
