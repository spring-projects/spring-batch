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

package org.springframework.batch.repeat.policy;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Very simple {@link CompletionPolicy} that bases its decision on the result of
 * a batch operation. If the result is null or not continuable according to the
 * {@link RepeatStatus} the batch is complete, otherwise not.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultResultCompletionPolicy extends CompletionPolicySupport {

	/**
	 * True if the result is null, or a {@link RepeatStatus} indicating
	 * completion.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(org.springframework.batch.repeat.RepeatContext,
	 *      RepeatStatus)
	 */
	public boolean isComplete(RepeatContext context, RepeatStatus result) {
		return (result == null || !result.isContinuable());
	}

	/**
	 * Always false.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(org.springframework.batch.repeat.RepeatContext)
	 */
	public boolean isComplete(RepeatContext context) {
		return false;
	}

}
