package org.springframework.batch.sample.domain.trade.internal;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.Trade;

public class ItemTrackingTradeItemWriter implements ItemWriter<Trade> {
	private List<Trade> items = new ArrayList<Trade>();
	private String writeFailureISIN;

	public void setWriteFailureISIN(String writeFailureISIN) {
		this.writeFailureISIN = writeFailureISIN;
	}

	public void setItems(List<Trade> items) {
		this.items = items;
	}

	public List<Trade> getItems() {
		return items;
	}
	
	public void clearItems(){
		this.items.clear();
	}

	public void write(List<? extends Trade> items) throws Exception {
		List<Trade> newItems = new ArrayList<Trade>();
		for(Trade t : items){
			if (t.getIsin().equals(this.writeFailureISIN)){
				throw new RuntimeException("write failed");
			}
			newItems.add(t);
		}
		this.items.addAll(newItems);
	}
}
