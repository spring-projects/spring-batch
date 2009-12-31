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
package org.springframework.batch.core.step.skip;

import org.springframework.batch.classify.SubclassClassifier;

/**
 * A {@link SkipPolicy} that depends on an exception classifier to make its
 * decision, and then delegates to the classifier result.
 * 
 * @author Dave Syer
 * 
 * @see SubclassClassifier
 */
public class ExceptionClassifierSkipPolicy implements SkipPolicy {

	private SubclassClassifier<Throwable, SkipPolicy> classifier;

	/**
	 * The classifier that will be used to choose a delegate policy.
	 * 
	 * @param classifier the classifier to use to choose a delegate policy
	 */
	public void setExceptionClassifier(SubclassClassifier<Throwable, SkipPolicy> classifier) {
		this.classifier = classifier;
	}

	/**
	 * Consult the classifier and find a delegate policy, and then use that to
	 * determine the outcome.
	 * 
	 * @param t the throwable to consider
	 * @param skipCount the current skip count
	 * @return true if the exception can be skipped
	 * @throws SkipLimitExceededException if a limit is exceeded
	 */
	public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
		return classifier.classify(t).shouldSkip(t, skipCount);
	}

}
