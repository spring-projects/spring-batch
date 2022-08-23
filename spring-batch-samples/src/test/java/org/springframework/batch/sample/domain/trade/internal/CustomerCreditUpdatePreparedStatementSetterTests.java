/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.sample.domain.trade.internal;

import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.sample.domain.trade.CustomerCredit;

/**
 * @author Dave Syer
 * @author Glenn Renfro
 *
 */
class CustomerCreditUpdatePreparedStatementSetterTests {

	private final CustomerCreditUpdatePreparedStatementSetter setter = new CustomerCreditUpdatePreparedStatementSetter();

	private CustomerCredit credit;

	private PreparedStatement ps;

	@BeforeEach
	void setUp() {
		ps = mock(PreparedStatement.class);
		credit = new CustomerCredit();
		credit.setId(13);
		credit.setCredit(new BigDecimal(12000));
		credit.setName("foo");
	}

	@Test
	void testSetValues() throws SQLException {
		ps.setBigDecimal(1, credit.getCredit().add(CustomerCreditUpdatePreparedStatementSetter.FIXED_AMOUNT));
		ps.setLong(2, credit.getId());
		setter.setValues(credit, ps);
	}

}
