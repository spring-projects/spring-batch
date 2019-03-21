/*
 * Copyright 2010-2011 the original author or authors.
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
package org.springframework.batch.item.xml;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.ClassUtils;

public abstract class AbstractStaxEventReaderItemReaderTests {

	protected StaxEventItemReader<Trade> reader = new StaxEventItemReader<>();

	@Before
	public void setUp() throws Exception {
		reader.setFragmentRootElementName("trade");
		reader.setUnmarshaller(getUnmarshaller());
		reader.afterPropertiesSet();
	}

	@Test
	public void testRead() throws Exception {
		reader.setResource(new ClassPathResource(ClassUtils.addResourcePathToPackagePath(getClass(), "input.xml")));
		reader.open(new ExecutionContext());
		Trade result;
		List<Trade> results = new ArrayList<>();
		while ((result = reader.read()) != null) {
			results.add(result);
		}
		checkResults(results);
	}

	@Test
	public void testReadNested() throws Exception {
		reader.setResource(new ClassPathResource(ClassUtils
				.addResourcePathToPackagePath(getClass(), "input-nested.xml")));
		reader.open(new ExecutionContext());
		Trade result;
		List<Trade> results = new ArrayList<>();
		while ((result = reader.read()) != null) {
			results.add(result);
		}
		checkResults(results);
	}

	/**
	 * @return Unmarshaller specific to the OXM library used
	 */
	protected abstract Unmarshaller getUnmarshaller() throws Exception;

	/**
	 * @param results list of domain objects returned by input source
	 */
	protected void checkResults(List<Trade> results) {
		assertEquals(3, results.size());

		Trade trade1 = results.get(0);
		assertEquals("XYZ0001", trade1.getIsin());
		assertEquals(5, trade1.getQuantity());
		assertEquals(new BigDecimal("11.39"), trade1.getPrice());
		assertEquals("Customer1", trade1.getCustomer());

		Trade trade2 = results.get(1);
		assertEquals("XYZ0002", trade2.getIsin());
		assertEquals(2, trade2.getQuantity());
		assertEquals(new BigDecimal("72.99"), trade2.getPrice());
		assertEquals("Customer2", trade2.getCustomer());

		Trade trade3 = results.get(2);
		assertEquals("XYZ0003", trade3.getIsin());
		assertEquals(9, trade3.getQuantity());
		assertEquals(new BigDecimal("99.99"), trade3.getPrice());
		assertEquals("Customer3", trade3.getCustomer());
	}

	@After
	public void tearDown() throws Exception {
		reader.close();
	}

}
