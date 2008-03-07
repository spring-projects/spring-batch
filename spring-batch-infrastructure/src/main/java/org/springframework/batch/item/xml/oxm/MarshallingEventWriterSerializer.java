package org.springframework.batch.item.xml.oxm;

import java.io.IOException;

import javax.xml.stream.XMLEventWriter;
import javax.xml.transform.Result;

import org.springframework.batch.item.xml.EventWriterSerializer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.oxm.Marshaller;
import org.springframework.xml.transform.StaxResult;

/**
 * Object to xml serializer that wraps a Spring OXM {@link Marshaller} object.
 * 
 * @author Lucas Ward
 * 
 */
public class MarshallingEventWriterSerializer implements EventWriterSerializer {

	private Marshaller marshaller;

	public MarshallingEventWriterSerializer(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	public void serializeObject(XMLEventWriter writer, Object output) {
		Result result = new StaxResult(writer);
		try {
			marshaller.marshal(output, result);
		}
		catch (IOException xse) {
			throw new DataAccessResourceFailureException("Unable to write to file resource: [" + result.getSystemId()
					+ "]", xse);
		}
	}
}
