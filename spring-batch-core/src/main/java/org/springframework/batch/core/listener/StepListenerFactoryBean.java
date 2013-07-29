/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.batch.core.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.jsr.JsrStepListenerMetaData;

/**
 * This {@link AbstractListenerFactoryBean} implementation is used to create a
 * {@link StepListener}.
 *
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 * @see AbstractListenerFactoryBean
 * @see StepListenerMetaData
 */
public class StepListenerFactoryBean extends AbstractListenerFactoryBean {

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
		List<ListenerMetaData> values = new ArrayList<ListenerMetaData>();
		Collections.addAll(values, StepListenerMetaData.values());
		Collections.addAll(values, JsrStepListenerMetaData.values());

		return values.toArray(new ListenerMetaData[0]);
	}

	@Override
	protected Class<?> getDefaultListenerClass() {
		return StepListener.class;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class getObjectType() {
		return StepListener.class;
	}

	/**
	 * Convenience method to wrap any object and expose the appropriate
	 * {@link StepListener} interfaces.
	 *
	 * @param delegate a delegate object
	 * @return a StepListener instance constructed from the delegate
	 */
	public static StepListener getListener(Object delegate) {
		StepListenerFactoryBean factory = new StepListenerFactoryBean();
		factory.setDelegate(delegate);
		return (StepListener) factory.getObject();
	}

	/**
	 * Convenience method to check whether the given object is or can be made
	 * into a {@link StepListener}.
	 *
	 * @param delegate the object to check
	 * @return true if the delegate is an instance of any of the
	 *         {@link StepListener} interfaces, or contains the marker
	 *         annotations
	 */
	public static boolean isListener(Object delegate) {
		return AbstractListenerFactoryBean.isListener(delegate, StepListener.class, StepListenerMetaData.values());
	}
}
