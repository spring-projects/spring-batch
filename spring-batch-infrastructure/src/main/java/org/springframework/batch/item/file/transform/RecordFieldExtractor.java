/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.batch.item.file.transform;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.util.Assert;

import org.jspecify.annotations.Nullable;

/**
 * This is a field extractor for a Java record. By default, it will extract all record
 * components, unless a subset is selected using {@link #setNames(String...)}.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class RecordFieldExtractor<T> implements FieldExtractor<T> {

	private List<String> names;

	private final Class<? extends T> targetType;

	private final RecordComponent[] recordComponents;

	public RecordFieldExtractor(Class<? extends T> targetType) {
		Assert.notNull(targetType, "target type must not be null");
		Assert.isTrue(targetType.isRecord(), "target type must be a record");
		this.targetType = targetType;
		this.recordComponents = this.targetType.getRecordComponents();
		this.names = getRecordComponentNames();
	}

	/**
	 * Set the names of record components to extract.
	 * @param names of record component to be extracted.
	 */
	public void setNames(String... names) {
		Assert.notNull(names, "Names must not be null");
		Assert.notEmpty(names, "Names must not be empty");
		validate(names);
		this.names = Arrays.stream(names).toList();
	}

	/**
	 * @see FieldExtractor#extract(Object)
	 */
	@Override
	public Object[] extract(T item) {
		List<Object> values = new ArrayList<>();
		for (String componentName : this.names) {
			RecordComponent recordComponent = getRecordComponentByName(componentName).orElseThrow();
			Object value;
			try {
				value = recordComponent.getAccessor().invoke(item);
				values.add(value);
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("Unable to extract value for record component " + componentName, e);
			}
		}
		return values.toArray();
	}

	private List<String> getRecordComponentNames() {
		return Arrays.stream(this.recordComponents).map(RecordComponent::getName).toList();
	}

	private void validate(String[] names) {
		for (String name : names) {
			if (getRecordComponentByName(name).isEmpty()) {
				throw new IllegalArgumentException(
						"Component '" + name + "' is not defined in record " + targetType.getName());
			}
		}
	}

	private Optional<RecordComponent> getRecordComponentByName(String name) {
		return Arrays.stream(this.recordComponents)
			.filter(recordComponent -> recordComponent.getName().equals(name))
			.findFirst();
	}

}
