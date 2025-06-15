package com.dobrev.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbBean
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlEvent {
    private String eventId;
    private String userId;
    private String eventType;
    private String shortUrlHash;
    private String originalUrl;
    private long timestamp;

    @DynamoDbPartitionKey
    public String getEventId() {
        return eventId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "hashTimestampIdx")
    public String getShortUrlHash() {
        return shortUrlHash;
    }

    @DynamoDbSecondarySortKey(indexNames = "hashTimestampIdx")
    public long getTimestamp() {
        return timestamp;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "userIdEventTypeIdx")
    public String getUserId() {
        return userId;
    }

    @DynamoDbSecondarySortKey(indexNames = "{userIdEventTypeIdx}")
    public String getEventType() {
        return eventType;
    }
}
