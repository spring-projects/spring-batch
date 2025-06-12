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
package org.springframework.batch.core.step;

import java.io.Serializable;

import org.springframework.batch.core.ExitStatus;

/**
 * Represents a contribution to a {@link StepExecution}, buffering changes until they can
 * be applied at a chunk boundary.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class StepContribution implements Serializable {

	private volatile long readCount = 0;

	private volatile long writeCount = 0;

	private volatile long filterCount = 0;

	private final long parentSkipCount;

	private volatile long readSkipCount;

	private volatile long writeSkipCount;

	private volatile long processSkipCount;

	private ExitStatus exitStatus = ExitStatus.EXECUTING;

	private final StepExecution stepExecution;

	/**
	 * @param execution {@link StepExecution} the stepExecution used to initialize
	 * {@code skipCount}.
	 */
	public StepContribution(StepExecution execution) {
		this.stepExecution = execution;
		this.parentSkipCount = execution.getSkipCount();
	}

	/**
	 * Set the {@link ExitStatus} for this contribution.
	 * @param status {@link ExitStatus} instance to be used to set the exit status.
	 */
	public void setExitStatus(ExitStatus status) {
		this.exitStatus = status;
	}

	/**
	 * Public getter for the {@code ExitStatus}.
	 * @return the {@link ExitStatus} for this contribution
	 */
	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	/**
	 * Increment the counter for the number of items processed.
	 * @param count The {@code long} amount to increment by.
	 */
	public void incrementFilterCount(long count) {
		filterCount += count;
	}

	/**
	 * Increment the counter for the number of items read.
	 */
	public void incrementReadCount() {
		readCount++;
	}

	/**
	 * Increment the counter for the number of items written.
	 * @param count The {@code long} amount to increment by.
	 */
	public void incrementWriteCount(long count) {
		writeCount += count;
	}

	/**
	 * Public access to the read counter.
	 * @return the read item counter.
	 */
	public long getReadCount() {
		return readCount;
	}

	/**
	 * Public access to the write counter.
	 * @return the write item counter.
	 */
	public long getWriteCount() {
		return writeCount;
	}

	/**
	 * Public getter for the filter counter.
	 * @return the filter counter.
	 */
	public long getFilterCount() {
		return filterCount;
	}

	/**
	 * @return the sum of skips accumulated in the parent {@link StepExecution} and this
	 * <code>StepContribution</code>.
	 */
	public long getStepSkipCount() {
		return readSkipCount + writeSkipCount + processSkipCount + parentSkipCount;
	}

	/**
	 * @return the number of skips collected in this <code>StepContribution</code> (not
	 * including skips accumulated in the parent {@link StepExecution}).
	 */
	public long getSkipCount() {
		return readSkipCount + writeSkipCount + processSkipCount;
	}

	/**
	 * Increment the read skip count for this contribution.
	 */
	public void incrementReadSkipCount() {
		readSkipCount++;
	}

	/**
	 * Increment the read skip count for this contribution.
	 * @param count The {@code long} amount to increment by.
	 */
	public void incrementReadSkipCount(long count) {
		readSkipCount += count;
	}

	/**
	 * Increment the write skip count for this contribution.
	 */
	public void incrementWriteSkipCount() {
		writeSkipCount++;
	}

	/**
	 *
	 */
	public void incrementProcessSkipCount() {
		processSkipCount++;
	}

	/**
	 * Public getter for the read skip count.
	 * @return the read skip count.
	 */
	public long getReadSkipCount() {
		return readSkipCount;
	}

	/**
	 * Public getter for the write skip count.
	 * @return the write skip count.
	 */
	public long getWriteSkipCount() {
		return writeSkipCount;
	}

	/**
	 * Public getter for the process skip count.
	 * @return the process skip count.
	 */
	public long getProcessSkipCount() {
		return processSkipCount;
	}

	/**
	 * Public getter for the parent step execution of this contribution.
	 * @return parent step execution of this contribution
	 */
	public StepExecution getStepExecution() {
		return stepExecution;
	}

	@Override
	public String toString() {
		return "[StepContribution: read=" + readCount + ", written=" + writeCount + ", filtered=" + filterCount
				+ ", readSkips=" + readSkipCount + ", writeSkips=" + writeSkipCount + ", processSkips="
				+ processSkipCount + ", exitStatus=" + exitStatus.getExitCode() + "]";
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StepContribution other)) {
			return false;
		}
		return toString().equals(other.toString());
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 11 + toString().hashCode() * 43;
	}

}
