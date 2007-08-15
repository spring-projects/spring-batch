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

package org.springframework.batch.sample.module;

import java.util.Properties;

import org.springframework.batch.execution.tasklet.ReadProcessTasklet;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.io.file.support.DefaultFlatFileInputSource;
import org.springframework.batch.sample.dao.TradeWriter;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.batch.statistics.StatisticsProvider;

/**
 * Simple implementation of a {@link ReadProcessTasklet}, which illustrates the
 * case when reading and processing of input is not separated. This can be
 * viable in cases, when the input reading and processing logic need not to be
 * reused in different contexts. In general it is recommended to separate these
 * two concerns.
 * 
 * Note this class is NOT thread-safe, contrast to 'standard' module
 * implementations provided by the framework.
 * 
 * @author Robert Kasanicky
 * @author Lucas Ward
 */
public class SimpleTradeTasklet extends ReadProcessTasklet implements StatisticsProvider {
	/**
	 * reads the data from input file
	 */
	private DefaultFlatFileInputSource inputSource;

	/**
	 * maps a line to a Trade object
	 */
	private FieldSetMapper tradeFieldSetMapper = new TradeFieldSetMapper();

	/**
	 * writes a Trade object to output
	 */
	private TradeWriter tradeWriter;

	/**
	 * domain object being processed
	 */
	private Trade trade;

	/**
	 * number of trade objects processed
	 */
	private int tradeCount = 0;

	/**
	 * Read method, all reading from any input source(s) should be done here.
	 * The input template is read using the readAndMap method, which accepts a
	 * FieldSetMapper. This call returns an object (which should be a Trade
	 * value object) then will be stored in a class-level variable for use by
	 * the process method.
	 */
	public boolean read() {
		trade = (Trade) tradeFieldSetMapper.mapLine(inputSource.readFieldSet());

		if (trade == null) {
			// no Trade object returned, reading input is finished
			return false;
		}

		tradeCount++;
		return true;
	}

	/**
	 * Process the data obtained during the read() method. Because this is a
	 * simple example job, the data is simply written out without any
	 * processing.
	 */
	public void process() {
		tradeWriter.writeTrade(trade);
	}

	/**
	 * Inner class which implements the FieldSetMapper interface. It contains
	 * one method, mapLine, which accepts a FieldSet as a parameter. This method
	 * will be called by the inputSource when it is passed in.
	 * 
	 */
	private static class TradeFieldSetMapper implements FieldSetMapper {
		public Object mapLine(FieldSet fieldSet) {

			if (fieldSet == null) {
				return null;
			}

			Trade trade = new Trade();
			trade.setIsin(fieldSet.readString("ISIN"));
			trade.setQuantity(fieldSet.readLong(1));
			trade.setPrice(fieldSet.readBigDecimal(2));
			trade.setCustomer(fieldSet.readString(3));

			return trade;

		}
	}

	public void setInputSource(DefaultFlatFileInputSource inputTemplate) {
		this.inputSource = inputTemplate;
	}

	public void setTradeDao(TradeWriter tradeWriter) {
		this.tradeWriter = tradeWriter;
	}

	public Properties getStatistics() {
		Properties statistics = new Properties();
		statistics.setProperty("Trade.Count", String.valueOf(tradeCount));
		statistics.putAll(inputSource.getStatistics());
		return statistics;
	}

}
