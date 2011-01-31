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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterRead;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.annotation.OnProcessError;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;
import org.springframework.batch.core.annotation.OnWriteError;

/**
 * Enumeration for {@link StepListener} meta data, which ties together the names
 * of methods, their interfaces, annotation, and expected arguments.
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see StepListenerFactoryBean
 */
public enum StepListenerMetaData implements ListenerMetaData {

	BEFORE_STEP("beforeStep", "before-step-method", BeforeStep.class, StepExecutionListener.class, StepExecution.class),
	AFTER_STEP("afterStep", "after-step-method", AfterStep.class, StepExecutionListener.class, StepExecution.class),
	BEFORE_CHUNK("beforeChunk", "before-chunk-method", BeforeChunk.class, ChunkListener.class),
	AFTER_CHUNK("afterChunk", "after-chunk-method", AfterChunk.class, ChunkListener.class),
	BEFORE_READ("beforeRead", "before-read-method", BeforeRead.class, ItemReadListener.class),
	AFTER_READ("afterRead", "after-read-method", AfterRead.class, ItemReadListener.class, Object.class),
	ON_READ_ERROR("onReadError", "on-read-error-method", OnReadError.class, ItemReadListener.class, Exception.class),
	BEFORE_PROCESS("beforeProcess", "before-process-method", BeforeProcess.class, ItemProcessListener.class, Object.class),
	AFTER_PROCESS("afterProcess", "after-process-method", AfterProcess.class, ItemProcessListener.class, Object.class, Object.class),
	ON_PROCESS_ERROR("onProcessError", "on-process-error-method", OnProcessError.class, ItemProcessListener.class, Object.class, Exception.class),
	BEFORE_WRITE("beforeWrite", "before-write-method", BeforeWrite.class, ItemWriteListener.class, List.class),
	AFTER_WRITE("afterWrite", "after-write-method", AfterWrite.class, ItemWriteListener.class, List.class),
	ON_WRITE_ERROR("onWriteError", "on-write-error-method", OnWriteError.class, ItemWriteListener.class, Exception.class, List.class),
	ON_SKIP_IN_READ("onSkipInRead", "on-skip-in-read-method", OnSkipInRead.class, SkipListener.class, Throwable.class),
	ON_SKIP_IN_PROCESS("onSkipInProcess", "on-skip-in-process-method", OnSkipInProcess.class, SkipListener.class, Object.class, Throwable.class),
	ON_SKIP_IN_WRITE("onSkipInWrite", "on-skip-in-write-method", OnSkipInWrite.class, SkipListener.class, Object.class, Throwable.class);
	
	private final String methodName;
	private final String propertyName;
	private final Class<? extends Annotation> annotation;
	private final Class<? extends StepListener> listenerInterface;
	private final Class<?>[] paramTypes;
	private static final Map<String, StepListenerMetaData> propertyMap;
	
	StepListenerMetaData(String methodName, String propertyName, Class<? extends Annotation> annotation, Class<? extends StepListener> listenerInterface, Class<?>... paramTypes) {
		this.methodName = methodName;
		this.propertyName = propertyName;
		this.annotation = annotation;
		this.listenerInterface = listenerInterface;
		this.paramTypes = paramTypes;
	}
	
	static{
		propertyMap = new HashMap<String, StepListenerMetaData>();
		for(StepListenerMetaData metaData : values()){
			propertyMap.put(metaData.getPropertyName(), metaData);
		}
	}

	public String getMethodName() {
		return methodName;
	}

	public Class<? extends Annotation> getAnnotation() {
		return annotation;
	}

	public Class<?> getListenerInterface() {
		return listenerInterface;
	}

	public Class<?>[] getParamTypes() {
		return paramTypes;
	}
	
	public String getPropertyName() {
		return propertyName;
	}
	
	/**
	 * Return the relevant meta data for the provided property name.
	 * 
	 * @param propertyName
	 * @return meta data with supplied property name, null if none exists.
	 */
	public static StepListenerMetaData fromPropertyName(String propertyName){
		return propertyMap.get(propertyName);
	}
	
	public static ListenerMetaData[] itemListenerMetaData() {
		return new ListenerMetaData[] {BEFORE_WRITE, AFTER_WRITE, ON_WRITE_ERROR, BEFORE_PROCESS, AFTER_PROCESS, ON_PROCESS_ERROR, BEFORE_READ, AFTER_READ, ON_READ_ERROR, ON_SKIP_IN_WRITE, ON_SKIP_IN_PROCESS, ON_SKIP_IN_READ};
	}

	public static ListenerMetaData[] stepExecutionListenerMetaData() {
		return new ListenerMetaData[] {BEFORE_STEP, AFTER_STEP};
	}

	public static ListenerMetaData[] taskletListenerMetaData() {
		return new ListenerMetaData[] {BEFORE_CHUNK, AFTER_CHUNK};
	}

}
