/*
 * Copyright 2006-2018 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

/**
 * Implementation that uses XStream and Jettison to provide serialization.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.0
 * @see ExecutionContextSerializer
 * @deprecated Due to the incompatibilities between current Jettison versions and XStream
 * 		versions, this serializer is deprecated in favor of
 * 		{@link Jackson2ExecutionContextStringSerializer}
 */
@Deprecated
public class XStreamExecutionContextStringSerializer implements ExecutionContextSerializer, InitializingBean {

	private ReflectionProvider reflectionProvider = null;

	private HierarchicalStreamDriver hierarchicalStreamDriver;

	private XStream xstream;

	public void setReflectionProvider(ReflectionProvider reflectionProvider) {
		this.reflectionProvider = reflectionProvider;
	}

	public void setHierarchicalStreamDriver(HierarchicalStreamDriver hierarchicalStreamDriver) {
		this.hierarchicalStreamDriver = hierarchicalStreamDriver;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		init();
	}

	public synchronized void init() throws Exception {
		if (hierarchicalStreamDriver == null) {
			this.hierarchicalStreamDriver = new JettisonMappedXmlDriver();
		}
		if (reflectionProvider == null) {
			xstream =  new XStream(hierarchicalStreamDriver);
		}
		else {
			xstream = new XStream(reflectionProvider, hierarchicalStreamDriver);
		}
	}

	/**
	 * Serializes the passed execution context to the supplied OutputStream.
	 *
	 * @param context {@link Map} containing the context information.
	 * @param out {@link OutputStream} where the serialized context information
	 * will be written.
	 *
	 * @see Serializer#serialize(Object, OutputStream)
	 */
	@Override
	public void serialize(Map<String, Object> context, OutputStream out) throws IOException {
		Assert.notNull(context, "context is required");
		Assert.notNull(out, "An OutputStream is required");

		out.write(xstream.toXML(context).getBytes());
	}

	/**
	 * Deserializes the supplied input stream into a new execution context.
	 *
	 * @param in {@link InputStream} containing the information to be deserialized.

	 * @return a reconstructed execution context
	 * @see Deserializer#deserialize(InputStream)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> deserialize(InputStream in) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		StringBuilder sb = new StringBuilder();

		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}

		return (Map<String, Object>) xstream.fromXML(sb.toString());
	}
}
