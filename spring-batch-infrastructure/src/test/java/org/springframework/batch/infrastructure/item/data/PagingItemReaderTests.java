package org.springframework.batch.infrastructure.item.data;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PagingItemReaderTests {
    @Test
    void readsItemsBasedOnPageSize_returningNullWhenThereAreNoRemainingElementsAndNoRemainingPages() throws Exception {
        int totalItems = 2;
        int pageSize = 1;
        PagingItemReader<TestItem> pagingItemReader = new PagingItemReader<>(
                pageable ->
                        switch (pageable.getPageNumber()) {
                            case 0 -> new PageImpl<>(
                                    List.of(new TestItem(1)),
                                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                                    totalItems);
                            case 1 -> new PageImpl<>(
                                    List.of(new TestItem(2)),
                                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                                    totalItems);
                            default -> throw new IllegalStateException("Unexpected value: " + pageable.getPageNumber());
                        },
                pageSize);

        assertThat(Objects.requireNonNull(pagingItemReader.read()).number()).isEqualTo(1);
        assertThat(Objects.requireNonNull(pagingItemReader.read()).number()).isEqualTo(2);
        assertThat(pagingItemReader.read()).isNull();
    }

    private record TestItem(int number) {
    }
}