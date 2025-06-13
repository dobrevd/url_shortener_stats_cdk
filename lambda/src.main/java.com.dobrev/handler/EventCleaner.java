package handler;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class EventCleaner {
    private final UrlEventStore urlEventStore;
    private final S3EventArchiver s3EventArchiver;

    public void clean() {
        long cutoff = Instant.now().minus(Duration.ofDays(60)).getEpochSecond();
        long cutoffResolveEvent = Instant.now().minus(Duration.ofDays(30)).getEpochSecond();

        List<UrlEvent> createEvents = urlEventStore.findAllCreateEvents(cutoff);
        List<UrlEvent> toDelete = findAllTypeEvents(createEvents, cutoffResolveEvent);

        if (!toDelete.isEmpty()) {
            var urlEvents = urlEventStore.deleteEvents(toDelete);
            storeDeletedEventsToS3(urlEvents);
        }
    }

    private List<UrlEvent> findAllTypeEvents(@NotNull List<UrlEvent> createEvents, long cutoffResolveEvent) {
        Predicate<UrlEvent> resolveEventPredicate = createEventPredicate(cutoffResolveEvent);

        return createEvents.parallelStream()
                .map(event -> urlEventStore.findLatestMatchingEvent(event.getShortUrlHash(), resolveEventPredicate))
                .filter(Objects::nonNull)
                .toList();
    }

    private void storeDeletedEventsToS3(List<UrlEvent> toDelete) {
        s3EventArchiver.storeDeletedEventsToS3(toDelete);
    }

    private Predicate<UrlEvent> createEventPredicate(long cutoffResolveEvent) {
        return event -> "RESOLVE".equals(event.getEventType()) && event.getTimestamp() < cutoffResolveEvent;
    }
}
