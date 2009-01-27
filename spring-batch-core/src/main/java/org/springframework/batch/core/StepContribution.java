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
package org.springframework.batch.core;

import java.io.Serializable;

/**
 * Represents a contribution to a {@link StepExecution}, buffering changes until
 * they can be applied at a chunk boundary.
 * 
 * @author Dave Syer
 * 
 */
public class StepContribution implements Serializable {

	private volatile int readCount = 0;

	private volatile int writeCount = 0;

	private volatile int filterCount = 0;

	private final int parentSkipCount;

	private volatile int readSkipCount;

	private volatile int writeSkipCount;

	private volatile int processSkipCount;

	private ExitStatus exitStatus = ExitStatus.EXECUTING;

	/**
	 * @param execution
	 */
	public StepContribution(StepExecution execution) {
		this.parentSkipCount = execution.getSkipCount();
	}

	/**
	 * Set the {@link ExitStatus} for this contribution.
	 * 
	 * @param status
	 */
	public void setExitStatus(ExitStatus status) {
		this.exitStatus = status;
	}

	/**
	 * Public getter for the status.
	 * 
	 * @return the {@link ExitStatus} for this contribution
	 */
	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	/**
	 * Increment the counter for the number of items processed.
	 */
	public void incrementFilterCount(int count) {
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
	 */
	public void incrementWriteCount(int count) {
		writeCount += count;
	}

	/**
	 * Public access to the read counter.
	 * 
	 * @return the item counter.
	 */
	public int getReadCount() {
		return readCount;
	}

	/**
	 * Public access to the write counter.
	 * 
	 * @return the item counter.
	 */
	public int getWriteCount() {
		return writeCount;
	}

	/**
	 * Public getter for the filter counter.
	 * @return the filter counter
	 */
	public int getFilterCount() {
		return filterCount;
	}

	/**
	 * @return the sum of skips accumulated in the parent {@link StepExecution}
	 * and this <code>StepContribution</code>.
	 */
	public int getStepSkipCount() {
		return readSkipCount + writeSkipCount + processSkipCount + parentSkipCount;
	}

	/**
	 * @return the number of skips collected in this
	 * <code>StepContribution</code> (not including skips accumulated in the
	 * parent {@link StepExecution}).
	 */
	public int getSkipCount() {
		return readSkipCount + writeSkipCount + processSkipCount;
	}

	/**
	 * Increment the read skip count for this contribution
	 */
	public void incrementReadSkipCount() {
		readSkipCount++;
	}

	/**
	 * Increment the read skip count for this contribution
	 */
	public void incrementReadSkipCount(int count) {
		readSkipCount += count;
	}

	/**
	 * Increment the write skip count for this contribution
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
	 * @return the read skip count
	 */
	public int getReadSkipCount() {
		return readSkipCount;
	}

	/**
	 * @return the write skip count
	 */
	public int getWriteSkipCount() {
		return writeSkipCount;
	}

	/**
	 * Public getter for the process skip count.
	 * @return the process skip count
	 */
	public int getProcessSkipCount() {
		return processSkipCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
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
		if (!(obj instanceof StepContribution)) {
			return false;
		}
		StepContribution other = (StepContribution) obj;
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
