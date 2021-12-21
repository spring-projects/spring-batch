/*
 * Copyright 2021-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.support;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Composite {@link ItemReader} that passes the item through a sequence of
 * injected <code>ItemFetcher</code>s (return value of previous
 * fetcher is the entry value of the next).<br>
 * <br>
 *
 * Note the user is responsible for injecting a chain of {@link ItemFetcher}s
 * that conforms to declared input and output types.
 *
 * @author Wiktor Kęska
 */
public class CompositeItemReader<I, O> extends AbstractItemStreamItemReader<O>
    implements InitializingBean {

  private AbstractItemStreamItemReader<I> reader;
  private List<? extends ItemFetcher<?, ?>> itemFetchers;

  @Override
  public O read() throws Exception {
    Object item = reader.read();

    for (ItemFetcher<?, ?> fetcher : itemFetchers) {
      if (item == null) {
        return null;
      }

      item = fetchItem(fetcher, item);
    }
    return (O) item;
  }

  private <T> Object fetchItem(ItemFetcher<T, ?> fetcher, Object input) throws Exception {
    return fetcher.fetch((T) input);
  }

  @Override
  public void close() {
    reader.close();
  }

  @Override
  public void open(ExecutionContext executionContext) {
    reader.open(executionContext);
  }

  @Override
  public void update(ExecutionContext executionContext) {
    reader.update(executionContext);
  }

  /**
   * Establishes the {@link AbstractItemStreamItemReader<I>} reader that will read base items.
   * @param reader {@link AbstractItemStreamItemReader<I>} reader that will read base items.
   */
  public void setReader(AbstractItemStreamItemReader<I> reader) {
    this.reader = reader;
  }

  /**
   * Establishes the {@link ItemFetcher} fecthers that will fetch additional value
   * for reader result.
   * @param fetchers list of {@link ItemFetcher} fetchers that will fetch value for item.
   */
  public void setFetchers(List<? extends ItemFetcher<?, ?>> fetchers) {
    this.itemFetchers = fetchers;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(reader, "The 'reader' may not be null");

    Assert.notNull(itemFetchers, "The 'itemFetchers' may not be null");
    Assert.notEmpty(itemFetchers, "The 'itemFetchers' may not be empty");
  }
}
