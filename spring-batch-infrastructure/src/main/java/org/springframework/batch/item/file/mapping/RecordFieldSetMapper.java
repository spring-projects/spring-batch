/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.item.file.mapping;

import java.lang.reflect.Constructor;

import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;

/**
 * This is a {@link FieldSetMapper} that supports Java records mapping
 * (requires JKD 14 or higher).  It uses the record's canonical constructor
 * to map components with the same name as tokens in the {@link FieldSet}.
 * 
 * @param <T> type of mapped items
 * @author Mahmoud Ben Hassine
 * @since 4.3
 */
public class RecordFieldSetMapper<T> implements FieldSetMapper<T> {

	private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
	private final Constructor<T> mappedConstructor;
	private String[] constructorParameterNames;
	private Class<?>[] constructorParameterTypes;

	/**
	 * Create a new {@link RecordFieldSetMapper}.
	 * 
	 * @param targetType type of mapped items
	 */
	public RecordFieldSetMapper(Class<T> targetType) {
		this(targetType, new DefaultConversionService());
	}

	/**
	 * Create a new {@link RecordFieldSetMapper}.
	 * 
	 * @param targetType type of mapped items
	 * @param conversionService service to use to convert raw data to typed fields
	 */
	public RecordFieldSetMapper(Class< T> targetType, ConversionService conversionService) {
		this.typeConverter.setConversionService(conversionService);
		this.mappedConstructor = BeanUtils.getResolvableConstructor(targetType);
		if (this.mappedConstructor.getParameterCount() > 0) {
			this.constructorParameterNames = BeanUtils.getParameterNames(this.mappedConstructor);
			this.constructorParameterTypes = this.mappedConstructor.getParameterTypes();
		}
	}

	@Override
	public T mapFieldSet(FieldSet fieldSet) {
		Assert.isTrue(fieldSet.getFieldCount() == this.constructorParameterNames.length,
				"Fields count must be equal to record components count");
		Assert.isTrue(fieldSet.hasNames(), "Field names must specified");
		Object[] args = new Object[0];
		if (this.constructorParameterNames != null && this.constructorParameterTypes != null) {
			args = new Object[this.constructorParameterNames.length];
			for (int i = 0; i < args.length; i++) {
				String name = this.constructorParameterNames[i];
				Class<?> type = this.constructorParameterTypes[i];
				args[i] = this.typeConverter.convertIfNecessary(fieldSet.readRawString(name), type);
			}
		}
		return BeanUtils.instantiateClass(this.mappedConstructor, args);
	}
}
