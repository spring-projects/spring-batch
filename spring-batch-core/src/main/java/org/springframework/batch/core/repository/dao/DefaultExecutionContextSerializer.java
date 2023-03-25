/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.repository.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Map;

import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.util.Assert;

/**
 * An implementation of the {@link ExecutionContextSerializer} that produces/consumes
 * Base64 content.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.2
 */
public class DefaultExecutionContextSerializer implements ExecutionContextSerializer {

	/**
	 * Serializes an execution context to the provided {@link OutputStream}. The stream is
	 * not closed prior to it's return.
	 * @param context {@link Map} contents of the {@code ExecutionContext}.
	 * @param out {@link OutputStream} where the serialized context information will be
	 * written.
	 */
	@Override
	public void serialize(Map<String, Object> context, OutputStream out) throws IOException {
		Assert.notNull(context, "context is required");
		Assert.notNull(out, "OutputStream is required");

		for (Object value : context.values()) {
			Assert.notNull(value, "A null value was found");
			if (!(value instanceof Serializable)) {
				throw new IllegalArgumentException(
						"Value: [" + value + "] must be serializable. " + "Object of class: ["
								+ value.getClass().getName() + "] must be an instance of " + Serializable.class);
			}
		}
		var byteArrayOutputStream = new ByteArrayOutputStream(1024);
		var encodingStream = Base64.getEncoder().wrap(byteArrayOutputStream);
		try (var objectOutputStream = new ObjectOutputStream(encodingStream)) {
			objectOutputStream.writeObject(context);
		}
		out.write(byteArrayOutputStream.toByteArray());
	}

	/**
	 * Deserializes an execution context from the provided {@link InputStream}.
	 * @param inputStream {@link InputStream} containing the information to be
	 * deserialized.
	 * @return the object serialized in the provided {@link InputStream}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> deserialize(InputStream inputStream) throws IOException {
		var decodingStream = Base64.getDecoder().wrap(inputStream);
		try {
			var objectInputStream = new ObjectInputStream(decodingStream);
			return (Map<String, Object>) objectInputStream.readObject();
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to deserialize object", ex);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to deserialize object type", ex);
		}
	}

}
