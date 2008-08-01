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
package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.trade.CustomerCredit;

/**
 * @author Dave Syer
 *
 */
public class CustomerCreditUpdatePreparedStatementSetterTests {
	
	private CustomerCreditUpdatePreparedStatementSetter setter = new CustomerCreditUpdatePreparedStatementSetter();

	private CustomerCredit credit;

	private PreparedStatement ps;

	@Before
	public void setUp() throws Exception {
		ps = EasyMock.createMock(PreparedStatement.class);
		credit = new CustomerCredit();
		credit.setId(13);
		credit.setCredit(new BigDecimal(12000));
		credit.setName("foo");
	}
	
	/*
	 * Test method for {@link org.springframework.batch.sample.domain.trade.internal.CustomerCreditUpdatePreparedStatementSetter#setValues(CustomerCredit, PreparedStatement) }
	 */
	@Test
	public void testSetValues() throws SQLException {
		ps.setBigDecimal(1, credit.getCredit().add(CustomerCreditUpdatePreparedStatementSetter.FIXED_AMOUNT));
		EasyMock.expectLastCall();
		ps.setLong(2, credit.getId());
		EasyMock.expectLastCall();
		EasyMock.replay(ps);
		setter.setValues(credit, ps);
		EasyMock.verify(ps);
	}

}
