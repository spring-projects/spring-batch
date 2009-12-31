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

import java.util.Map;

import org.springframework.batch.classify.Classifier;
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
	 * Setter for policy map. This property should not be changed dynamically -
	 * set it once, e.g. in configuration, and then don't change it during a
	 * running application. Either this property or the exception classifier
	 * directly should be set, but not both.
	 * 
	 * @param policyMap a map of String to {@link SkipPolicy} that will be used
	 * to create a {@link Classifier} to locate a policy.
	 */
	public void setPolicyMap(Map<Class<? extends Throwable>, SkipPolicy> policyMap) {
		SubclassClassifier<Throwable, SkipPolicy> subclassClassifier = new SubclassClassifier<Throwable, SkipPolicy>(
				policyMap, new NeverSkipItemSkipPolicy());
		this.classifier = subclassClassifier;
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
