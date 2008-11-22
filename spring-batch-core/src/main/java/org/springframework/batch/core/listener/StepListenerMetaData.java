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
public enum StepListenerMetaData {

	BEFORE_STEP("beforeStep", BeforeStep.class, StepExecutionListener.class, StepExecution.class),
	AFTER_STEP("afterStep", AfterStep.class, StepExecutionListener.class, StepExecution.class),
	BEFORE_CHUNK("beforeChunk", BeforeChunk.class, ChunkListener.class),
	AFTER_CHUNK("afterChunk", AfterChunk.class, ChunkListener.class),
	BEFORE_READ("beforeRead", BeforeRead.class, ItemReadListener.class),
	AFTER_READ("afterRead", AfterRead.class, ItemReadListener.class, Object.class),
	ON_READ_ERROR("onReadError", OnReadError.class, ItemReadListener.class, Exception.class),
	BEFORE_PROCESS("beforeProcess", BeforeProcess.class, ItemProcessListener.class, Object.class),
	AFTER_PROCESS("afterProcess", AfterProcess.class, ItemProcessListener.class, Object.class),
	ON_PROCESS_ERROR("onProcessError", OnProcessError.class, ItemProcessListener.class, Object.class, Exception.class),
	BEFORE_WRITE("beforeWrite", BeforeWrite.class, ItemWriteListener.class, Object.class),
	AFTER_WRITE("afterWrite", AfterWrite.class, ItemWriteListener.class, Object.class),
	ON_WRITE_ERROR("onWriteError", OnWriteError.class, ItemWriteListener.class, Object.class, Exception.class),
	ON_SKIP_IN_READ("onSkipInRead", OnSkipInRead.class, SkipListener.class, Throwable.class),
	ON_SKIP_IN_PROCESS("onSkipInProcess", OnSkipInProcess.class, SkipListener.class, Object.class, Throwable.class),
	ON_SKIP_IN_WRITE("onSkipInWrite", OnSkipInWrite.class, SkipListener.class, Object.class, Throwable.class);
	
	private final String methodName;
	private final Class<? extends Annotation> annotation;
	private final Class<? extends StepListener> listenerInterface;
	private final Class<?>[] paramTypes;
	
	StepListenerMetaData(String methodName, Class<? extends Annotation> annotation, Class<? extends StepListener> listenerInterface, Class<?>... paramTypes) {
		this.methodName = methodName;
		this.annotation = annotation;
		this.listenerInterface = listenerInterface;
		this.paramTypes = paramTypes;
	}

	public String getMethodName() {
		return methodName;
	}

	public Class<? extends Annotation> getAnnotation() {
		return annotation;
	}

	public Class<? extends StepListener> getListenerInterface() {
		return listenerInterface;
	}

	public Class<?>[] getParamTypes() {
		return paramTypes;
	}
}
