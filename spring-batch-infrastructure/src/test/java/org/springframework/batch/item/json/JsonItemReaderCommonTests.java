/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.json;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;

/**
 * @author Mahmoud Ben Hassine
 */
public abstract class JsonItemReaderCommonTests extends AbstractItemStreamItemReaderTests {

	private static final String FOOS = "[" +
			"  {\"value\":1}," +
			"  {\"value\":2}," +
			"  {\"value\":3}," +
			"  {\"value\":4}," +
			"  {\"value\":5}" +
			"]";

	protected abstract JsonObjectReader<Foo> getJsonObjectReader();

	@Override
	protected ItemReader<Foo> getItemReader() {
		ByteArrayResource resource = new ByteArrayResource(FOOS.getBytes());
		JsonObjectReader<Foo> jsonObjectReader = getJsonObjectReader();
		JsonItemReader<Foo> itemReader = new JsonItemReader<>(resource, jsonObjectReader);
		itemReader.setName("fooJsonItemReader");
		return itemReader;
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) {
		JsonItemReader<Foo> reader = (JsonItemReader<Foo>) tested;
		reader.setResource(new ByteArrayResource("[]".getBytes()));

		reader.open(new ExecutionContext());
	}

}
