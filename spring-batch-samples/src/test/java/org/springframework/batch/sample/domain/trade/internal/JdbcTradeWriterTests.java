/*
 * Copyright 2006-2008 the original author or authors.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-source-context.xml"})
public class JdbcTradeWriterTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	private JdbcTradeDao writer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
		this.writer = new JdbcTradeDao();
		this.writer.setDataSource(dataSource);

	}

	@Autowired
	public void setIncrementer(AbstractDataFieldMaxValueIncrementer incrementer) {
		incrementer.setIncrementerName("TRADE_SEQ");
		this.writer.setIncrementer(incrementer);
	}

	@Transactional @Test
	public void testWrite() {

		Trade trade = new Trade();
		trade.setCustomer("testCustomer");
		trade.setIsin("5647238492");
		trade.setPrice(new BigDecimal(Double.toString(99.69)));
		trade.setQuantity(5);
		
		writer.writeTrade(trade);
		
		simpleJdbcTemplate.getJdbcOperations().query("SELECT * FROM TRADE WHERE ISIN = '5647238492'", new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				assertEquals("testCustomer", rs.getString("CUSTOMER"));
				assertEquals(new BigDecimal(Double.toString(99.69)), rs.getBigDecimal("PRICE"));
				assertEquals(5,rs.getLong("QUANTITY"));
			}
		});
	}
}
