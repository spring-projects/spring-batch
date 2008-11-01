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
package org.springframework.batch.core.configuration.support;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * A {@link JobFactory} that creates its own {@link ApplicationContext} from a
 * path supplied, and pulls a bean out when asked to create a {@link Job}.
 * 
 * @author Dave Syer
 * 
 */
public class ClassPathXmlApplicationContextJobFactory implements JobFactory {

	final private String beanName;

	final private String path;

	final private ApplicationContext parent;

	/**
	 * @param beanName the id of the {@link Job} in the application context to
	 * be created
	 * @param path the path to the XML configuration containing the {@link Job}
	 * @param parent the application context to use as a parent (or null)
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
	 * @see org.springframework.batch.core.configuration.JobFactory#createJob()
	 */
	public Job createJob() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { path }, false, parent);
		context.setDisplayName("Job ApplicationContext "+beanName);
		context.refresh();
		Job job = (Job) context.getBean(beanName, Job.class);
		return new ContextClosingJob(job, context);
	}

	/**
	 * Return the bean name of the job in the application context. N.B. this is
	 * usually the name of the job as well, but it needn't be. The important
	 * thing is that the job can be located by this name.
	 * 
	 * @see org.springframework.batch.core.configuration.JobFactory#getJobName()
	 */
	public String getJobName() {
		return beanName;
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static class ContextClosingJob implements Job {
		private Job delegate;

		private ConfigurableApplicationContext context;

		/**
		 * @param delegate
		 * @param context
		 */
		public ContextClosingJob(Job delegate, ConfigurableApplicationContext context) {
			super();
			this.delegate = delegate;
			this.context = context;
		}

		/**
		 * @param execution
		 * @throws JobExecutionException
		 * @see org.springframework.batch.core.Job#execute(org.springframework.batch.core.JobExecution)
		 */
		public void execute(JobExecution execution) throws JobExecutionException {
			try {
				delegate.execute(execution);
			}
			finally {
				context.close();
			}
		}

		/**
		 * @see org.springframework.batch.core.Job#getName()
		 */
		public String getName() {
			return delegate.getName();
		}

		/**
		 * @deprecated planned for removal in 2.0
		 * @see org.springframework.batch.core.Job#getSteps()
		 */
		public List getSteps() {
			return delegate.getSteps();
		}

		/**
		 * @see org.springframework.batch.core.Job#isRestartable()
		 */
		public boolean isRestartable() {
			return delegate.isRestartable();
		}

	}

}
