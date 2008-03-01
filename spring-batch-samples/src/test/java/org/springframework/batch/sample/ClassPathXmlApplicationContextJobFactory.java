/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.sample;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.repository.JobFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * A {@link JobFactory} that creates its own {@link ApplicationContext} from a
 * path supplied, and pulls a bean out when asked to create a {@link Job}.
 * 
 * @author Dave Syer
 * 
 */
public class ClassPathXmlApplicationContextJobFactory implements JobFactory {

	private String beanName;

	private String path;

	private ApplicationContext parent;

	/**
	 * @param beanName
	 * @param path
	 */
	public ClassPathXmlApplicationContextJobFactory(String beanName, String path, ApplicationContext parent) {
		super();
		this.beanName = beanName;
		this.path = path;
		this.parent = parent;
	}

	/**
	 * Create a {@link ClassPathXmlApplicationContext} from the path provided
	 * and pull out a bean with the name given during initialization.
	 * 
	 * @see org.springframework.batch.core.repository.JobFactory#createJob()
	 */
	public Job createJob() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { path }, parent);
		return (Job) context.getBean(beanName, Job.class);
	}

	/**
	 * Return the bean name of the job in the application context. N.B. this is
	 * usually the name of the job as well, but it needn't be. The important
	 * thing is that the job can be located by this name.
	 * 
	 * @see org.springframework.batch.core.repository.JobFactory#getJobName()
	 */
	public String getJobName() {
		return beanName;
	}

}
