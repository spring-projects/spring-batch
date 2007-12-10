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

package org.springframework.batch.sample.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;


/**
 * Writes a Trade object to a database
 *
 * @author Robert Kasanicky
 */
public class JdbcTradeWriter implements TradeWriter {
	private Log log = LogFactory.getLog(JdbcTradeWriter.class);
    /**
     * template for inserting a row
     */
    private static final String INSERT_TRADE_RECORD = "INSERT INTO trade (id, isin, quantity, price, customer) VALUES (?, ?, ? ,?, ?)";

    /**
     * handles the processing of sql query
     */
    private JdbcOperations jdbcTemplate;

    /**
     * database is not expected to be setup for autoincrementation
     */
    private DataFieldMaxValueIncrementer incrementer;

    /**
     * @see TradeWriter
     */
    public void writeTrade(Trade trade) {
        Long id = new Long(incrementer.nextLongValue());
        log.debug("Processing: " + trade);
        jdbcTemplate.update(INSERT_TRADE_RECORD,
            new Object[] {
                id, trade.getIsin(), new Long(trade.getQuantity()), trade.getPrice(),
                trade.getCustomer()
            });
    }

    public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setIncrementer(DataFieldMaxValueIncrementer incrementer) {
        this.incrementer = incrementer;
    }

	public void write(Object output) {
		this.writeTrade((Trade)output);
	}

}
