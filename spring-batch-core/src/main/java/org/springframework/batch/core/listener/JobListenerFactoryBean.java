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

import org.springframework.batch.core.JobExecutionListener;

/**
 * This {@link AbstractListenerFactoryBean} implementation is used to create a
 * {@link JobExecutionListener}.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 * @see AbstractListenerFactoryBean
 * @see JobListenerMetaData
 */
public class JobListenerFactoryBean extends AbstractListenerFactoryBean {

	protected ListenerMetaData getMetaDataFromPropertyName(String propertyName) {
		return JobListenerMetaData.fromPropertyName(propertyName);
	}

	protected ListenerMetaData[] getMetaDataValues() {
		return JobListenerMetaData.values();
	}

	protected Class<?> getDefaultListenerClass() {
		return JobExecutionListener.class;
	}

	public Class<?> getObjectType() {
		return JobExecutionListener.class;
	}

	/**
	 * Convenience method to wrap any object and expose the appropriate
	 * {@link JobExecutionListener} interfaces.
	 * 
	 * @param delegate a delegate object
	 * @return a JobListener instance constructed from the delegate
	 */
	public static JobExecutionListener getListener(Object delegate) {
		JobListenerFactoryBean factory = new JobListenerFactoryBean();
		factory.setDelegate(delegate);
		return (JobExecutionListener) factory.getObject();
	}

	/**
	 * Convenience method to check whether the given object is or can be made
	 * into a {@link JobExecutionListener}.
	 * 
	 * @param delegate the object to check
	 * @return true if the delegate is an instance of
	 *         {@link JobExecutionListener}, or contains the marker annotations
	 */
	public static boolean isListener(Object delegate) {
		return AbstractListenerFactoryBean.isListener(delegate, JobExecutionListener.class, JobListenerMetaData
				.values());
	}
}
