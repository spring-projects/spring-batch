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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.configuration.JobFactory;

/**
 * A {@link JobFactory} that can optionally prepend a group name to a job's
 * name, to make it fit a naming convention for type or origin. E.g. the source
 * job might be <code>overnightJob</code> and the group
 * <code>financeDepartment</code>, which would result in a {@link Job} with
 * identical functionality but named <code>financeDepartment$overnightJob</code>
 * . The use of a "." separator for elements is deliberate, since it is a "safe"
 * character in a <a href="http://www.w3.org/Addressing/URL">URL</a>.
 * 
 * 
 * @author Dave Syer
 * 
 */
public abstract class AbstractGroupAwareJobFactory implements JobFactory {
	
	/**
	 * 
	 */
	private static final String SEPARATOR = ".";

	private final String groupName;

	private final String jobName;

	/**
	 * Create a job factory for a job in no particular group.
	 * 
	 * @param jobName the name of the job
	 */
	public AbstractGroupAwareJobFactory(String jobName) {
		this(null, jobName);
	}

	/**
	 * Create a job factory for a job in the group provided. The {@link Job}
	 * eventually returned from {@link #createJob()} will have a name composed
	 * of the group and job names together.
	 * 
	 * @param groupName the name of the group
	 * @param jobName the name of the job
	 */
	public AbstractGroupAwareJobFactory(String groupName, String jobName) {
		this.groupName = groupName;
		this.jobName = jobName;
	}

	/**
	 * The main factory method. Delegates to {@link #doCreateJob(String)} to
	 * create a delegate, which is then wrapped to give a job with the same
	 * functionality but a composite name (if the group is specified).
	 * 
	 * @see org.springframework.batch.core.configuration.JobFactory#createJob()
	 */
	public final Job createJob() {
		Job job = doCreateJob(jobName);
		return groupName == null ? job : new GroupAwareJob(groupName, job);
	}

	/**
	 * Extension point for concrete subclasses.
	 * 
	 * @return a Job from the provided name
	 */
	protected abstract Job doCreateJob(String jobName);

	/**
	 * Return the bean name of the job in the application context. N.B. this is
	 * usually the name of the job as well, but it needn't be. The important
	 * thing is that the job can be located by this name.
	 * 
	 * @see org.springframework.batch.core.configuration.JobFactory#getJobName()
	 */
	public final String getJobName() {
		return groupName == null ? jobName : groupName + SEPARATOR + jobName;
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static class GroupAwareJob implements Job {

		private final Job delegate;

		private final String groupName;

		/**
		 * @param groupName
		 * @param delegate
		 */
		public GroupAwareJob(String groupName, Job delegate) {
			super();
			this.groupName = groupName;
			this.delegate = delegate;
		}

		public void execute(JobExecution execution) {
			delegate.execute(execution);
		}

		/**
		 * Concatenates the group name and the delegate job name (joining with a
		 * "$").
		 * 
		 * @see org.springframework.batch.core.Job#getName()
		 */
		public String getName() {
			return groupName + SEPARATOR + delegate.getName();
		}

		public boolean isRestartable() {
			return delegate.isRestartable();
		}

		public JobParametersIncrementer getJobParametersIncrementer() {
			return delegate.getJobParametersIncrementer();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof GroupAwareJob) {
				return ((GroupAwareJob) obj).delegate.equals(delegate);
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

	}

}