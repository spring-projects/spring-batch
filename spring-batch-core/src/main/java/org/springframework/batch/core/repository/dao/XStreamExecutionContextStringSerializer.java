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

import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.XStream;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

/**
 * Implementation that uses XStream and Jettison to provide serialization.
 * 
 * @author Thomas Risberg
 * @since 2.0
 */
public class XStreamExecutionContextStringSerializer implements ExecutionContextStringSerializer, InitializingBean {

	private ReflectionProvider reflectionProvider = null;

	private HierarchicalStreamDriver hierarchicalStreamDriver;

	private XStream xstream;

	public String serialize(Map<String, Object> context) {
		return xstream.toXML(context);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> deserialize(String context) {
		return (Map<String, Object>) xstream.fromXML(context);
	}

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
}
