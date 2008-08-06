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
package org.springframework.batch.core.explore;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;

/**
 * @author Dave Syer
 * 
 */
public interface BatchMetaDataExplorer {

	int countJobExecutionsByStatus(EnumSet<BatchStatus> statuses);

	/**
	 * @param statuses the status values to search
	 * @param start the start record (defaults to 0)
	 * @param count the maximum number of objects to return 
	 * @return the {@link JobExecution} objects that have the provided status,
	 * sorted in reverse order by start time.
	 */
	Collection<JobExecution> findJobExecutionsByStatus(EnumSet<BatchStatus> statuses, int start, int count);
	
	int countJobExecutionsByDate(Date from, Date to);

	Collection<JobExecution> findJobExecutionsByDate(Date from, Date to, int start, int count);

	Collection<JobInstance> findJobInstancesByJobName(String jobName);

	Collection<JobExecution> getJobExecutionsForJobInstance(JobInstance jobInstance);

}
