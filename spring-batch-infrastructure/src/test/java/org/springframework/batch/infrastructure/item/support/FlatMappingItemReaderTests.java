package org.springframework.batch.infrastructure.item.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class FlatMappingItemReaderTests {
    @Test
    void mapsTheItemsReadByTheUpstreamItemReader_readingTheResultingItemsInSequence() throws Exception {
        // given
        ListItemReader<TestUpstreamItem> upstreamItemReader = new ListItemReader<>(List.of(
                new TestUpstreamItem(2),
                new TestUpstreamItem(3)
        ));
        FlatMappingItemReader<TestUpstreamItem, TestItem> flatMappingItemReader = new FlatMappingItemReader<>(upstreamItemReader,
                testUpstreamItem -> IntStream.range(0, testUpstreamItem.number())
                        .mapToObj(n -> new TestItem(testUpstreamItem.number() * 2 + n))
                        .toList());

        // when
        // then
        assertThat(Objects.requireNonNull(flatMappingItemReader.read()).number()).isEqualTo(4);
        assertThat(Objects.requireNonNull(flatMappingItemReader.read()).number()).isEqualTo(5);
        assertThat(Objects.requireNonNull(flatMappingItemReader.read()).number()).isEqualTo(6);
        assertThat(Objects.requireNonNull(flatMappingItemReader.read()).number()).isEqualTo(7);
        assertThat(Objects.requireNonNull(flatMappingItemReader.read()).number()).isEqualTo(8);
        assertThat(flatMappingItemReader.read()).isNull();
    }

    private record TestUpstreamItem(int number) {
    }

    private record TestItem(int number) {
    }
}