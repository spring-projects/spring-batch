/**
 *
 */
package org.springframework.batch.core.repository.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

/**
 * An implementation of the {@link ExecutionContextSerializer} using the default
 * serialization implementations from Spring ({@link DefaultSerializer} and
 * {@link DefaultDeserializer}).
 *
 * @author Michael Minella
 * @since 2.2
 */
public class DefaultExecutionContextSerializer implements ExecutionContextSerializer {

	private Serializer serializer = new DefaultSerializer();
	private Deserializer deserializer = new DefaultDeserializer();

	/**
	 * Serializes an execution context to the provided {@link OutputStream}.  The
	 * stream is not closed prior to it's return.
	 *
	 * @param context
	 * @param out
	 */
    @Override
	@SuppressWarnings("unchecked")
	public void serialize(Object context, OutputStream out) throws IOException {
		Assert.notNull(context);
		Assert.notNull(out);

		serializer.serialize(context, out);
	}

	/**
	 * Deserializes an execution context from the provided {@link InputStream}.
	 *
	 * @param inputStream
	 * @return the object serialized in the provided {@link InputStream}
	 */
    @Override
	public Object deserialize(InputStream inputStream) throws IOException {
		return deserializer.deserialize(inputStream);
	}

}
