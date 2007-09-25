package org.springframework.batch.io.oxm;

import java.io.IOException;

import javax.xml.stream.XMLEventWriter;
import javax.xml.transform.Result;

import org.springframework.batch.io.stax.ObjectToXmlSerializer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.oxm.Marshaller;
import org.springframework.xml.transform.StaxResult;

/**
 * Object to xml serializer that wraps a Spring-OXM
 * Marshaller object.
 *
 * @author Lucas Ward
 *
 */
public class MarshallingObjectToXmlSerializer implements ObjectToXmlSerializer{

	private Marshaller marshaller;

	private Result result;

	public MarshallingObjectToXmlSerializer(Marshaller marshaller){
		this.marshaller = marshaller;
	}

	public void setEventWriter(XMLEventWriter writer) {
		result = new StaxResult(writer);
	}

	public void serializeObject(Object output) {

		try {
			marshaller.marshal(output, result);
		} catch (IOException xse) {
			throw new DataAccessResourceFailureException(
					"Unable to write to file resource: [" + result.getSystemId() + "]", xse);
		}
	}
}
