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

package org.springframework.batch.sample.iosample.internal;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

/**
 * This class is used to delegate to a {@link FieldSetMapper} based on one field
 * in the {@link FieldSet}.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class PrefixMatchingCompositeFieldSetMapper<T> implements FieldSetMapper<T> {

	private Map<String, FieldSetMapper<T>> mappers = new HashMap<String, FieldSetMapper<T>>();

	public T mapFieldSet(FieldSet fieldSet) {
		String prefix = fieldSet.readString("prefix");
		return this.mappers.get(prefix).mapFieldSet(fieldSet);
	}

	public void setMappers(Map<String, FieldSetMapper<T>> mappers) {
		this.mappers = mappers;
	}
}
