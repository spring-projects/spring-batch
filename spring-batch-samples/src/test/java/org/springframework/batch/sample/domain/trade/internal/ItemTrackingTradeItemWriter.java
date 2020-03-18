/*
 * Copyright 2008-2014 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

public class ItemTrackingTradeItemWriter implements ItemWriter<Trade> {
	private List<Trade> items = new ArrayList<>();
	private String writeFailureISIN;
	private JdbcOperations jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setWriteFailureISIN(String writeFailureISIN) {
		this.writeFailureISIN = writeFailureISIN;
	}

	public List<Trade> getItems() {
		return items;
	}

	@Override
	public void write(List<? extends Trade> items) throws Exception {
		List<Trade> newItems = new ArrayList<>();

		for (Trade t : items) {
			if (t.getIsin().equals(this.writeFailureISIN)) {
				throw new IOException("write failed");
			}

			newItems.add(t);

			if (jdbcTemplate != null) {
				jdbcTemplate.update("UPDATE TRADE set VERSION=? where ID=? and version=?", t.getVersion() + 1, t
						.getId(), t.getVersion());
			}
		}

		this.items.addAll(newItems);
	}
}
