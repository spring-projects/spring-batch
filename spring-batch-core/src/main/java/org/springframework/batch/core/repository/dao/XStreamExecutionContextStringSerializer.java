/*
 * Copyright 2006-2008 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * Implementation that uses XStream and Jettison to provide serialization.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @since 2.0
 * @see ExecutionContextSerializer
 */
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
	 * @param context
	 * @param out
	 * @see Serializer#serialize(Object, OutputStream)
	 */
	public void serialize(Object context, OutputStream out) throws IOException {
		Assert.notNull(context);
		Assert.notNull(out);

		out.write(xstream.toXML(context).getBytes());
	}

	/**
	 * Deserializes the supplied input stream into a new execution context.
	 *
	 * @param in
	 * @return a reconstructed execution context
	 * @see Deserializer#deserialize(InputStream)
	 */
	@SuppressWarnings("unchecked")
	public Object deserialize(InputStream in) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		StringBuilder sb = new StringBuilder();

		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}

		return xstream.fromXML(sb.toString());
	}
}
