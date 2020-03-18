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
import java.util.List;
import java.util.Map;

import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.api.chunk.listener.ItemProcessListener;
import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.api.chunk.listener.ItemWriteListener;
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.api.listener.StepListener;

import org.springframework.batch.core.listener.ListenerMetaData;
import org.springframework.batch.core.listener.StepListenerFactoryBean;

/**
 * Enumeration for the JSR specific {@link StepListener} meta data, which
 * ties together the names of methods, their interfaces, and expected arguments.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 * @see StepListenerFactoryBean
 */
public enum JsrStepListenerMetaData implements ListenerMetaData {
	BEFORE_STEP("beforeStep", "jsr-before-step", StepListener.class),
	AFTER_STEP("afterStep", "jsr-after-step", StepListener.class),
	BEFORE_CHUNK("beforeChunk", "jsr-before-chunk", ChunkListener.class),
	AFTER_CHUNK("afterChunk", "jsr-after-chunk", ChunkListener.class),
	AFTER_CHUNK_ERROR("onError", "jsr-after-chunk-error", ChunkListener.class, Exception.class),
	BEFORE_READ("beforeRead", "jsr-before-read", ItemReadListener.class),
	AFTER_READ("afterRead", "jsr-after-read", ItemReadListener.class, Object.class),
	AFTER_READ_ERROR("onReadError", "jsr-after-read-error", ItemReadListener.class, Exception.class),
	BEFORE_PROCESS("beforeProcess", "jsr-before-process", ItemProcessListener.class, Object.class),
	AFTER_PROCESS("afterProcess", "jsr-after-process", ItemProcessListener.class, Object.class, Object.class),
	AFTER_PROCESS_ERROR("onProcessError", "jsr-after-process-error", ItemProcessListener.class, Object.class, Exception.class),
	BEFORE_WRITE("beforeWrite", "jsr-before-write", ItemWriteListener.class, List.class),
	AFTER_WRITE("afterWrite", "jsr-after-write", ItemWriteListener.class, List.class),
	AFTER_WRITE_ERROR("onWriteError", "jsr-after-write-error", ItemWriteListener.class, List.class, Exception.class),
	SKIP_READ("onSkipReadItem", "jsr-skip-read", SkipReadListener.class, Exception.class),
	SKIP_PROCESS("onSkipProcessItem", "jsr-skip-process", SkipProcessListener.class, Object.class, Exception.class),
	SKIP_WRITE("onSkipWriteItem", "jsr-skip-write", SkipWriteListener.class, List.class, Exception.class),
	RETRY_READ("onRetryReadException", "jsr-retry-read", RetryReadListener.class, Exception.class),
	RETRY_PROCESS("onRetryProcessException", "jsr-retry-process", RetryProcessListener.class, Object.class, Exception.class),
	RETRY_WRITE("onRetryWriteException", "jsr-retry-write", RetryWriteListener.class, List.class, Exception.class);

	private final String methodName;
	private final String propertyName;
	private final Class<?> listenerInterface;
	private static final Map<String, JsrStepListenerMetaData> propertyMap;
	private final Class<?>[] paramTypes;

	JsrStepListenerMetaData(String methodName, String propertyName, Class<?> listenerInterface, Class<?>... paramTypes) {
		this.propertyName = propertyName;
		this.methodName = methodName;
		this.listenerInterface = listenerInterface;
		this.paramTypes = paramTypes;
	}

	static{
		propertyMap = new HashMap<>();
		for(JsrStepListenerMetaData metaData : values()){
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
		return listenerInterface;
	}

	@Override
	public Class<?>[] getParamTypes() {
		return paramTypes;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Return the relevant meta data for the provided property name.
	 *
	 * @param propertyName the name of the property to return.
	 * @return meta data with supplied property name, null if none exists.
	 */
	public static JsrStepListenerMetaData fromPropertyName(String propertyName){
		return propertyMap.get(propertyName);
	}
}
