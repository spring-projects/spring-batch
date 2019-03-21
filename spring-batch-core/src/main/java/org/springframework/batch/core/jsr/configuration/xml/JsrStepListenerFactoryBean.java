/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.batch.core.jsr.JsrStepListenerMetaData;
import org.springframework.batch.core.listener.ListenerMetaData;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.listener.StepListenerMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Minella
 */
public class JsrStepListenerFactoryBean extends StepListenerFactoryBean {

	@Override
	protected ListenerMetaData getMetaDataFromPropertyName(String propertyName) {
		ListenerMetaData metaData = StepListenerMetaData.fromPropertyName(propertyName);

		if(metaData == null) {
			metaData = JsrStepListenerMetaData.fromPropertyName(propertyName);
		}

		return metaData;
	}

	@Override
	protected ListenerMetaData[] getMetaDataValues() {
		List<ListenerMetaData> values = new ArrayList<>();
		Collections.addAll(values, StepListenerMetaData.values());
		Collections.addAll(values, JsrStepListenerMetaData.values());

		return values.toArray(new ListenerMetaData[values.size()]);
	}
}
