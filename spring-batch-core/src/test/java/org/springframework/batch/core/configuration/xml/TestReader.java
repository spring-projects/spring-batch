package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class TestReader implements ItemReader<String> {

	List<String> items = null;
	
	{
		List<String> l = new ArrayList<String>();
		l.add("Item *** 1 ***");
		l.add("Item *** 2 ***");
		this.items = Collections.synchronizedList(l);
	}

	public String read() throws Exception, UnexpectedInputException,
			ParseException {
		if (items.size() > 0) {
			String item = items.remove(0); 
			return item;
		}
		return null;
	}

}
