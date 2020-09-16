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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;

/**
 * In-memory implementation of {@link JobExecutionDao}.
 * 
 * @deprecated as of v4.3 in favor of using the {@link JdbcJobExecutionDao}
 * with an in-memory database. Scheduled for removal in v5.0.
 */
@Deprecated
public class MapJobExecutionDao implements JobExecutionDao {

	// JDK6 Make this into a ConcurrentSkipListMap: adds and removes tend to be very near the front or back
	private final ConcurrentMap<Long, JobExecution> executionsById = new ConcurrentHashMap<>();

	private final AtomicLong currentId = new AtomicLong(0L);

	public void clear() {
		executionsById.clear();
	}

	private static JobExecution copy(JobExecution original) {
		JobExecution copy = (JobExecution) SerializationUtils.deserialize(SerializationUtils.serialize(original));
		return copy;
	}

	@Override
	public void saveJobExecution(JobExecution jobExecution) {
		Assert.isTrue(jobExecution.getId() == null, "jobExecution id is not null");
		Long newId = currentId.getAndIncrement();
		jobExecution.setId(newId);
		jobExecution.incrementVersion();
		executionsById.put(newId, copy(jobExecution));
	}

	@Override
	public List<JobExecution> findJobExecutions(JobInstance jobInstance) {
		List<JobExecution> executions = new ArrayList<>();
		for (JobExecution exec : executionsById.values()) {
			if (exec.getJobInstance().equals(jobInstance)) {
				executions.add(copy(exec));
			}
		}
		Collections.sort(executions, new Comparator<JobExecution>() {

			@Override
			public int compare(JobExecution e1, JobExecution e2) {
				long result = (e1.getId() - e2.getId());
				if (result > 0) {
					return -1;
				}
				else if (result < 0) {
					return 1;
				}
				else {
					return 0;
				}
			}
		});
		return executions;
	}

	@Override
	public void updateJobExecution(JobExecution jobExecution) {
		Long id = jobExecution.getId();
		Assert.notNull(id, "JobExecution is expected to have an id (should be saved already)");
		JobExecution persistedExecution = executionsById.get(id);
		Assert.notNull(persistedExecution, "JobExecution must already be saved");

		synchronized (jobExecution) {
			if (!persistedExecution.getVersion().equals(jobExecution.getVersion())) {
				throw new OptimisticLockingFailureException("Attempt to update job execution id=" + id
						+ " with wrong version (" + jobExecution.getVersion() + "), where current version is "
						+ persistedExecution.getVersion());
			}
			jobExecution.incrementVersion();
			executionsById.put(id, copy(jobExecution));
		}
	}

	@Nullable
	@Override
	public JobExecution getLastJobExecution(@Nullable JobInstance jobInstance) {
		JobExecution lastExec = null;
		for (JobExecution exec : executionsById.values()) {
			if (!exec.getJobInstance().equals(jobInstance)) {
				continue;
			}
			if (lastExec == null) {
				lastExec = exec;
			}
			if (lastExec.getCreateTime().before(exec.getCreateTime())) {
				lastExec = exec;
			}
		}
		return copy(lastExec);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.repository.dao.JobExecutionDao#
	 * findRunningJobExecutions(java.lang.String)
	 */
	@Override
	public Set<JobExecution> findRunningJobExecutions(String jobName) {
		Set<JobExecution> result = new HashSet<>();
		for (JobExecution exec : executionsById.values()) {
			if (!exec.getJobInstance().getJobName().equals(jobName) || !exec.isRunning()) {
				continue;
			}
			result.add(copy(exec));
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.repository.dao.JobExecutionDao#getJobExecution
	 * (java.lang.Long)
	 */
	@Override
	@Nullable
	public JobExecution getJobExecution(Long executionId) {
		return copy(executionsById.get(executionId));
	}

	@Override
	public void synchronizeStatus(JobExecution jobExecution) {
		JobExecution saved = getJobExecution(jobExecution.getId());
		if (saved.getVersion().intValue() != jobExecution.getVersion().intValue()) {
			jobExecution.upgradeStatus(saved.getStatus());
			jobExecution.setVersion(saved.getVersion());
		}
	}
}
