package org.springframework.batch.item.support.builder;

import org.springframework.batch.item.ItemFetcher;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.CompositeItemReader;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * Creates a fully qualified {@link CompositeItemReader}.
 *
 * @author Wiktor Kęska
 */
class CompositeItemReaderBuilder<I, O> {

  private List<? extends ItemFetcher<?, ?>> fetchers;
  private AbstractItemStreamItemReader<I> reader;

  /**
   * The reader to read base value.
   *
   * @param reader the reader to use. The reader must not be null.
   *
   * @return this instance for method chaining.
   *
   * @see CompositeItemReader#setReader(AbstractItemStreamItemReader)
   */
  public CompositeItemReaderBuilder<I, O>  reader(AbstractItemStreamItemReader<I> reader) {
    this.reader = reader;

    return this;
  }

  /**
   * The list of item fetchers to fetch additional values.
   *
   * @param fetchers the list of fetchers to use. The fetchers list must not be null
   * nor be empty.
   * @return this instance for method chaining.
   *
   * @see CompositeItemReader#setFetchers(List)
   */
  public CompositeItemReaderBuilder<I, O>  fetchers(List<? extends ItemFetcher<?, ?>> fetchers) {
    this.fetchers = fetchers;

    return this;
  }

  /**
   * The list of item fetchers to fetch additional values.
   *
   * @param fetchers the list of fetchers to use. The fetchers list must not be null
   * nor be empty.
   * @return this instance for method chaining.
   *
   * @see CompositeItemReader#setFetchers(List)
   */
  public CompositeItemReaderBuilder<I, O>  fetchers(ItemFetcher<?, ?>... fetchers) {
    return fetchers(Arrays.asList(fetchers));
  }

  /**
   * Returns a fully constructed {@link CompositeItemReader}.
   *
   * @return a new {@link CompositeItemReader}
   */
  public CompositeItemReader<I, O> build() {
    Assert.notNull(reader, "A reader is required.");

    Assert.notNull(fetchers, "A list of fetchers is required.");
    Assert.notEmpty(fetchers, "The fetchers list must have one or more fetcher.");

    CompositeItemReader<I, O> compositeItemReader = new CompositeItemReader<>();
    compositeItemReader.setReader(reader);
    compositeItemReader.setFetchers(fetchers);
    return compositeItemReader;
  }
}
