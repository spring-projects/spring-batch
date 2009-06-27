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
package org.springframework.batch.item.file.transform;

import java.util.Collection;

/**
 * {@link FieldExtractor} that just returns the original item. If the item is an
 * array or collection it will be returned as is, otherwise it is wrapped in a
 * single element array.
 * 
 * @author Dave Syer
 * 
 */
public class PassThroughFieldExtractor<T> implements FieldExtractor<T> {

	/**
	 * @param item the object to convert
	 * @return an array of objects as close as possible to the original item
	 */
	public Object[] extract(T item) {

		if (item.getClass().isArray()) {
			return (Object[]) item;
		}

		if (item instanceof Collection<?>) {
			return ((Collection<?>) item).toArray();
		}

		if (item instanceof FieldSet) {
			return ((FieldSet) item).getValues();
		}

		return new Object[] { item };

	}

}
