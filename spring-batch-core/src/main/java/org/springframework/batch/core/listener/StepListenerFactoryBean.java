/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.batch.core.listener;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.jsr.JsrStepListenerMetaData;

/**
 * This {@link AbstractListenerFactoryBean} implementation is used to create a
 * {@link StepListener}.
 *
 * @author Lucas Ward
 * @author Dan Garrette
 * @author Alexei KLENIN
 * @since 2.0
 * @see AbstractListenerFactoryBean
 * @see StepListenerMetaData
 */
public class StepListenerFactoryBean extends AbstractListenerFactoryBean<StepListener> {
	private static final String STR_STEP_LISTENER_ANNOTATIONS_LIST = Stream
			.of(StepListenerMetaData.values())
			.map(StepListenerMetaData::getAnnotation)
			.map(Class::getSimpleName)
			.map(annotation -> String.format("\t- @%s\n", annotation))
			.collect(Collectors.joining());
	private static final String ERR_OBJECT_NOT_STEP_LISTENER_TEMPLATE =
			"Object of type [%s] is not a valid step listener. " +
			"It must ether implement StepListener interface or have methods annotated with any of:\n" +
			STR_STEP_LISTENER_ANNOTATIONS_LIST;

	@Override
	protected ListenerMetaData getMetaDataFromPropertyName(String propertyName) {
		return StepListenerMetaData.fromPropertyName(propertyName);
	}

	@Override
	protected ListenerMetaData[] getMetaDataValues() {
		return StepListenerMetaData.values();
	}

	@Override
	protected Class<?> getDefaultListenerClass() {
		return StepListener.class;
	}

	@Override
	public Class<StepListener> getObjectType() {
		return StepListener.class;
	}

	@Override
	public void setDelegate(Object delegate) {
		super.setDelegate(assertListener(delegate));
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
		ListenerMetaData[] metaDataValues = Stream
				.<ListenerMetaData> concat(
					Stream.of(StepListenerMetaData.values()),
					Stream.of(JsrStepListenerMetaData.values()))
				.toArray(ListenerMetaData[]::new);

		return AbstractListenerFactoryBean.isListener(delegate, StepListener.class, metaDataValues);
	}

	/**
	 * Asserts that {@code delegate} is a valid step listener. If this is not a case, throws an
	 * {@link IllegalArgumentException} with message detailing the problem. Object is a valid
	 * step listener is it ether implements interface {@link StepListener} or has any method
	 * annotated with one of marker annotations.
	 *
	 * @param delegate the object to check
	 * @return valid step execution listener
	 */
	public static Object assertListener(Object delegate) {
		if (!isListener(delegate)) {
			throw new IllegalArgumentException(String.format(
					ERR_OBJECT_NOT_STEP_LISTENER_TEMPLATE, delegate.getClass().getName()));
		}

		return delegate;
	}
}
