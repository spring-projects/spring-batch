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
package org.springframework.batch.item;

import com.mongodb.lang.Nullable;
import org.springframework.batch.item.support.CompositeItemReader;
import org.springframework.lang.NonNull;

/**
 * Interface for item fetching. Given an item as input, this interface provides
 * an extension point which allows reader to fetch additional data.
 * It should be noted that while it's possible to return different type than the one provided,
 * it's not strictly necessary. Furthermore,
 * returning {@code null} indicates that the item should not be continued to be processed.
 *
 * @author Wiktor Kęska
 *
 * @param <I> type of input item
 * @param <O> type of output item
 */
public interface ItemFetcher<I, O> {

  /**
   * Fetch data for provided item, returning a potentially modified or new item for continued
   * fetching. If the returned result is {@code null}, it is assumed that fetching of the item
   * should not continue.
   *
   * A {@code null} item will never reach this method because the only possible sources are:
   * <ul>
   *     <li>an {@link CompositeItemReader} (which indicates no more items)</li>
   *     <li>a previous {@link ItemFetcher} in a composite reader (which indicates a filtered item)</li>
   * </ul>
   *
   * @param item for with to fetch, never {@code null}.
   * @return potentially modified or new item for continued fetching, {@code null} if fetching of the
   *  provided item should not continue.
   * @throws Exception thrown if exception occurs during fetching.
   */
  @Nullable
  O fetch(@NonNull I item) throws Exception;
}
