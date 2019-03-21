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
package org.springframework.batch.core.jsr;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.batch.api.listener.JobListener;

import org.springframework.batch.core.listener.ListenerMetaData;

/**
 * Enumeration for {@link JobListener} meta data, which ties together the names
 * of methods, their interfaces, annotation, and expected arguments.
 *
 * @author Michael Minella
 * @since 3.0
 */
public enum JsrJobListenerMetaData implements ListenerMetaData {
	BEFORE_JOB("beforeJob", "jsr-before-job"),
	AFTER_JOB("afterJob", "jsr-after-job");

	private final String methodName;
	private final String propertyName;
	private static final Map<String, JsrJobListenerMetaData> propertyMap;

	JsrJobListenerMetaData(String methodName, String propertyName) {
		this.methodName = methodName;
		this.propertyName = propertyName;
	}

	static{
		propertyMap = new HashMap<>();
		for(JsrJobListenerMetaData metaData : values()){
			propertyMap.put(metaData.getPropertyName(), metaData);
		}
	}

	@Override
	public String getMethodName() {
		return methodName;
	}

	@Override
	public Class<? extends Annotation> getAnnotation() {
		return null;
	}

	@Override
	public Class<?> getListenerInterface() {
		return JobListener.class;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public Class<?>[] getParamTypes() {
		return new Class<?>[0];
	}

	/**
	 * Return the relevant meta data for the provided property name.
	 *
	 * @param propertyName the name of the property to return.
	 * @return meta data with supplied property name, null if none exists.
	 */
	public static JsrJobListenerMetaData fromPropertyName(String propertyName){
		return propertyMap.get(propertyName);
	}
}
