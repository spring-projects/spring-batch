/*
 * Copyright 2006-2010 the original author or authors.
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

import java.util.Collection;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;

/**
 * A source of {@link StepContribution} instances that can be aggregated and used to update an ongoing
 * {@link StepExecution}.
 * 
 * @author Dave Syer
 * 
 */
public interface StepContributionSource {

	/**
	 * Get the currently available contributions and drain the source. The next call would return an empty collection,
	 * unless new contributions have arrived.
	 * 
	 * @return a collection of {@link StepContribution} instances
	 */
	Collection<StepContribution> getStepContributions();

}
