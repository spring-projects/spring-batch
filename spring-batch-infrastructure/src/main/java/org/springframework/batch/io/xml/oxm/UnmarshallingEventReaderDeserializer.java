package org.springframework.batch.io.xml.oxm;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;

import org.springframework.batch.io.xml.EventReaderDeserializer;
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
public class UnmarshallingEventReaderDeserializer implements EventReaderDeserializer {

	private Unmarshaller unmarshaller;

	public UnmarshallingEventReaderDeserializer(Unmarshaller unmarshaller){
		Assert.notNull(unmarshaller);
		this.unmarshaller = unmarshaller;
	}

	public Object deserializeFragment(XMLEventReader eventReader) {
		Object item = null;
		try {
			item = unmarshaller.unmarshal(new StaxSource(eventReader));
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException("IO error during unmarshalling", e);
		}
		return item;
	}
}
