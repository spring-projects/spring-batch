/*
 * Copyright 2006-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-source-context.xml"})
public class JdbcTradeWriterTests implements InitializingBean {
	private JdbcOperations jdbcTemplate;
	private JdbcTradeDao writer;
	private AbstractDataFieldMaxValueIncrementer incrementer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.writer = new JdbcTradeDao();
		this.writer.setDataSource(dataSource);
	}

	@Autowired
	public void setIncrementer(@Qualifier("incrementerParent") AbstractDataFieldMaxValueIncrementer incrementer) {
		incrementer.setIncrementerName("TRADE_SEQ");
		this.incrementer = incrementer;
	}

	@Test
	@Transactional
	public void testWrite() {
		Trade trade = new Trade();
		trade.setCustomer("testCustomer");
		trade.setIsin("5647238492");
		trade.setPrice(new BigDecimal("99.69"));
		trade.setQuantity(5);

		writer.writeTrade(trade);

		jdbcTemplate.query("SELECT * FROM TRADE WHERE ISIN = '5647238492'", new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				assertEquals("testCustomer", rs.getString("CUSTOMER"));
				assertEquals(new BigDecimal(Double.toString(99.69)), rs.getBigDecimal("PRICE"));
				assertEquals(5,rs.getLong("QUANTITY"));
			}
		});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.writer.setIncrementer(incrementer);
	}
}
