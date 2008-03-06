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
package org.springframework.batch.sample.item.writer;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.CustomerCredit;

/**
 * @author Dave Syer
 * 
 */
public class BatchSqlCustomerCreditIncreaseWriterTests extends TestCase {

	private BatchSqlCustomerCreditIncreaseWriter writer = new BatchSqlCustomerCreditIncreaseWriter();

	private ItemWriter delegate;

	private MockControl control = MockControl.createControl(ItemWriter.class);

	private CustomerCredit customerCredit;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		delegate = (ItemWriter) control.getMock();
		writer.setDelegate(delegate);
		customerCredit = new CustomerCredit();
		customerCredit.setId(13);
		customerCredit.setCredit(new BigDecimal(1000));
		customerCredit.setName("foo");
	}
	
	public void testAfterPropertiesSet() throws Exception {
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			// expected: wrong class
			String message = e.getMessage();
			assertTrue("Message does not contain 'instance'"+message, message.indexOf("instance")>=0);
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.item.writer.BatchSqlCustomerCreditIncreaseWriter#write(java.lang.Object)}.
	 * @throws Exception
	 */
	public void testWrite() throws Exception {
		delegate.write(customerCredit);
		control.setVoidCallable();
		control.replay();
		writer.write(customerCredit);
		control.verify();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.item.writer.BatchSqlCustomerCreditIncreaseWriter#clear()}.
	 */
	public void testClear() {
		delegate.clear();
		control.setVoidCallable();
		control.replay();
		writer.clear();
		control.verify();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.item.writer.BatchSqlCustomerCreditIncreaseWriter#flush()}.
	 */
	public void testFlush() {
		delegate.flush();
		control.setVoidCallable();
		control.replay();
		writer.flush();
		control.verify();
	}

}
