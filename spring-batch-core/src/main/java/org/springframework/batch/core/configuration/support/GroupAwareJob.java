/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * A {@link Job} that can optionally prepend a group name to another job's name, to make
 * it fit a naming convention for type or origin. For example, the source job might be
 * <code>overnightJob</code> and the group might be <code>financeDepartment</code>, which
 * would result in a {@link Job} with identical functionality but named
 * <code>financeDepartment.overnightJob</code> . The use of a "." separator for elements
 * is deliberate, since it is a "safe" character in a
 * <a href="https://www.w3.org/Addressing/URL">URL</a>.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class GroupAwareJob implements Job {

	/**
	 * The separator between group and delegate job names in the final name given to this
	 * job.
	 */
	private static final String SEPARATOR = ".";

	private final Job delegate;

	private final String groupName;

	/**
	 * Create a new {@link Job} with the delegate and no group name.
	 * @param delegate a delegate for the features of a regular Job
	 */
	public GroupAwareJob(Job delegate) {
		this(null, delegate);
	}

	/**
	 * Create a new {@link Job} with the given group name and delegate.
	 * @param groupName the group name to prepend (can be {@code null})
	 * @param delegate a delegate for the features of a regular Job
	 */
	public GroupAwareJob(@Nullable String groupName, Job delegate) {
		super();
		this.groupName = groupName;
		this.delegate = delegate;
	}

	@Override
	public void execute(JobExecution execution) {
		delegate.execute(execution);
	}

	/**
	 * Concatenates the group name and the delegate job name (joining with a ".").
	 *
	 * @see org.springframework.batch.core.Job#getName()
	 */
	@Override
	public String getName() {
		return groupName == null ? delegate.getName() : groupName + SEPARATOR + delegate.getName();
	}

	@Override
	public boolean isRestartable() {
		return delegate.isRestartable();
	}

	@Override
	@Nullable
	public JobParametersIncrementer getJobParametersIncrementer() {
		return delegate.getJobParametersIncrementer();
	}

	@Override
	public JobParametersValidator getJobParametersValidator() {
		return delegate.getJobParametersValidator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GroupAwareJob) {
			return ((GroupAwareJob) obj).delegate.equals(delegate);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public String toString() {
		return ClassUtils.getShortName(delegate.getClass()) + ": [name=" + getName() + "]";
	}

}
