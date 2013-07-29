/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.jsr.tck.spi;

import javax.batch.operations.JobOperator;

import com.ibm.jbatch.tck.spi.JobExecutionWaiter;
import com.ibm.jbatch.tck.spi.JobExecutionWaiterFactory;

/**
 * A factory to create instances of the {@link JobExecutionWaiter}.
 * 
 * @author Michael Minella
 */
public class SpringJobExcutionWaiterFactory implements
JobExecutionWaiterFactory {

	// job timeout in milliseconds
	private long timeout = 5000;

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.tck.spi.JobExecutionWaiterFactory#createWaiter(long, javax.batch.operations.JobOperator, long)
	 */
	@Override
	public JobExecutionWaiter createWaiter(long executionId, JobOperator jobOp,
			long sleepTime) {
		return new SpringJobExecutionWaiter(jobOp, sleepTime, timeout, executionId);
	}
}
