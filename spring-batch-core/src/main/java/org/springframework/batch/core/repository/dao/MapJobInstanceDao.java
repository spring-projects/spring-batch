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

package org.springframework.batch.core.repository.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArraySet;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobInstanceDao}.
 */
public class MapJobInstanceDao implements JobInstanceDao {

	// JDK6 Make a ConcurrentSkipListSet: tends to add on end
	private final Set<JobInstance> jobInstances = new CopyOnWriteArraySet<JobInstance>();

	private final AtomicLong currentId = new AtomicLong(0L);

	public void clear() {
		jobInstances.clear();
	}

	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {

		Assert.state(getJobInstance(jobName, jobParameters) == null, "JobInstance must not already exist");

		JobInstance jobInstance = new JobInstance(currentId.getAndIncrement(), jobParameters, jobName);
		jobInstance.incrementVersion();
		jobInstances.add(jobInstance);

		return jobInstance;
	}

	public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {

		for (JobInstance instance : jobInstances) {
			if (instance.getJobName().equals(jobName) && instance.getJobParameters().equals(jobParameters)) {
				return instance;
			}
		}
		return null;

	}

	public JobInstance getJobInstance(Long instanceId) {
		for (JobInstance instance : jobInstances) {
			if (instance.getId().equals(instanceId)) {
				return instance;
			}
		}
		return null;
	}

	public List<String> getJobNames() {
		List<String> result = new ArrayList<String>();
		for (JobInstance instance : jobInstances) {
			result.add(instance.getJobName());
		}
		Collections.sort(result);
		return result;
	}

	public List<JobInstance> getJobInstances(String jobName, int start, int count) {
		List<JobInstance> result = new ArrayList<JobInstance>();
		for (JobInstance instance : jobInstances) {
			if (instance.getJobName().equals(jobName)) {
				result.add(instance);
			}
		}
		Collections.sort(result, new Comparator<JobInstance>() {
			// sort by ID descending
			public int compare(JobInstance o1, JobInstance o2) {
				return Long.signum(o2.getId() - o1.getId());
			}
		});

		int startIndex = Math.min(start, result.size());
		int endIndex = Math.min(start + count, result.size());
		return result.subList(startIndex, endIndex);
	}

	public JobInstance getJobInstance(JobExecution jobExecution) {
		return jobExecution.getJobInstance();
	}

}
