/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.lang.Nullable;

/**
 * Enumeration for {@link JobExecutionListener} meta data, which ties together the names
 * of methods, their interfaces, annotation, and expected arguments.
 *
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @since 2.0
 * @see JobListenerFactoryBean
 */
public enum JobListenerMetaData implements ListenerMetaData {

	BEFORE_JOB("beforeJob", "before-job-method", BeforeJob.class),
	AFTER_JOB("afterJob", "after-job-method", AfterJob.class);


	private final String methodName;
	private final String propertyName;
	private final Class<? extends Annotation> annotation;
	private static final Map<String, JobListenerMetaData> propertyMap;

	JobListenerMetaData(String methodName, String propertyName, Class<? extends Annotation> annotation) {
		this.methodName = methodName;
		this.propertyName = propertyName;
		this.annotation = annotation;
	}

	static{
		propertyMap = new HashMap<>();
		for(JobListenerMetaData metaData : values()){
			propertyMap.put(metaData.getPropertyName(), metaData);
		}
	}

	@Override
	public String getMethodName() {
		return methodName;
	}

	@Override
	public Class<? extends Annotation> getAnnotation() {
		return annotation;
	}

	@Override
	public Class<?> getListenerInterface() {
		return JobExecutionListener.class;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public Class<?>[] getParamTypes() {
		return new Class<?>[]{ JobExecution.class };
	}

	/**
	 * Return the relevant meta data for the provided property name.
	 *
	 * @param propertyName name of the property to retrieve.
	 * @return meta data with supplied property name, {@code null} if none exists.
	 */
	@Nullable
	public static JobListenerMetaData fromPropertyName(String propertyName){
		return propertyMap.get(propertyName);
	}
}
