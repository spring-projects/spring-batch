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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.TradeDao;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import javax.sql.DataSource;


/**
 * Writes a Trade object to a database
 *
 * @author Robert Kasanicky
 */
public class JdbcTradeDao implements TradeDao {
	private Log log = LogFactory.getLog(JdbcTradeDao.class);
    /**
     * template for inserting a row
     */
    private static final String INSERT_TRADE_RECORD = "INSERT INTO trade (id, isin, quantity, price, customer) VALUES (?, ?, ? ,?, ?)";

    /**
     * handles the processing of sql query
     */
    private SimpleJdbcTemplate simpleJdbcTemplate;

    /**
     * database is not expected to be setup for autoincrementation
     */
    private DataFieldMaxValueIncrementer incrementer;

    /**
     * @see TradeDao
     */
    public void writeTrade(Trade trade) {
        Long id = incrementer.nextLongValue();
        log.debug("Processing: " + trade);
        simpleJdbcTemplate.update(INSERT_TRADE_RECORD,
				id, trade.getIsin(), trade.getQuantity(), trade.getPrice(),
				trade.getCustomer());
    }

    public void setDataSource(DataSource dataSource) {
        this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    public void setIncrementer(DataFieldMaxValueIncrementer incrementer) {
        this.incrementer = incrementer;
    }

}
