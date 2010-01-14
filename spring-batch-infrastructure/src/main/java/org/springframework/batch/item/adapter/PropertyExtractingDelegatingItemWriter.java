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

package org.springframework.batch.item.adapter;

import java.util.Arrays;
import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.Assert;

/**
 * Delegates processing to a custom method - extracts property values from item
 * object and uses them as arguments for the delegate method.
 * 
 * @see ItemWriterAdapter
 * 
 * @author Robert Kasanicky
 */
public class PropertyExtractingDelegatingItemWriter<T> extends AbstractMethodInvokingDelegator<T> implements
		ItemWriter<T> {

	private String[] fieldsUsedAsTargetMethodArguments;

	/**
	 * Extracts values from item's fields named in
	 * fieldsUsedAsTargetMethodArguments and passes them as arguments to the
	 * delegate method.
	 */
	public void write(List<? extends T> items) throws Exception {
		for (T item : items) {

			// helper for extracting property values from a bean
			BeanWrapper beanWrapper = new BeanWrapperImpl(item);

			Object[] methodArguments = new Object[fieldsUsedAsTargetMethodArguments.length];
			for (int i = 0; i < fieldsUsedAsTargetMethodArguments.length; i++) {
				methodArguments[i] = beanWrapper.getPropertyValue(fieldsUsedAsTargetMethodArguments[i]);
			}

			invokeDelegateMethodWithArguments(methodArguments);

		}
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notEmpty(fieldsUsedAsTargetMethodArguments);
	}

	/**
	 * @param fieldsUsedAsMethodArguments the values of the these item's fields
	 * will be used as arguments for the delegate method. Nested property values
	 * are supported, e.g. <code>address.city</code>
	 */
	public void setFieldsUsedAsTargetMethodArguments(String[] fieldsUsedAsMethodArguments) {
		this.fieldsUsedAsTargetMethodArguments = Arrays.asList(fieldsUsedAsMethodArguments).toArray(
				new String[fieldsUsedAsMethodArguments.length]);
	}

}
