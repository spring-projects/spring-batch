/*
 * Copyright 2018-2019 the original author or authors.
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

import java.math.BigDecimal;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.batch.item.json.domain.Trade;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Mahmoud Ben Hassine
 */
public abstract class JsonItemReaderFunctionalTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	protected abstract JsonObjectReader<Trade> getJsonObjectReader();

	protected abstract Class<? extends Exception> getJsonParsingException();

	@Test
	public void testJsonReading() throws Exception {
		JsonItemReader<Trade> itemReader = new JsonItemReaderBuilder<Trade>()
				.jsonObjectReader(getJsonObjectReader())
				.resource(new ClassPathResource("org/springframework/batch/item/json/trades.json"))
				.name("tradeJsonItemReader")
				.build();

		itemReader.open(new ExecutionContext());

		Trade trade = itemReader.read();
		Assert.assertNotNull(trade);
		Assert.assertEquals("123", trade.getIsin());
		Assert.assertEquals("foo", trade.getCustomer());
		Assert.assertEquals(new BigDecimal("1.2"), trade.getPrice());
		Assert.assertEquals(1, trade.getQuantity());

		trade = itemReader.read();
		Assert.assertNotNull(trade);
		Assert.assertEquals("456", trade.getIsin());
		Assert.assertEquals("bar", trade.getCustomer());
		Assert.assertEquals(new BigDecimal("1.4"), trade.getPrice());
		Assert.assertEquals(2, trade.getQuantity());

		trade = itemReader.read();
		Assert.assertNotNull(trade);
		Assert.assertEquals("789", trade.getIsin());
		Assert.assertEquals("foobar", trade.getCustomer());
		Assert.assertEquals(new BigDecimal("1.6"), trade.getPrice());
		Assert.assertEquals(3, trade.getQuantity());

		trade = itemReader.read();
		Assert.assertNotNull(trade);
		Assert.assertEquals("100", trade.getIsin());
		Assert.assertEquals("barfoo", trade.getCustomer());
		Assert.assertEquals(new BigDecimal("1.8"), trade.getPrice());
		Assert.assertEquals(4, trade.getQuantity());

		trade = itemReader.read();
		Assert.assertNull(trade);
	}

	@Test
	public void testEmptyResource() throws Exception {
		JsonItemReader<Trade> itemReader = new JsonItemReaderBuilder<Trade>()
				.jsonObjectReader(getJsonObjectReader())
				.resource(new ByteArrayResource("[]".getBytes()))
				.name("tradeJsonItemReader")
				.build();

		itemReader.open(new ExecutionContext());

		Trade trade = itemReader.read();
		Assert.assertNull(trade);
	}

	@Test
	public void testInvalidResourceFormat() {
		this.expectedException.expect(ItemStreamException.class);
		this.expectedException.expectMessage("Failed to initialize the reader");
		this.expectedException.expectCause(instanceOf(IllegalStateException.class));
		JsonItemReader<Trade> itemReader = new JsonItemReaderBuilder<Trade>()
				.jsonObjectReader(getJsonObjectReader())
				.resource(new ByteArrayResource("{}, {}".getBytes()))
				.name("tradeJsonItemReader")
				.build();

		itemReader.open(new ExecutionContext());
	}

	@Test
	public void testInvalidResourceContent() throws Exception {
		this.expectedException.expect(ParseException.class);
		this.expectedException.expectCause(Matchers.instanceOf(getJsonParsingException()));
		JsonItemReader<Trade> itemReader = new JsonItemReaderBuilder<Trade>()
				.jsonObjectReader(getJsonObjectReader())
				.resource(new ByteArrayResource("[{]".getBytes()))
				.name("tradeJsonItemReader")
				.build();
		itemReader.open(new ExecutionContext());

		itemReader.read();
	}
}
