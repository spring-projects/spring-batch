/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.batch.item.support;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.classify.Classifier;
import org.springframework.classify.ClassifierSupport;
import org.springframework.lang.Nullable;

/**
 * Calls one of a collection of ItemProcessors, based on a router
 * pattern implemented through the provided {@link Classifier}.
 * 
 * Note the user is responsible for injecting a {@link Classifier}
 * that returns an ItemProcessor that conforms to the declared input and output types.
 * 
 * @author Jimmy Praet
 * @since 3.0
 */
public class ClassifierCompositeItemProcessor<I,O> implements ItemProcessor<I, O> {

	private Classifier<? super I, ItemProcessor<?, ? extends O>> classifier = 
			new ClassifierSupport<> (null);

	/**
	 * Establishes the classifier that will determine which {@link ItemProcessor} to use.
	 * @param classifier the {@link Classifier} to set
	 */
	public void setClassifier(Classifier<? super I, ItemProcessor<?, ? extends O>> classifier) {
		this.classifier = classifier;
	}
	
	/**
	 * Delegates to injected {@link ItemProcessor} instances according to the
	 * classification by the {@link Classifier}.
	 */
	@Nullable
	@Override
	public O process(I item) throws Exception {
		return processItem(classifier.classify(item), item);
	}
	
    /* 
     * Helper method to work around wildcard capture compiler error: see https://docs.oracle.com/javase/tutorial/java/generics/capture.html
     * The method process(capture#4-of ?) in the type ItemProcessor<capture#4-of ?,capture#5-of ? extends O> is not applicable for the arguments (I)
     */
    @SuppressWarnings("unchecked")
	private <T> O processItem(ItemProcessor<T, ? extends O> processor, I input) throws Exception {
    	return processor.process((T) input);
    }	

}
