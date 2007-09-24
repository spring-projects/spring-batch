package org.springframework.batch.io.oxm;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;

import org.springframework.batch.io.stax.FragmentDeserializer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StaxSource;

/**
 * Delegates deserializing to Spring-WS {@link Unmarshaller}.
 *
 * @author Robert Kasanicky
 * @authoer Lucas Ward
 */
public class UnmarshallingFragmentDeserializer implements FragmentDeserializer {

	private Unmarshaller unmarshaller;

	public UnmarshallingFragmentDeserializer(Unmarshaller unmarshaller){
		this.unmarshaller = unmarshaller;
	}

	public Object deserializeFragment(XMLEventReader eventReader) {
		Object item = null;
		try {
			item = unmarshaller.unmarshal(new StaxSource(eventReader));
		}
		catch (XmlMappingException e) {
			throw new UnmarshallingFailureException("Mapping failure during unmarshalling", e);
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException("IO error during unmarshalling", e);
		}
		return item;
	}
}
