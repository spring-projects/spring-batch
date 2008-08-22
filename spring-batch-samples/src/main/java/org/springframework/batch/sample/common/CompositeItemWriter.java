package org.springframework.batch.sample.common;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;

public class CompositeItemWriter<T> implements ItemWriter<T> {

    ItemWriter<T> itemWriter;

    public CompositeItemWriter(ItemWriter<T> itemWriter) {
      this.itemWriter = itemWriter;
    }

    public void write(T item) throws Exception {

      //Add business logic here

      itemWriter.write(item);
    }

    public void clear() throws ClearFailedException {
      itemWriter.clear();
    }

    public void flush() throws FlushFailedException {
      itemWriter.flush();
    }
}
