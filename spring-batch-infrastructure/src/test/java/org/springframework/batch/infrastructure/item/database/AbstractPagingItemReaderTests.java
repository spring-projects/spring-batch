package org.springframework.batch.infrastructure.item.database;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractPagingItemReaderTests {

  static class PagingItemReader extends AbstractPagingItemReader<Integer> {

    private final List<Integer> data = List.of(
        0,1,2,3,4,5,6,7,8,9,
        10,11,12,13,14,15,16,17,18,19
    );

    @Override
    protected void doReadPage() {
      int start = getPage() * getPageSize();
      int end = Math.min(start + getPageSize(), data.size());

      if (start >= data.size()) {
        results = List.of();
        return;
      }

      List<Integer> pageData = new ArrayList<>();
      for (int i = start; i < end; i++) {
        pageData.add(data.get(i));
      }

      this.results = pageData;
    }

    @Override
    protected void doOpen() {}
  }

  @Test
  void jumpToItem_shouldReadExactItem_afterJump() throws Exception {
    PagingItemReader reader = new PagingItemReader();
    reader.open(new ExecutionContext());

    reader.jumpToItem(7);
    assertEquals(7, reader.read());
  }

  @Test
  void jumpToItem_zeroIndex() throws Exception {
    PagingItemReader reader = new PagingItemReader();
    reader.open(new ExecutionContext());

    reader.jumpToItem(0);
    assertEquals(0, reader.read());
  }

  @Test
  void jumpToItem_lastItemInFirstPage() throws Exception {
    PagingItemReader reader = new PagingItemReader();
    reader.open(new ExecutionContext());

    reader.jumpToItem(9);
    assertEquals(9, reader.read());
  }

  @Test
  void jumpToItem_firstItemOfNextPage() throws Exception {
    PagingItemReader reader = new PagingItemReader();
    reader.open(new ExecutionContext());

    reader.jumpToItem(10);
    assertEquals(10, reader.read());
  }
}