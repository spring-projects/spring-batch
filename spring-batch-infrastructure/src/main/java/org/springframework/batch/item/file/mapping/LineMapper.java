package org.springframework.batch.item.file.mapping;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.transform.LineTokenizer;


/**
 * Interface for mapping lines (strings) to domain objects typically used by
 * {@link FlatFileItemReader} to map lines read from a file to domain objects
 * on a per line basis.  Implementations of this interface perform the actual
 * work of parsing a line without having to deal with how the line was
 * obtained.  
 * 
 * @author Robert Kasanicky
 * @param <T> type of the domain object
 * @see FieldSetMapper
 * @see LineTokenizer
 * @since 2.0
 */
public interface LineMapper<T> {

	/**
	 * Implementations must implement this method to map the provided line to 
	 * the parameter type T.  The line number represents the number of lines
	 * into a file the current line resides.
	 * 
	 * @param line to be mapped
	 * @param lineNumber of the current line
	 * @return mapped object of type T
	 * @throws Exception if error occured while parsing.
	 */
	T mapLine(String line, int lineNumber) throws Exception;
}
