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
package org.springframework.batch.item.support.builder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.batch.item.ItemFetcher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemReader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Wiktor Kęska
 */
public class CompositeItemReaderBuilderTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().silent();

  @Mock
  private ItemFetcher<Object, Object> fetcher1;

  @Mock
  private ItemFetcher<Object, Object> fetcher2;

  @Mock
  private AbstractItemStreamItemReader<Object> reader;

  private List<ItemFetcher<Object, Object>> fetchers;

  @Before
  public void setup() {
    this.fetchers = new ArrayList<>();
    this.fetchers.add(fetcher1);
    this.fetchers.add(fetcher2);
  }


  @Test
  public void testFetch() throws Exception {
    Object item = new Object();
    Object itemAfterFirstFetch = new Object();
    Object itemAfterSecondFetch = new Object();
    CompositeItemReader<Object, Object> composite = new CompositeItemReaderBuilder<>()
        .reader(this.reader)
        .fetchers(this.fetcher1, this.fetcher2)
        .build();

    when(reader.read()).thenReturn(item);
    when(fetcher1.fetch(item)).thenReturn(itemAfterFirstFetch);
    when(fetcher2.fetch(itemAfterFirstFetch)).thenReturn(itemAfterSecondFetch);

    assertSame(itemAfterSecondFetch, composite.read());
  }

  @Test
  public void testFetchVarargs() throws Exception {
    Object item = new Object();
    Object itemAfterFirstFetch = new Object();
    Object itemAfterSecondFetch = new Object();
    CompositeItemReader<Object, Object> composite = new CompositeItemReaderBuilder<>()
        .reader(this.reader)
        .fetchers(fetchers)
        .build();

    when(reader.read()).thenReturn(item);
    when(fetcher1.fetch(item)).thenReturn(itemAfterFirstFetch);
    when(fetcher2.fetch(itemAfterFirstFetch)).thenReturn(itemAfterSecondFetch);

    assertSame(itemAfterSecondFetch, composite.read());
  }

  @Test
  public void testNullOrEmptyFetchers() throws Exception {
    var compositeReader = new CompositeItemReaderBuilder<>();
    validateExceptionMessage(compositeReader,"A reader is required.");
    compositeReader.reader(reader);
    validateExceptionMessage(compositeReader,"A list of fetchers is required.");
    compositeReader.fetchers(List.of());
    validateExceptionMessage(compositeReader,"The fetchers list must have one or more fetcher.");
  }

  private void validateExceptionMessage(CompositeItemReaderBuilder<?, ?> builder, String message) {
    try {
      builder.build();
      fail("IllegalArgumentException should have been thrown");
    }
    catch (IllegalArgumentException iae) {
      assertEquals("IllegalArgumentException message did not match the expected result.", message,
          iae.getMessage());
    }
  }
}