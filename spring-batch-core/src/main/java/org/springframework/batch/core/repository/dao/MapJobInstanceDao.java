/*
 * Copyright 2006-2013 the original author or authors.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.JobParameters;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobInstanceDao}.
 */
public class MapJobInstanceDao implements JobInstanceDao {

	// JDK6 Make a ConcurrentSkipListSet: tends to add on end
	private final Map<String, JobInstance> jobInstances = new ConcurrentHashMap<String, JobInstance>();
	//	private final Set<JobInstance> jobInstances = new CopyOnWriteArraySet<JobInstance>();

	private JobKeyGenerator<JobParameters> jobKeyGenerator = new DefaultJobKeyGenerator();

	private final AtomicLong currentId = new AtomicLong(0L);

	public void clear() {
		jobInstances.clear();
	}

	@Override
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {

		Assert.state(getJobInstance(jobName, jobParameters) == null, "JobInstance must not already exist");

		JobInstance jobInstance = new JobInstance(currentId.getAndIncrement(), jobName);
		jobInstance.incrementVersion();
		jobInstances.put(jobName + jobKeyGenerator.generateKey(jobParameters), jobInstance);

		return jobInstance;
	}

	@Override
	public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
		return jobInstances.get(jobName + jobKeyGenerator.generateKey(jobParameters));
	}

	@Override
	public JobInstance getJobInstance(Long instanceId) {
		for (Map.Entry<String, JobInstance> instanceEntry : jobInstances.entrySet()) {
			JobInstance instance = instanceEntry.getValue();
			if (instance.getId().equals(instanceId)) {
				return instance;
			}
		}
		return null;
	}

	@Override
	public List<String> getJobNames() {
		List<String> result = new ArrayList<String>();
		for (Map.Entry<String, JobInstance> instanceEntry : jobInstances.entrySet()) {
			result.add(instanceEntry.getValue().getJobName());
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public List<JobInstance> getJobInstances(String jobName, int start, int count) {
		List<JobInstance> result = new ArrayList<JobInstance>();
		for (Map.Entry<String, JobInstance> instanceEntry : jobInstances.entrySet()) {
			JobInstance instance = instanceEntry.getValue();
			if (instance.getJobName().equals(jobName)) {
				result.add(instance);
			}
		}
		Collections.sort(result, new Comparator<JobInstance>() {
			// sort by ID descending
			@Override
			public int compare(JobInstance o1, JobInstance o2) {
				return Long.signum(o2.getId() - o1.getId());
			}
		});

		int startIndex = Math.min(start, result.size());
		int endIndex = Math.min(start + count, result.size());
		return result.subList(startIndex, endIndex);
	}

	@Override
	public JobInstance getJobInstance(JobExecution jobExecution) {
		return jobExecution.getJobInstance();
	}

	@Override
	public List<JobInstance> getJobInstancesByName(String jobName, int start, int count) {
		//no 'wildcard' implementation for map
		return getJobInstances(jobName,start,count);
	}

}
