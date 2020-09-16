/*
 * Copyright 2006-2020 the original author or authors.
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
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobInstanceDao}.
 * 
 * @deprecated as of v4.3 in favor of using the {@link JdbcJobInstanceDao}
 * with an in-memory database. Scheduled for removal in v5.0.
 */
@Deprecated
public class MapJobInstanceDao implements JobInstanceDao {
	private static final String STAR_WILDCARD = "\\*";
	private static final String STAR_WILDCARD_PATTERN = ".*";

	// JDK6 Make a ConcurrentSkipListSet: tends to add on end
	private final Map<String, JobInstance> jobInstances = new ConcurrentHashMap<>();

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
		jobInstances.put(jobName + "|" + jobKeyGenerator.generateKey(jobParameters), jobInstance);

		return jobInstance;
	}

	@Override
	@Nullable
	public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
		return jobInstances.get(jobName + "|" + jobKeyGenerator.generateKey(jobParameters));
	}

	@Override
	@Nullable
	public JobInstance getJobInstance(@Nullable Long instanceId) {
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
		List<String> result = new ArrayList<>();
		for (Map.Entry<String, JobInstance> instanceEntry : jobInstances.entrySet()) {
			result.add(instanceEntry.getValue().getJobName());
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public List<JobInstance> getJobInstances(String jobName, int start, int count) {
		List<JobInstance> result = new ArrayList<>();
		for (Map.Entry<String, JobInstance> instanceEntry : jobInstances.entrySet()) {
			JobInstance instance = instanceEntry.getValue();
			if (instance.getJobName().equals(jobName)) {
				result.add(instance);
			}
		}

		sortDescending(result);

		return subset(result, start, count);
	}

	@Override
	@Nullable
	public JobInstance getLastJobInstance(String jobName) {
		List<JobInstance> jobInstances = getJobInstances(jobName, 0, 1);
		return jobInstances.isEmpty() ? null : jobInstances.get(0);
	}

	@Override
	@Nullable
	public JobInstance getJobInstance(JobExecution jobExecution) {
		return jobExecution.getJobInstance();
	}

	@Override
	public int getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException {
		int count = 0;

		for (Map.Entry<String, JobInstance> instanceEntry : jobInstances.entrySet()) {
			String key = instanceEntry.getKey();
			String curJobName = key.substring(0, key.lastIndexOf("|"));

			if(curJobName.equals(jobName)) {
				count++;
			}
		}

		if(count == 0) {
			throw new NoSuchJobException("No job instances for job name " + jobName + " were found");
		} else {
			return count;
		}
	}

	@Override
	public List<JobInstance> findJobInstancesByName(String jobName, int start, int count) {
		List<JobInstance> result = new ArrayList<>();
		String convertedJobName = jobName.replaceAll(STAR_WILDCARD, STAR_WILDCARD_PATTERN);

		for (Map.Entry<String, JobInstance> instanceEntry : jobInstances.entrySet()) {
			JobInstance instance = instanceEntry.getValue();

			if(instance.getJobName().matches(convertedJobName)) {
				result.add(instance);
			}
		}

		sortDescending(result);

		return subset(result, start, count);
	}

	private void sortDescending(List<JobInstance> result) {
		Collections.sort(result, new Comparator<JobInstance>() {
			@Override
			public int compare(JobInstance o1, JobInstance o2) {
				return Long.signum(o2.getId() - o1.getId());
			}
		});
	}

	private List<JobInstance> subset(List<JobInstance> jobInstances, int start, int count) {
		int startIndex = Math.min(start, jobInstances.size());
		int endIndex = Math.min(start + count, jobInstances.size());

		return jobInstances.subList(startIndex, endIndex);
	}
}
