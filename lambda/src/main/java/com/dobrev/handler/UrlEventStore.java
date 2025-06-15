package com.dobrev.handler;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class UrlEventStore {
    private final DynamoDbTable<UrlEvent> urlEventTable;

    public List<UrlEvent> findAllCreateEvents(long cutoff) {
        Expression expression = Expression.builder()
                .expression("eventType = :create AND #ts < :cutoff")
                .expressionNames(Map.of("#ts", "timestamp"))
                .expressionValues(Map.of(
                        ":create", AttributeValue.fromS("CREATE"),
                        ":cutoff", AttributeValue.fromN(Long.toString(cutoff))
                ))
                .build();

        return urlEventTable.scan(r -> r.filterExpression(expression))
                .items().stream()
                .toList();
    }

    public UrlEvent findLatestMatchingEvent(String shortUrlHash, Predicate<UrlEvent> predicate) {
        return urlEventTable.index("hashTimestampIdx")
                .query(buildLatestEventQuery(shortUrlHash))
                .stream()
                .flatMap(page -> page.items().stream())
                .findFirst()
                .filter(predicate)
                .orElse(null);
    }

    public List<UrlEvent> deleteEvents(List<UrlEvent> toDelete) {
        return toDelete.stream()
                .map(event -> urlEventTable.deleteItem(Key.builder()
                        .partitionValue(event.getEventId())
                        .sortValue(event.getTimestamp())
                        .build()))
                .toList();
    }

    private QueryEnhancedRequest buildLatestEventQuery(String shortUrlHash) {
        return QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(shortUrlHash).build()))
                .scanIndexForward(false)
                .limit(1)
                .build();
    }
}
