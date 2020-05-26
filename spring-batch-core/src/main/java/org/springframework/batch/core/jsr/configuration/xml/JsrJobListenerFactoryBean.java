/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.batch.api.listener.JobListener;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.jsr.JsrJobListenerMetaData;
import org.springframework.batch.core.listener.JobListenerMetaData;
import org.springframework.batch.core.listener.ListenerMetaData;
import org.springframework.beans.factory.FactoryBean;

/**
 * This {@link FactoryBean} is used by the JSR-352 namespace parser to create
 * {@link JobExecutionListener} objects.
 * 
 * @author Michael Minella
 * @since 3.0
 */
public class JsrJobListenerFactoryBean extends org.springframework.batch.core.listener.JobListenerFactoryBean {

	@Override
	public Class<?> getObjectType() {
		return JobListener.class;
	}

	@Override
	protected ListenerMetaData[] getMetaDataValues() {
		List<ListenerMetaData> values = new ArrayList<>();
		Collections.addAll(values, JobListenerMetaData.values());
		Collections.addAll(values, JsrJobListenerMetaData.values());

		return values.toArray(new ListenerMetaData[0]);
	}

	@Override
	protected ListenerMetaData getMetaDataFromPropertyName(String propertyName) {
		ListenerMetaData result = JobListenerMetaData.fromPropertyName(propertyName);

		if(result == null) {
			result = JsrJobListenerMetaData.fromPropertyName(propertyName);
		}

		return result;
	}
}
