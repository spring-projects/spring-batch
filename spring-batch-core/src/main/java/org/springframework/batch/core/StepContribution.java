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

/**
 * Represents a contribution to a {@link StepExecution}, buffering changes
 * until they can be applied at a chunk boundary.
 * 
 * @author Dave Syer
 * 
 */
public class StepContribution {

	private volatile int itemCount = 0;

	private final int parentSkipCount;

	private volatile int commitCount;

	private volatile int readSkipCount;

	private volatile int writeSkipCount;

	private volatile int uncommitedReadSkipCount;

	/**
	 * @param execution
	 */
	public StepContribution(StepExecution execution) {
		this.parentSkipCount = execution.getSkipCount();
	}

	/**
	 * Increment the counter for the number of items processed.
	 */
	public void incrementItemCount() {
		itemCount++;
	}

	/**
	 * Public access to the item counter.
	 * 
	 * @return the item counter.
	 */
	public int getItemCount() {
		return itemCount;
	}

	/**
	 * Increment the commit counter.
	 */
	public void incrementCommitCount() {
		commitCount++;
	}

	/**
	 * Public getter for the commit counter.
	 * @return the commitCount
	 */
	public int getCommitCount() {
		return commitCount;
	}

	/**
	 * @return the sum of skips accumulated in the parent {@link StepExecution}
	 * and this <code>StepContribution</code>, including uncommitted read
	 * skips.
	 */
	public int getStepSkipCount() {
		return uncommitedReadSkipCount + readSkipCount + writeSkipCount + parentSkipCount;
	}

	/**
	 * @return the number of skips collected in this
	 * <code>StepContribution</code> (not including skips accumulated in the
	 * parent {@link StepExecution}.
	 */
	public int getSkipCount() {
		return readSkipCount + writeSkipCount;
	}

	/**
	 * Increment the read skip count for this contribution
	 */
	public void incrementReadsSkipCount() {
		readSkipCount++;
	}

	/**
	 * Increment the write skip count for this contribution
	 */
	public void incrementWriteSkipCount() {
		writeSkipCount++;
	}

	/**
	 * Increment the counter for temporary skipped reads
	 */
	public void incrementTemporaryReadSkipCount() {
		uncommitedReadSkipCount++;
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
	 * Add the temporary read skip count to read skip count and reset the
	 * temporary counter.
	 */
	public void combineSkipCounts() {
		readSkipCount += uncommitedReadSkipCount;
		uncommitedReadSkipCount = 0;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[StepContribution: items=" + itemCount + ", commits=" + commitCount + ", uncommitedReadSkips="
				+ uncommitedReadSkipCount + ", readSkips=" + readSkipCount + "writeSkips=" + writeSkipCount + "]";
	}

}
