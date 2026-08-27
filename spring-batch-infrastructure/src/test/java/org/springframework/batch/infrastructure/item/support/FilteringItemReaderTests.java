package org.springframework.batch.infrastructure.item.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class FilteringItemReaderTests {
    @Test
    void filtersTheItemsReadByTheUpstreamItemReader() throws Exception {
        ListItemReader<TestItem> upstreamItemReader = new ListItemReader<>(List.of(
                new TestItem("---keep-1---"),
                new TestItem("---remove---"),
                new TestItem("---keep-2---"),
                new TestItem("---remove---"),
                new TestItem("---keep-3---")
        ));
        FilteringItemReader<TestItem> filteringItemReader = new FilteringItemReader<>(upstreamItemReader, testItem -> testItem.value().contains("keep"));
        assertThat(Objects.requireNonNull(filteringItemReader.read()).value()).isEqualTo("---keep-1---");
        assertThat(Objects.requireNonNull(filteringItemReader.read()).value()).isEqualTo("---keep-2---");
        assertThat(Objects.requireNonNull(filteringItemReader.read()).value()).isEqualTo("---keep-3---");
        assertThat(filteringItemReader.read()).isNull();
    }

    private record TestItem(String value) {
    }
}