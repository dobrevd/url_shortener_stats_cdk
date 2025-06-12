package handler;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class EventCleaner {
    private final DynamoDbTable<UrlEvent> urlEventTable;

    public void clean() {
        long cutoff = Instant.now().minus(Duration.ofDays(60)).getEpochSecond();
        long cutoffResolveEvent = Instant.now().minus(Duration.ofDays(30)).getEpochSecond();

        List<UrlEvent> createEvents = findAllCreateEvents(cutoff);
        List<UrlEvent> toDelete = findAllTypeEvents(createEvents, cutoffResolveEvent);

        if (!toDelete.isEmpty()) {
            var urlEvents = deleteEvents(toDelete);
            storeDeletedEventsToS3(urlEvents);
        }

    }

    private List<UrlEvent> findAllCreateEvents(long cutoff) {
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

    private List<UrlEvent> findAllTypeEvents(@NotNull List<UrlEvent> createEvents, long cutoffResolveEvent) {
        Predicate<UrlEvent> resolveEventPredicate = createEventPredicate(cutoffResolveEvent);

        return createEvents.parallelStream()
                .map(event -> urlEventTable.index("hashTimestampIdx")
                        .query(buildLatestEventQuery(event.getShortUrlHash()))
                        .stream()
                        .flatMap(page -> page.items().stream())
                        .findFirst()
                        .filter(resolveEventPredicate)
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private void storeDeletedEventsToS3(List<UrlEvent> toDelete) {
    }

    private List<UrlEvent> deleteEvents(List<UrlEvent> toDelete) {
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

    private Predicate<UrlEvent> createEventPredicate(long cutoffResolveEvent) {
        return event -> "RESOLVE".equals(event.getEventType()) && event.getTimestamp() < cutoffResolveEvent;
    }
}
