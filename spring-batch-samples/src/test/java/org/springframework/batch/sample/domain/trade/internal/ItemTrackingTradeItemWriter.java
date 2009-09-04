package org.springframework.batch.sample.domain.trade.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class ItemTrackingTradeItemWriter implements ItemWriter<Trade> {

	private List<Trade> items = new ArrayList<Trade>();

	private String writeFailureISIN;

	private SimpleJdbcTemplate jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	public void setWriteFailureISIN(String writeFailureISIN) {
		this.writeFailureISIN = writeFailureISIN;
	}

	public void setItems(List<Trade> items) {
		this.items = items;
	}

	public List<Trade> getItems() {
		return items;
	}

	public void clearItems() {
		this.items.clear();
	}

	public void write(List<? extends Trade> items) throws Exception {
		List<Trade> newItems = new ArrayList<Trade>();
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
