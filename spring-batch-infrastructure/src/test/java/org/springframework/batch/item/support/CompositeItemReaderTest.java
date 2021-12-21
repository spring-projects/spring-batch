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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemFetcher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Tests for {@link CompositeItemReader}.
 *
 * @author Wiktor Kęska
 */
class CompositeItemReaderTest {
  private static final String FOOS = "1 \n 2 \n 3 \n 4 \n 5 \n";
  private CompositeItemReader<Foo, Foo> compositeItemReader;

  protected FlatFileItemReader<Foo> getItemReader() throws Exception {
    FlatFileItemReader<Foo> tested = new FlatFileItemReader<>();
    Resource resource = new ByteArrayResource(FOOS.getBytes());
    tested.setResource(resource);
    tested.setLineMapper((line, lineNumber) -> {
      Foo foo = new Foo();
      foo.setId(Integer.parseInt(line.trim()));
      return foo;
    });

    tested.setSaveState(true);
    tested.afterPropertiesSet();
    return tested;
  }

  @BeforeEach
  void setUp() throws Exception {
    compositeItemReader = new CompositeItemReader<>();
    compositeItemReader.setReader(getItemReader());
    compositeItemReader.setFetchers(List.of(new NameFetcher(), new ValueFetcher()));

    compositeItemReader.open(new ExecutionContext());
    compositeItemReader.afterPropertiesSet();
  }

  /**
   * Regular usage scenario - item is passed through the fetchers chain,
   * return value of Foo witch fetched additional values.
   */
  @Test
  void shouldFetchItems() throws Exception {
    var item = compositeItemReader.read();
    assertThat(item).isNotNull();
    assertReadedItem(item, "One", 11);

    item = compositeItemReader.read();
    assertThat(item).isNotNull();
    assertReadedItem(item, "Two", 12);

    item = compositeItemReader.read();
    assertThat(item).isNotNull();
    assertReadedItem(item, "Three", 13);

    item = compositeItemReader.read();
    assertThat(item).isNotNull();
    assertReadedItem(item, "Four", 14);

    item = compositeItemReader.read();
    assertThat(item).isNotNull();
    assertReadedItem(item, "Five", 15);

    item = compositeItemReader.read();

    assertThat(item).isNull();
  }

  /**
   * The list of transformers must not be null or empty and
   * can contain only instances of {@link ItemProcessor}.
   */
  @Test
  public void testAfterPropertiesSet() throws Exception {

    // fetchers not set
    compositeItemReader.setFetchers(null);
    try {
      compositeItemReader.afterPropertiesSet();
      Assertions.fail();
    }
    catch (IllegalArgumentException e) {
      // expected
    }

    // fetchers are empty list
    compositeItemReader.setFetchers(new ArrayList<ItemFetcher<Object,Object>>());
    try {
      compositeItemReader.afterPropertiesSet();
      Assertions.fail();
    }
    catch (IllegalArgumentException e) {
      // expected
    }

    // reader not set
    compositeItemReader.setReader(null);
    try {
      compositeItemReader.afterPropertiesSet();
      Assertions.fail();
    }
    catch (IllegalArgumentException e) {
      // expected
    }

  }

  private void assertReadedItem(Foo item, String expectedName, Integer expectedValue) {
    assertThat(item.getName()).isEqualTo(expectedName);
    assertThat(item.getValue()).isEqualTo(expectedValue);
  }

  private static class NameFetcher implements ItemFetcher<Foo, Foo>{

    private static Map<Integer, String> numbers;

    NameFetcher() {
      numbers = new HashMap<>();
      numbers.put(1, "One");
      numbers.put(2, "Two");
      numbers.put(3, "Three");
      numbers.put(4, "Four");
      numbers.put(5, "Five");
    }

    @Override
    public Foo fetch(Foo item) {
      item.setName(numbers.get(item.getId()));
      return item;
    }
  }

  private static class ValueFetcher implements ItemFetcher<Foo, Foo>{

    private static Map<Integer, Integer> numbers;

    ValueFetcher() {
      numbers = new HashMap<>();
      numbers.put(1, 11);
      numbers.put(2, 12);
      numbers.put(3, 13);
      numbers.put(4, 14);
      numbers.put(5, 15);
    }

    @Override
    public Foo fetch(Foo item) {
      item.setValue(numbers.get(item.getId()));
      return item;
    }
  }
}
