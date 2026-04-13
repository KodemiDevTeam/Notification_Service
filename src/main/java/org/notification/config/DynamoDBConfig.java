package org.notification.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDynamoDBRepositories(basePackages = "org.notification.repository")
public class DynamoDBConfig {

    private final String region;
    private final String endpoint;

    public DynamoDBConfig(
            @Value("${aws.region:us-east-1}") String region,
            @Value("${aws.dynamodb.endpoint:}") String endpoint) {

        this.region = region;
        this.endpoint = endpoint;
    }

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {

        AmazonDynamoDBClientBuilder builder =
                AmazonDynamoDBClientBuilder.standard()
                        // ✅ Secure credential handling
                        .withCredentials(
                                DefaultAWSCredentialsProviderChain.getInstance()
                        );

        // If using local DynamoDB
        if (endpoint != null && !endpoint.isBlank()) {

            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(endpoint, region));

        }
        // If using AWS Cloud DynamoDB
        else {

            builder.withRegion(region);

        }

        return builder.build();
    }
}