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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.sample.domain.CustomerCredit;

/**
 * @author Dave Syer
 *
 */
public class CustomerCreditUpdatePreparedStatementSetterTests extends TestCase {
	
	private CustomerCreditUpdatePreparedStatementSetter setter = new CustomerCreditUpdatePreparedStatementSetter();

	private CustomerCredit credit;

	private PreparedStatement ps;

	private MockControl control = MockControl.createControl(PreparedStatement.class);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		ps = (PreparedStatement) control.getMock();
		credit = new CustomerCredit();
		credit.setId(13);
		credit.setCredit(new BigDecimal(12000));
		credit.setName("foo");
	}
	/**
	 * Test method for {@link org.springframework.batch.sample.item.writer.CustomerCreditUpdatePreparedStatementSetter#setValues(java.lang.Object, java.sql.PreparedStatement)}.
	 * @throws SQLException 
	 */
	public void testSetValues() throws SQLException {
		ps.setBigDecimal(1, credit.getCredit());
		control.setVoidCallable();
		ps.setLong(2, credit.getId());
		control.setVoidCallable();
		control.replay();
		setter.setValues(credit, ps);
		control.verify();
	}

}
