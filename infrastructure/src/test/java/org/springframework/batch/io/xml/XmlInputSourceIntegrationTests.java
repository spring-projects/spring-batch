/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.io.xml;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.io.sample.domain.LineItem;
import org.springframework.batch.io.sample.domain.Order;
import org.springframework.batch.io.sample.domain.Shipper;
import org.springframework.batch.io.xml.xstream.FieldAlias;
import org.springframework.batch.io.xml.xstream.Mapping;
import org.springframework.batch.io.xml.xstream.XStreamConfiguration;
import org.springframework.batch.io.xml.xstream.XStreamFactory;
import org.springframework.batch.restart.RestartData;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.ClassUtils;

/**
 * Integration test for XmlInputTemplate. It tests reading, xml validation, skip
 * and restart functionality.
 * @author peter.zozom
 */
public class XmlInputSourceIntegrationTests extends TestCase {

	private final static String INPUT_NAME = "xmlInputTemplate";

	private XmlInputSource xmlInput;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	/**
	 * Set up XmlInputTemplate: create mock for FileLocator and create
	 * XStreamConfiguration object.
	 * @throws Exception 
	 */
	public void setUp() throws Exception {

		// create mock for file locator
		Resource resource = new ClassPathResource(ClassUtils.addResourcePathToPackagePath(getClass(),
				"20070125.testStream.xmlFileStep.xml"));

		// Set up XStreamCfg:
		XStreamConfiguration streamConfiguration = new XStreamConfiguration();

		// Step 1: set field aliases
		List aliases = new ArrayList();

		FieldAlias alias = new FieldAlias();
		alias.setAliasName("org.springframework.batch.io.sample.domain.Customer");
		alias.setType("org.springframework.batch.io.sample.domain.Order");
		alias.setFieldName("customer");
		aliases.add(alias);

		alias = new FieldAlias();
		alias.setAliasName("org.springframework.batch.io.sample.domain.Shipper");
		alias.setType("org.springframework.batch.io.sample.domain.Order");
		alias.setFieldName("shipper");
		aliases.add(alias);

		streamConfiguration.setFieldAliases(aliases);

		// Step 2: set mappings
		List mappings = new ArrayList();

		Mapping mapping = new Mapping();
		mapping.setClassName("org.springframework.batch.io.sample.domain.Order");
		mapping.setNamespaceURI("http://adsj.accenture.com/purchaseorders");
		mapping.setLocalPart("order");
		mapping.setPrefix("");
		mappings.add(mapping);

		mapping = new Mapping();
		mapping.setClassName("org.springframework.batch.io.sample.domain.Customer");
		mapping.setNamespaceURI("http://adsj.accenture.com/purchaseorders");
		mapping.setLocalPart("customer");
		mapping.setPrefix("");
		mappings.add(mapping);

		mapping = new Mapping();
		mapping.setClassName("org.springframework.batch.io.sample.domain.LineItem");
		mapping.setNamespaceURI("http://adsj.accenture.com/purchaseorders");
		mapping.setLocalPart("lineItem");
		mapping.setPrefix("");
		mappings.add(mapping);

		mapping = new Mapping();
		mapping.setClassName("org.springframework.batch.io.sample.domain.Shipper");
		mapping.setNamespaceURI("http://adsj.accenture.com/purchaseorders");
		mapping.setLocalPart("shipper");
		mapping.setPrefix("");
		mappings.add(mapping);

		streamConfiguration.setMappings(mappings);

		// Set up input template
		xmlInput = new XmlInputSource() {
			public void registerSynchronization() {
			}
		};

		xmlInput.setResource(resource);
		xmlInput.setEncoding("UTF-8");
		xmlInput.setName(INPUT_NAME);
		XStreamFactory factory = new XStreamFactory();
		factory.setConfig(streamConfiguration);
		xmlInput.setInputFactory(factory);
	}

	public void tearDown() {
	}

	/**
	 * Test read functionality.
	 * @throws ParseException
	 */
	public void testRead() throws ParseException {

		xmlInput.setValidating(false);
		xmlInput.open();

		// READ FIRST RECORD
		Object result = xmlInput.read();

		// is it Order?
		assertTrue(result instanceof Order);
		Order order = (Order) result;
		// verify customer
		assertNotNull(order.getCustomer());
		assertEquals("Gladys Kravitz", order.getCustomer().getName());
		assertEquals("Anytown, PA", order.getCustomer().getAddress());
		assertEquals(34, order.getCustomer().getAge());
		assertEquals(0, order.getCustomer().getMoo());
		assertEquals(0, order.getCustomer().getPoo());
		// verify date
		assertEquals(sdf.parse("2003-01-07 14:16:00 GMT"), order.getDate());
		// verify line items
		List items = order.getLineItems();
		assertEquals(2, items.size());
		LineItem item = (LineItem) items.get(0);
		assertEquals("Burnham's Celestial Handbook, Vol 1", item.getDescription());
		assertEquals(5.0, item.getPerUnitOunces(), 0.0);
		assertEquals(21.79, item.getPrice(), 0.0);
		assertEquals(2, item.getQuantity());
		item = (LineItem) items.get(1);
		assertEquals("Burnham's Celestial Handbook, Vol 2", item.getDescription());
		assertEquals(5.0, item.getPerUnitOunces(), 0.0);
		assertEquals(19.89, item.getPrice(), 0.0);
		assertEquals(2, item.getQuantity());
		// verify shipper
		Shipper shipper = order.getShipper();
		assertEquals("ZipShip", shipper.getName());
		assertEquals(0.74, shipper.getPerOunceRate(), 0.0);

		// READ SECOND RECORD
		result = xmlInput.read();

		// is it Order?
		assertTrue(result instanceof Order);
		order = (Order) result;
		// verify customer
		assertNotNull(order.getCustomer());
		assertEquals("John Smith", order.getCustomer().getName());
		assertEquals("Chicago, IL", order.getCustomer().getAddress());
		assertEquals(46, order.getCustomer().getAge());
		assertEquals(0, order.getCustomer().getMoo());
		assertEquals(0, order.getCustomer().getPoo());
		// verify date
		assertEquals(sdf.parse("2003-01-07 14:16:02 GMT"), order.getDate());
		// verify line items
		items = order.getLineItems();
		assertEquals(3, items.size());
		item = (LineItem) items.get(0);
		assertEquals("XmlBeans in Action", item.getDescription());
		assertEquals(3.0, item.getPerUnitOunces(), 0.0);
		assertEquals(41.29, item.getPrice(), 0.0);
		assertEquals(1, item.getQuantity());
		item = (LineItem) items.get(1);
		assertEquals("JSR-173", item.getDescription());
		assertEquals(1.0, item.getPerUnitOunces(), 0.0);
		assertEquals(11.99, item.getPrice(), 0.0);
		assertEquals(5, item.getQuantity());
		item = (LineItem) items.get(2);
		assertEquals("Teach Yourself XML in 21 days", item.getDescription());
		assertEquals(1.0, item.getPerUnitOunces(), 0.0);
		assertEquals(35.49, item.getPrice(), 0.0);
		assertEquals(1, item.getQuantity());
		// verify shipper
		shipper = order.getShipper();
		assertEquals("ZipShip", shipper.getName());
		assertEquals(0.74, shipper.getPerOunceRate(), 0.0);

		// READ LAST RECORD
		result = xmlInput.read();

		// is it Order?
		assertTrue(result instanceof Order);
		order = (Order) result;
		// verify customer
		assertNotNull(order.getCustomer());
		assertEquals("Peter Newman", order.getCustomer().getName());
		assertEquals("Cleveland, OH", order.getCustomer().getAddress());
		assertEquals(23, order.getCustomer().getAge());
		assertEquals(0, order.getCustomer().getMoo());
		assertEquals(0, order.getCustomer().getPoo());
		// verify date
		assertEquals(sdf.parse("2003-01-07 14:16:35 GMT"), order.getDate());
		// verify line items
		items = order.getLineItems();
		assertEquals(1, items.size());
		item = (LineItem) items.get(0);
		assertEquals("Java 6", item.getDescription());
		assertEquals(2.0, item.getPerUnitOunces(), 0.0);
		assertEquals(12.79, item.getPrice(), 0.0);
		assertEquals(3, item.getQuantity());
		// verify shipper
		shipper = order.getShipper();
		assertEquals("UPS", shipper.getName());
		assertEquals(0.69, shipper.getPerOunceRate(), 0.0);

		// all records were processed already
		assertNull(xmlInput.read());

		// verify statistics TODO
		// Map statistics = xmlInput.getStatistics();
		// assertEquals("4",
		// statistics.get(XmlInputTemplate.READ_STATISTICS_NAME));

		xmlInput.close();
	}

	/**
	 * Test XML validation
	 */
	public void testValidation() {

		// turn on xml validation
		xmlInput.setValidating(true);
		// TEST 1: parse valid xml
		xmlInput.open();
		xmlInput.close();

	}
	
	public void testInvalidXml() throws Exception {

		xmlInput.setValidating(true);

		// TEST 2: parse invalid xml
		xmlInput.setResource(new ByteArrayResource("<invalid><xml>".getBytes()));

		try {
			xmlInput.open();
			fail("Parsing invalid xml file. Exception should be thrown.");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(true);
		}
		
	}

	/**
	 * Test skip functioanlity.
	 * @throws ParseException
	 */
	public void testSkip() throws ParseException {

		xmlInput.setValidating(false);
		xmlInput.open();

		// read first record
		xmlInput.read();
		// mark it as skipped
		xmlInput.skip();
		// read second record
		xmlInput.read();
		// read third record
		xmlInput.read();
		// mark it as skipped and rollback
		xmlInput.skip();
		xmlInput.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

		// read second record again (first was skipped)
		Object result = xmlInput.read();

		// is it Order?
		assertTrue(result instanceof Order);
		Order order = (Order) result;
		// verify customer
		assertNotNull(order.getCustomer());
		assertEquals("John Smith", order.getCustomer().getName());
		assertEquals("Chicago, IL", order.getCustomer().getAddress());
		assertEquals(46, order.getCustomer().getAge());
		assertEquals(0, order.getCustomer().getMoo());
		assertEquals(0, order.getCustomer().getPoo());
		// verify date
		assertEquals(sdf.parse("2003-01-07 14:16:02 GMT"), order.getDate());
		// verify line items
		List items = order.getLineItems();
		assertEquals(3, items.size());
		LineItem item = (LineItem) items.get(0);
		assertEquals("XmlBeans in Action", item.getDescription());
		assertEquals(3.0, item.getPerUnitOunces(), 0.0);
		assertEquals(41.29, item.getPrice(), 0.0);
		assertEquals(1, item.getQuantity());
		item = (LineItem) items.get(1);
		assertEquals("JSR-173", item.getDescription());
		assertEquals(1.0, item.getPerUnitOunces(), 0.0);
		assertEquals(11.99, item.getPrice(), 0.0);
		assertEquals(5, item.getQuantity());
		item = (LineItem) items.get(2);
		assertEquals("Teach Yourself XML in 21 days", item.getDescription());
		assertEquals(1.0, item.getPerUnitOunces(), 0.0);
		assertEquals(35.49, item.getPrice(), 0.0);
		assertEquals(1, item.getQuantity());
		// verify shipper
		Shipper shipper = order.getShipper();
		assertEquals("ZipShip", shipper.getName());
		assertEquals(0.74, shipper.getPerOunceRate(), 0.0);

		// No records left, third record should be skipped
		assertNull(xmlInput.read());

		// verify statistics TODO
		// Map statistics = xmlInput.getStatistics();
		// assertEquals("4",
		// statistics.get(XmlInputTemplate.READ_STATISTICS_NAME));
	}

	/**
	 * Test restart functionality.
	 * @throws ParseException
	 */
	public void testRestart() throws ParseException {

		xmlInput.open();

		// read first record and commit it
		xmlInput.read();
		xmlInput.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// read second record and commit it
		xmlInput.read();
		xmlInput.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// read third record
		xmlInput.read();
		xmlInput.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		RestartData restartData = xmlInput.getRestartData();

		xmlInput.close();

		xmlInput.open();
		xmlInput.restoreFrom(restartData);
		Object result = xmlInput.read();

		// is it Order?
		assertTrue(result instanceof Order);
		Order order = (Order) result;
		// verify customer
		assertNotNull(order.getCustomer());
		assertEquals("Peter Newman", order.getCustomer().getName());
		assertEquals("Cleveland, OH", order.getCustomer().getAddress());
		assertEquals(23, order.getCustomer().getAge());
		assertEquals(0, order.getCustomer().getMoo());
		assertEquals(0, order.getCustomer().getPoo());
		// verify date
		assertEquals(sdf.parse("2003-01-07 14:16:35 GMT"), order.getDate());
		// verify line items
		List items = order.getLineItems();
		assertEquals(1, items.size());
		LineItem item = (LineItem) items.get(0);
		assertEquals("Java 6", item.getDescription());
		assertEquals(2.0, item.getPerUnitOunces(), 0.0);
		assertEquals(12.79, item.getPrice(), 0.0);
		assertEquals(3, item.getQuantity());
		// verify shipper
		Shipper shipper = order.getShipper();
		assertEquals("UPS", shipper.getName());
		assertEquals(0.69, shipper.getPerOunceRate(), 0.0);

		// all records were processed already
		assertNull(xmlInput.read());

		// verify statistics TODO
		// Map statistics = xmlInput.getStatistics();
		// assertEquals("4",
		// statistics.get(XmlInputTemplate.READ_STATISTICS_NAME));
	}

	/**
	 * Tests null resource
	 * @throws Exception
	 */
	public void testGetFileLocatorStrategyWithNullParam() throws Exception {

		// set file locator strategy to null
		xmlInput.setResource(null);
		try {
			xmlInput.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

}
