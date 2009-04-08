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
package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

/**
 * Interface allowing for programmatic access to the decision on what the status
 * of a flow should be.  For example, if some condition that's stored in the 
 * database indicates that the job should stop for a manual check, a decider
 * implementation could check that value to determine the status of the flow. 
 * 
 * @author Dave Syer
 * @since 2.0
 */
public interface JobExecutionDecider {

	/**
	 * Strategy for branching an execution based on the state of an ongoing
	 * {@link JobExecution}. The return value will be used as a status to
	 * determine the next step in the job.
	 * 
	 * @param jobExecution a job execution
	 * @param stepExecution the latest step execution (may be null)
	 * @return the exit status code
	 */
	FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution);

}
