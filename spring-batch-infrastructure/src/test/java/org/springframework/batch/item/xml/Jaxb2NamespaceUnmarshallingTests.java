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

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.stream.StreamSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.domain.QualifiedTrade;
import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.ClassUtils;

public class Jaxb2NamespaceUnmarshallingTests {

	private StaxEventItemReader<QualifiedTrade> reader = new StaxEventItemReader<>();

	private Resource resource = new ClassPathResource(
			ClassUtils.addResourcePathToPackagePath(getClass(), "domain/trades.xml"));

	@Before
	public void setUp() throws Exception {
		reader.setResource(resource);
		reader.setFragmentRootElementName("{urn:org.springframework.batch.io.oxm.domain}trade");
		reader.setUnmarshaller(getUnmarshaller());
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
	}

	@Test
	public void testUnmarshal() throws Exception {
		QualifiedTrade trade = (QualifiedTrade) getUnmarshaller()
				.unmarshal(new StreamSource(new StringReader(TRADE_XML)));
		Assert.assertEquals("XYZ0001", trade.getIsin());
		Assert.assertEquals(5, trade.getQuantity());
		Assert.assertEquals(new BigDecimal("11.39"), trade.getPrice());
		Assert.assertEquals("Customer1", trade.getCustomer());
	}

	@Test
	public void testRead() throws Exception {
		QualifiedTrade result;
		List<QualifiedTrade> results = new ArrayList<>();
		while ((result = reader.read()) != null) {
			results.add(result);
		}
		checkResults(results);

	}

	protected Unmarshaller getUnmarshaller() throws Exception {

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(new Class<?>[] { QualifiedTrade.class });
		marshaller.setSchema(new ClassPathResource("trade.xsd", Trade.class));
		marshaller.afterPropertiesSet();

		return marshaller;
	}

	/**
	 * @param results list of domain objects returned by input source
	 */
	protected void checkResults(List<QualifiedTrade> results) {
		assertEquals(3, results.size());

		QualifiedTrade trade1 = results.get(0);
		Assert.assertEquals("XYZ0001", trade1.getIsin());
		Assert.assertEquals(5, trade1.getQuantity());
		Assert.assertEquals(new BigDecimal("11.39"), trade1.getPrice());
		Assert.assertEquals("Customer1", trade1.getCustomer());

		QualifiedTrade trade2 = results.get(1);
		Assert.assertEquals("XYZ0002", trade2.getIsin());
		Assert.assertEquals(2, trade2.getQuantity());
		Assert.assertEquals(new BigDecimal("72.99"), trade2.getPrice());
		Assert.assertEquals("Customer2", trade2.getCustomer());

		QualifiedTrade trade3 = results.get(2);
		Assert.assertEquals("XYZ0003", trade3.getIsin());
		Assert.assertEquals(9, trade3.getQuantity());
		Assert.assertEquals(new BigDecimal("99.99"), trade3.getPrice());
		Assert.assertEquals("Customer3", trade3.getCustomer());
	}

	@After
	public void tearDown() throws Exception {
		reader.close();
	}

	private static String TRADE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><trade xmlns=\"urn:org.springframework.batch.io.oxm.domain\">"
			+ "<customer>Customer1</customer><isin>XYZ0001</isin><price>11.39</price><quantity>5</quantity>"
			+ "</trade>";

}
