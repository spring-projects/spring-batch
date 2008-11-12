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

import java.lang.reflect.Method;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.configuration.util.AnnotationMethodResolver;
import org.springframework.batch.core.configuration.util.MethodInvoker;
import org.springframework.batch.core.configuration.util.MethodResolver;
import org.springframework.batch.core.configuration.util.SimpleMethodInvoker;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link JobExecutionListener} implementation that adapts a delegate object to the 
 * {@link JobExecutionListener} interface, using either string method names, or the
 * {@link BeforeJob} and {@link AfterJob} annotations.  It should be noted that priority
 * is given to method names.
 * 
 * @author Lucas Ward
 * @since 2.0
 */
public class JobExecutionListenerAdapter implements JobExecutionListener, InitializingBean{

	private final Object delegate;
	
	private String beforeMethod;
	private MethodInvoker beforeInvoker;
	
	private String afterMethod;
	private MethodInvoker afterInvoker;
	
	public JobExecutionListenerAdapter(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}
	
	public void afterPropertiesSet() throws Exception {
		if(beforeMethod != null){
			beforeInvoker = new SimpleMethodInvoker(delegate, beforeMethod, JobExecution.class);
		}
		else{
			MethodResolver resolver = new AnnotationMethodResolver(BeforeJob.class);
			Method method = resolver.findMethod(delegate);
			if(method != null){
				beforeInvoker = new SimpleMethodInvoker(delegate, method);
			}
		}
		
		if(afterMethod != null){
			afterInvoker = new SimpleMethodInvoker(delegate, afterMethod, JobExecution.class);
		}
		else{
			MethodResolver resolver = new AnnotationMethodResolver(AfterJob.class);
			Method method = resolver.findMethod(delegate);
			if(method != null){
				afterInvoker = new SimpleMethodInvoker(delegate, method);
			}
		}
		
		if(beforeInvoker == null && afterInvoker == null){
			throw new IllegalArgumentException("No methods found with the provided method name or appropriate annotations");
		}
	}

	public void setBeforeMethod(String beforeMethod) {
		this.beforeMethod = beforeMethod;
	}
	
	public void setAfterMethod(String afterMethod) {
		this.afterMethod = afterMethod;
	}
	
	public void afterJob(JobExecution jobExecution) {
		if(afterInvoker != null){
			afterInvoker.invokeMethod(jobExecution);
		}
	}

	public void beforeJob(JobExecution jobExecution) {
		if(beforeInvoker != null){
			beforeInvoker.invokeMethod(jobExecution);
		}
	}

}
