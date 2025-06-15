package com.dobrev.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;
import java.util.function.Consumer;

import static com.dobrev.util.TestDataFactory.getUrlEvents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UrlEventStoreTest {
    @Mock
    private DynamoDbTable<UrlEvent> urlEventTable;
    @InjectMocks
    private UrlEventStore urlEventStore;

    @Test
    void testFindAllCreateEvents() {
        // given - precondition
        long cutoff = 1620172800001L;
        List<UrlEvent> mockedEvents = List.of(getUrlEvents().get(0), getUrlEvents().get(1), getUrlEvents().get(2));

        PageIterable mockPageIterable = Mockito.mock(PageIterable.class);
        SdkIterable<UrlEvent> mockSdkIterable = mockedEvents::iterator;

        when(mockPageIterable.items()).thenReturn(mockSdkIterable);
        when(urlEventTable.scan(any(Consumer.class))).thenReturn(mockPageIterable);

        // when - action
        List<UrlEvent> result = urlEventStore.findAllCreateEvents(cutoff);

        // then - verify the output
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrderElementsOf(getUrlEvents().stream().limit(3).toList());
    }
}
