/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.batch.core.StepListener;

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

	protected ListenerMetaData getMetaDataFromPropertyName(String propertyName) {
		return StepListenerMetaData.fromPropertyName(propertyName);
	}

	protected ListenerMetaData[] getMetaDataValues() {
		return StepListenerMetaData.values();
	}

	protected Class<?> getDefaultListenerClass() {
		return StepListener.class;
	}

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
