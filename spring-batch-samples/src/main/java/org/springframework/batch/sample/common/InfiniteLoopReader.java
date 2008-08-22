/**
 * 
 */
package org.springframework.batch.sample.common;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * ItemReader implementation that will continually return a new object.  It's generally
 * useful for testing interruption.
 * 
 * @author Lucas Ward
 *
 */
public class InfiniteLoopReader implements ItemReader<Object> {

	public Object read() throws Exception, UnexpectedInputException,
			NoWorkFoundException, ParseException {
		return new Object();
	}

}
