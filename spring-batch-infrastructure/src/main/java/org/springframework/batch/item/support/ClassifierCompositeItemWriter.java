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

package org.springframework.batch.item.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.classify.Classifier;
import org.springframework.batch.classify.ClassifierSupport;
import org.springframework.batch.item.ItemWriter;

/**
 * Calls one of a collection of ItemWriters for each item, based on a router
 * pattern implemented through the provided {@link Classifier}.
 * 
 * The implementation is thread-safe if all delegates are thread-safe.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class ClassifierCompositeItemWriter<T> implements ItemWriter<T> {

	private Classifier<T, ItemWriter<? super T>> classifier = new ClassifierSupport<T, ItemWriter<? super T>>(null);

	/**
	 * @param classifier the classifier to set
	 */
	public void setClassifier(Classifier<T, ItemWriter<? super T>> classifier) {
		this.classifier = classifier;
	}

	/**
	 * Delegates to injected {@link ItemWriter} instances according to their
	 * classification by the {@link Classifier}.
	 */
	public void write(List<? extends T> items) throws Exception {

		Map<ItemWriter<? super T>, List<T>> map = new HashMap<ItemWriter<? super T>, List<T>>();

		for (T item : items) {
			ItemWriter<? super T> key = classifier.classify(item);
			if (!map.containsKey(key)) {
				map.put(key, new ArrayList<T>());
			}
			map.get(key).add(item);
		}

		for (ItemWriter<? super T> writer : map.keySet()) {
			writer.write(map.get(writer));
		}

	}

}
