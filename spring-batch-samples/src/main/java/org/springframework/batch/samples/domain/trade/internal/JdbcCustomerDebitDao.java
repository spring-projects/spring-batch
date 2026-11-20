/*
 * Copyright 2006-2012 the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.batch.samples.domain.trade.CustomerDebit;
import org.springframework.batch.samples.domain.trade.CustomerDebitDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Reduces customer's credit by the provided amount.
 *
 * @author Robert Kasanicky
 * @author Yanming Zhou
 */
public class JdbcCustomerDebitDao implements CustomerDebitDao {

	private static final String UPDATE_CREDIT = "UPDATE CUSTOMER SET credit= credit-? WHERE name=?";

	private JdbcClient jdbcClient;

	@Override
	public void write(CustomerDebit customerDebit) {
		jdbcClient.sql(UPDATE_CREDIT).params(customerDebit.getDebit(), customerDebit.getName()).update();
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcClient = JdbcClient.create(dataSource);
	}

}
