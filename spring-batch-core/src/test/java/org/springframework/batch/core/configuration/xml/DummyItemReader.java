package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DummyItemReader implements ItemReader<Object> {

	public Object read() throws Exception, UnexpectedInputException, ParseException {
		return null;
	}

}
