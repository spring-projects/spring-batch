package org.springframework.batch.item.file.mapping;

/**
 * Interface for mapping lines (strings) to domain objects.
 * 
 * @author Robert Kasanicky
 *
 * @param <T> type of the domain object
 */
public interface LineMapper<T> {

	T mapLine(String line, int lineNumber) throws Exception;
}
