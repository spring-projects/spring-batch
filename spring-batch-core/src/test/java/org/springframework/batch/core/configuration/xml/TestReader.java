package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class TestReader extends AbstractTestComponent implements ItemReader<String>, ItemStream {

	private boolean opened = false;

	List<String> items = null;

	{
		List<String> l = new ArrayList<String>();
		l.add("Item *** 1 ***");
		l.add("Item *** 2 ***");
		this.items = Collections.synchronizedList(l);
	}

	public boolean isOpened() {
		return opened;
	}

	public void setOpened(boolean opened) {
		this.opened = opened;
	}

	public String read() throws Exception, UnexpectedInputException, ParseException {
		executed = true;
		synchronized (items) {
			if (items.size() > 0) {
				String item = items.remove(0);
				return item;
			}
		}
		return null;
	}

	public void close() throws ItemStreamException {
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {
		opened = true;
	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
	}

}
