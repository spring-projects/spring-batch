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

package org.springframework.batch.sample.tasklet;

import java.util.Properties;

import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.tasklet.ItemOrientedTasklet;
import org.springframework.batch.io.file.DefaultFlatFileItemReader;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.sample.dao.TradeDao;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.batch.statistics.StatisticsProvider;

/**
 * Simple implementation of a {@link Tasklet}, which illustrates the reading
 * and processing of input data. This can be viable in cases, when the input
 * reading and processing logic need not to be reused in different contexts. In
 * general it is recommended to separate these two concerns using an
 * {@link ItemOrientedTasklet}.
 *
 * Note this class is thread-safe, as per the 'standard' module implementations
 * provided by the framework.
 *
 * @author Robert Kasanicky
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SimpleTradeTasklet implements Tasklet, StatisticsProvider {

	/*
	 * reads the data from input file
	 */
	private DefaultFlatFileItemReader inputSource;

	/*
	 * writes a Trade object to output
	 */
	private TradeDao tradeDao;

	/**
	 * number of trade objects processed
	 */
	private int tradeCount = 0;

	/**
	 * The input template is read using the readAndMap method, which accepts a
	 * FieldSetMapper. This call returns a Trade object, which is then
	 * processed. Because this is a simple example job, the data is simply
	 * written out without any processing.
	 */
	public ExitStatus execute() throws Exception {
		Trade trade = (Trade)inputSource.read();

		if (trade == null) {
			// no Trade object returned, reading input is finished
			return ExitStatus.FINISHED;
		}

		tradeCount++;
		tradeDao.writeTrade(trade);
		return ExitStatus.CONTINUABLE;
	}

	public void setItemReader(DefaultFlatFileItemReader inputTemplate) {
		this.inputSource = inputTemplate;
	}

	public void setTradeDao(TradeDao tradeDao) {
		this.tradeDao = tradeDao;
	}

	public Properties getStatistics() {
		Properties statistics = new Properties();
		statistics.setProperty("trade.count", String.valueOf(tradeCount));
		statistics.putAll(inputSource.getStatistics());
		return statistics;
	}

}
