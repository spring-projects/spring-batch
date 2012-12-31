/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
@SuppressWarnings("rawtypes")
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
