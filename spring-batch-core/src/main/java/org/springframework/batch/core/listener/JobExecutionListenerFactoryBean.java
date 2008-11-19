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
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} implementation that accepts a delgate, checking whether
 * or not it implements the {@link JobExecutionListener} interface, and if so, passes directly
 * through.  However, if it does not implement the interface, {@link JobExecutionListenerAdapter}
 * is used to 
 * 
 * @author Lucas Ward
 *
 */
public class JobExecutionListenerFactoryBean implements FactoryBean, InitializingBean{

	private Object delegate;
	private String beforeMethod;
	private String afterMethod;
	
	public void setDelegate(Object delegate) {
		this.delegate = delegate;
	}
	
	public void setBeforeMethod(String beforeMethod) {
		this.beforeMethod = beforeMethod;
	}
	
	public void setAfterMethod(String afterMethod) {
		this.afterMethod = afterMethod;
	}
	
	public Object getObject() throws Exception {
		if(delegate instanceof JobExecutionListener){
			return delegate;
		}
		else{
			JobExecutionListenerAdapter listenerAdapter = new JobExecutionListenerAdapter(delegate);
			listenerAdapter.setBeforeMethod(beforeMethod);
			listenerAdapter.setAfterMethod(afterMethod);
			listenerAdapter.afterPropertiesSet();
			return listenerAdapter;
		}
	}

	@SuppressWarnings("unchecked")
	public Class getObjectType() {
		return JobExecutionListener.class;
	}

	public boolean isSingleton() {
		return false;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "Delegate listener must not be null");
	}
}
