/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.support.builder;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.ClassifierCompositeItemProcessor;
import org.springframework.classify.Classifier;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified {@link ClassifierCompositeItemProcessor}.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class ClassifierCompositeItemProcessorBuilder<I, O> {

	private Classifier<? super I, ItemProcessor<?, ? extends O>> classifier;

	/**
	 * Establishes the classifier that will determine which {@link ItemProcessor} to use.
	 * @param classifier the classifier to set
	 * @return this instance for method chaining
	 * @see ClassifierCompositeItemProcessor#setClassifier(Classifier)
	 */
	public ClassifierCompositeItemProcessorBuilder<I, O> classifier(Classifier<? super I, ItemProcessor<?, ? extends O>> classifier) {
		this.classifier = classifier;

		return this;
	}

	/**
	 * Returns a fully constructed {@link ClassifierCompositeItemProcessor}.
	 *
	 * @return a new {@link ClassifierCompositeItemProcessor}
	 */
	public ClassifierCompositeItemProcessor<I, O> build() {
		Assert.notNull(classifier, "A classifier is required.");

		ClassifierCompositeItemProcessor<I, O> processor = new ClassifierCompositeItemProcessor<>();
		processor.setClassifier(this.classifier);
		return processor;
	}
}
