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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.util.MethodInvoker;

/**
 * @author Lucas Ward
 *
 */
public class StepExecutionListenerAdapter<T, S> implements StepExecutionListener, ChunkListener, 
	ItemReadListener<T>, ItemWriteListener<S>, ItemProcessListener<T, S>, SkipListener<T, S>{

	private Set<MethodInvoker> beforeStepMethodInvokers = new HashSet<MethodInvoker>();
	private Set<MethodInvoker> afterMethodInvokers = new HashSet<MethodInvoker>();
	
	public void setAfterMethodInvokers(Set<MethodInvoker> afterMethodInvokers) {
		this.afterMethodInvokers = afterMethodInvokers;
	}
	
	public void setBeforeMethodInvokers(Set<MethodInvoker> beforeMethodInvokers) {
		this.beforeStepMethodInvokers = beforeMethodInvokers;
	}
	
	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	public void beforeStep(StepExecution stepExecution) {
		for(MethodInvoker invoker : beforeStepMethodInvokers){
			invoker.invokeMethod(stepExecution);
		}
	}

	public void afterChunk() {
		// TODO Auto-generated method stub
		
	}

	public void beforeChunk() {
		// TODO Auto-generated method stub
		
	}

	public void afterRead(T item) {
		// TODO Auto-generated method stub
		
	}

	public void beforeRead() {
		// TODO Auto-generated method stub
		
	}

	public void onReadError(Exception ex) {
		// TODO Auto-generated method stub
		
	}

	public void afterWrite(List<? extends S> items) {
		// TODO Auto-generated method stub
		
	}

	public void beforeWrite(List<? extends S> items) {
		// TODO Auto-generated method stub
		
	}

	public void onWriteError(Exception exception, List<? extends S> items) {
		// TODO Auto-generated method stub
		
	}

	public void afterProcess(T item, S result) {
		// TODO Auto-generated method stub
		
	}

	public void beforeProcess(T item) {
		// TODO Auto-generated method stub
		
	}

	public void onProcessError(T item, Exception e) {
		// TODO Auto-generated method stub
		
	}

	public void onSkipInProcess(T item, Throwable t) {
		// TODO Auto-generated method stub
		
	}

	public void onSkipInRead(Throwable t) {
		// TODO Auto-generated method stub
		
	}

	public void onSkipInWrite(S item, Throwable t) {
		// TODO Auto-generated method stub
		
	}

}
