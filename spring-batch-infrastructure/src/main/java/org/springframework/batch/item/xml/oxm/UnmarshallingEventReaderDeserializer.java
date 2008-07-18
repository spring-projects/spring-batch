package org.springframework.batch.item.xml.oxm;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;

import org.springframework.batch.item.xml.EventReaderDeserializer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.xml.transform.StaxSource;

/**
 * Delegates deserializing to Spring OXM {@link Unmarshaller}.
 *
 * @author Robert Kasanicky
 * @author Lucas Ward
 */
public class UnmarshallingEventReaderDeserializer<T> implements EventReaderDeserializer<T> {

	private Unmarshaller unmarshaller;

	public UnmarshallingEventReaderDeserializer(Unmarshaller unmarshaller){
		Assert.notNull(unmarshaller);
		this.unmarshaller = unmarshaller;
	}

	@SuppressWarnings("unchecked")
	public T deserializeFragment(XMLEventReader eventReader) {
		T item = null;
		try {
			item = (T) unmarshaller.unmarshal(new StaxSource(eventReader));
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException("IO error during unmarshalling", e);
		}
		return item;
	}
}
