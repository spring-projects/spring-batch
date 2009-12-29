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

package org.springframework.batch.integration.chunk;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.batch.core.StepContribution;

public class ChunkRequest<T> implements Serializable {

	private final Long jobId;
	private final Collection<? extends T> items;
	private final StepContribution stepContribution;

	public ChunkRequest(Collection<? extends T> items, Long jobId, StepContribution stepContribution) {
		this.items = items;
		this.jobId = jobId;
		this.stepContribution = stepContribution;
	}

	public Long getJobId() {
		return jobId;
	}

	public Collection<? extends T> getItems() {
		return items;
	}
	
	/**
	 * @return the {@link StepContribution} for this chunk
	 */
	public StepContribution getStepContribution() {
		return stepContribution;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName()+": jobId="+jobId+", contribution="+stepContribution+", item count="+items.size();
	}

}
