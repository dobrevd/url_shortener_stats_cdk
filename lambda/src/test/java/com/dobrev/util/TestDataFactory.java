package com.dobrev.util;

import com.dobrev.handler.UrlEvent;

import java.util.Arrays;
import java.util.List;

public final class TestDataFactory {
    public static final String USER_ID = "123";
    public static final String CREATE_EVENT_TYPE = "CREATE";
    public static final String RESOLVE_EVENT_TYPE = "RESOLVE";

    public static List<UrlEvent> getUrlEvents() {
        return Arrays.asList(
                UrlEvent.builder()
                        .eventId("evt_001")
                        .userId(USER_ID)
                        .eventType(CREATE_EVENT_TYPE)
                        .shortUrlHash("abc123")
                        .originalUrl("https://example.com/nature1.jpg")
                        .timestamp(1620000000000L)  // 3. Mai 2021
                        .build(),

                UrlEvent.builder()
                        .eventId("evt_002")
                        .userId(USER_ID)
                        .eventType(RESOLVE_EVENT_TYPE)
                        .shortUrlHash("abc123")
                        .originalUrl("https://example.com/nature1.jpg")
                        .timestamp(1620086400000L)  // 4. Mai 2021
                        .build(),

                UrlEvent.builder()
                        .eventId("evt_003")
                        .userId(USER_ID)
                        .eventType(RESOLVE_EVENT_TYPE)
                        .shortUrlHash("abc123")
                        .originalUrl("https://example.com/nature1.jpg")
                        .timestamp(1620172800000L)  // 5. Mai 2021
                        .build(),

                UrlEvent.builder()
                        .eventId("evt_004")
                        .userId(USER_ID)
                        .eventType(CREATE_EVENT_TYPE)
                        .shortUrlHash("abc123")
                        .originalUrl("https://example.com/nature.jpg")
                        .timestamp(1620259200000L)  // 6. Mai 2021
                        .build(),

                UrlEvent.builder()
                        .eventId("evt_005")
                        .userId(USER_ID)
                        .eventType(CREATE_EVENT_TYPE)
                        .shortUrlHash("abc123")
                        .originalUrl("https://example.com/nature.jpg")
                        .timestamp(1620345600000L)  // 7. Mai 2021
                        .build(),

                UrlEvent.builder()
                        .eventId("evt_006")
                        .userId(USER_ID)
                        .eventType(CREATE_EVENT_TYPE)
                        .shortUrlHash("abc123")
                        .originalUrl("https://example.com/nature.jpg")
                        .timestamp(1620432000000L)  // 8. Mai 2021
                        .build(),

                UrlEvent.builder()
                        .eventId("evt_007")
                        .userId(USER_ID)
                        .eventType(CREATE_EVENT_TYPE)
                        .shortUrlHash("abc123")
                        .originalUrl("https://example.com/nature.jpg")
                        .timestamp(1620518400000L)  // 9. Mai 2021
                        .build(),

                UrlEvent.builder()
                        .eventId("evt_008")
                        .userId(USER_ID)
                        .eventType(CREATE_EVENT_TYPE)
                        .shortUrlHash("abc123")
                        .originalUrl("https://example.com/nature.jpg")
                        .timestamp(1620604800000L)  // 10. Mai 2021
                        .build()
        );
    }
}
