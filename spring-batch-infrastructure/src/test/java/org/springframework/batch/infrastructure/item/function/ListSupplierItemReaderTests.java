package org.springframework.batch.infrastructure.item.function;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ListSupplierItemReaderTests {
    @Test
    void readsItemsFromSupplierInSequence_returningNullWhenThereAreNoRemainingElements() throws Exception {
        ListSupplierItemReader<TestItem> listSupplierItemReader = new ListSupplierItemReader<>(() -> List.of(
                new TestItem(1),
                new TestItem(2),
                new TestItem(3)));

        assertThat(Objects.requireNonNull(listSupplierItemReader.read()).number()).isEqualTo(1);
        assertThat(Objects.requireNonNull(listSupplierItemReader.read()).number()).isEqualTo(2);
        assertThat(Objects.requireNonNull(listSupplierItemReader.read()).number()).isEqualTo(3);
        assertThat(listSupplierItemReader.read()).isNull();
    }

    @Test
    void callsTheSupplierExactlyOnceDuringTheFirstRead() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        ListSupplierItemReader<TestItem> listSupplierItemReader = new ListSupplierItemReader<>(() -> {
            callCount.getAndIncrement();
            return List.of(new TestItem(1), new TestItem(2));
        });

        assertThat(callCount.get()).isEqualTo(0);
        assertThat(Objects.requireNonNull(listSupplierItemReader.read()).number()).isEqualTo(1);
        assertThat(callCount.get()).isEqualTo(1);
        assertThat(Objects.requireNonNull(listSupplierItemReader.read()).number()).isEqualTo(2);
        assertThat(callCount.get()).isEqualTo(1);
        assertThat(listSupplierItemReader.read()).isNull();
    }

    private record TestItem(int number) {
    }
}