/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.samples.domain.trade.internal;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.batch.infrastructure.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.samples.domain.trade.CustomerCredit;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class CustomerCreditUpdatePreparedStatementSetter implements ItemPreparedStatementSetter<CustomerCredit> {

	public static final BigDecimal FIXED_AMOUNT = new BigDecimal(1000);

	@Override
	public void setValues(CustomerCredit customerCredit, PreparedStatement ps) throws SQLException {
		ps.setBigDecimal(1, customerCredit.getCredit().add(FIXED_AMOUNT));
		ps.setLong(2, customerCredit.getId());
	}

}
