/*
 * Copyright 2017-2019 the original author or authors.
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

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.classify.Classifier;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified ClassifierCompositeItemWriter.
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 *
 * @since 4.0
 */
public class ClassifierCompositeItemWriterBuilder<T> {

	private Classifier<T, ItemWriter<? super T>> classifier;

	/**
	 * Establish the classifier to be used for the selection of which {@link ItemWriter}
	 * to use.
	 *
	 * @param classifier the classifier to set
	 * @return this instance for method chaining
	 * @see org.springframework.batch.item.support.ClassifierCompositeItemWriter#setClassifier(Classifier)
	 */
	public ClassifierCompositeItemWriterBuilder<T> classifier(Classifier<T, ItemWriter<? super T>> classifier) {
		this.classifier = classifier;

		return this;
	}

	/**
	 * Returns a fully constructed {@link ClassifierCompositeItemWriter}.
	 *
	 * @return a new {@link ClassifierCompositeItemWriter}
	 */
	public ClassifierCompositeItemWriter<T> build() {
		Assert.notNull(classifier, "A classifier is required.");

		ClassifierCompositeItemWriter<T> writer = new ClassifierCompositeItemWriter<>();
		writer.setClassifier(this.classifier);
		return writer;
	}

}
