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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.core.Entity;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.support.SerializationUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * In-memory implementation of {@link StepExecutionDao}.
 */
public class MapStepExecutionDao implements StepExecutionDao {

	private Map<Long, Map<Long, StepExecution>> executionsByJobExecutionId = new ConcurrentHashMap<Long, Map<Long,StepExecution>>();

	private Map<Long, StepExecution> executionsByStepExecutionId = new ConcurrentHashMap<Long, StepExecution>();

	private AtomicLong currentId = new AtomicLong();

	public void clear() {
		executionsByJobExecutionId.clear();
		executionsByStepExecutionId.clear();
	}

	private static StepExecution copy(StepExecution original) {
		return (StepExecution) SerializationUtils.deserialize(SerializationUtils.serialize(original));
	}

	private static void copy(final StepExecution sourceExecution, final StepExecution targetExecution) {
		// Cheaper than full serialization is a reflective field copy, which is
		// fine for volatile storage
		ReflectionUtils.doWithFields(StepExecution.class, new ReflectionUtils.FieldCallback() {
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				field.setAccessible(true);
				field.set(targetExecution, field.get(sourceExecution));
			}
		});
	}

	public void saveStepExecution(StepExecution stepExecution) {

		Assert.isTrue(stepExecution.getId() == null);
		Assert.isTrue(stepExecution.getVersion() == null);
		Assert.notNull(stepExecution.getJobExecutionId(), "JobExecution must be saved already.");

		Map<Long, StepExecution> executions = executionsByJobExecutionId.get(stepExecution.getJobExecutionId());
		if (executions == null) {
			executions = new ConcurrentHashMap<Long, StepExecution>();
			executionsByJobExecutionId.put(stepExecution.getJobExecutionId(), executions);
		}

		stepExecution.setId(currentId.incrementAndGet());
		stepExecution.incrementVersion();
		StepExecution copy = copy(stepExecution);
		executions.put(stepExecution.getId(), copy);
		executionsByStepExecutionId.put(stepExecution.getId(), copy);

	}

	public void updateStepExecution(StepExecution stepExecution) {

		Assert.notNull(stepExecution.getJobExecutionId());

		Map<Long, StepExecution> executions = executionsByJobExecutionId.get(stepExecution.getJobExecutionId());
		Assert.notNull(executions, "step executions for given job execution are expected to be already saved");

		final StepExecution persistedExecution = executionsByStepExecutionId.get(stepExecution.getId());
		Assert.notNull(persistedExecution, "step execution is expected to be already saved");

		synchronized (stepExecution) {
			if (!persistedExecution.getVersion().equals(stepExecution.getVersion())) {
				throw new OptimisticLockingFailureException("Attempt to update step execution id="
						+ stepExecution.getId() + " with wrong version (" + stepExecution.getVersion()
						+ "), where current version is " + persistedExecution.getVersion());
			}

			stepExecution.incrementVersion();
			StepExecution copy = new StepExecution(stepExecution.getStepName(), stepExecution.getJobExecution());
			copy(stepExecution, copy);
			executions.put(stepExecution.getId(), copy);
			executionsByStepExecutionId.put(stepExecution.getId(), copy);
		}
	}

	public StepExecution getStepExecution(JobExecution jobExecution, Long stepExecutionId) {
		return executionsByStepExecutionId.get(stepExecutionId);
	}

	public void addStepExecutions(JobExecution jobExecution) {
		Map<Long, StepExecution> executions = executionsByJobExecutionId.get(jobExecution.getId());
		if (executions == null || executions.isEmpty()) {
			return;
		}
		List<StepExecution> result = new ArrayList<StepExecution>(executions.values());
		Collections.sort(result, new Comparator<Entity>() {

			public int compare(Entity o1, Entity o2) {
				return Long.signum(o2.getId() - o1.getId());
			}
		});

		List<StepExecution> copy = new ArrayList<StepExecution>(result.size());
		for (StepExecution exec : result) {
			copy.add(copy(exec));
		}
		jobExecution.addStepExecutions(copy);
	}
}
