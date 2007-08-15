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

package org.springframework.batch.item.provider;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.item.ItemProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link ItemProvider} based on {@link FieldSetInputSource}. Not restartable
 * or transaction aware, but can be used by multiple concurrent threads, so
 * useful as a base class or for testing.
 * 
 * @author Rob Harrop
 * @author Dave Syer
 */
public abstract class AbstractFieldSetItemProvider extends AbstractItemProvider implements InitializingBean {

	protected FieldSetInputSource source;

	private Object mutex = new Object();

	/**
	 * Make sure mandatory properties are set.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(source);
	}

	/**
	 * Setter for the input source. Mandatory with no default.
	 * @param source
	 */
	public void setSource(FieldSetInputSource source) {
		this.source = source;
	}

	/**
	 * Get the next field set from the input source, and then call
	 * {@link #transform(FieldSet)} on the result. Synchronizes access to the
	 * input source using an internal mutex as a lock.
	 * 
	 * @see org.springframework.batch.item.ItemProvider#next()
	 */
	public final Object next() {
		FieldSet fieldSet;
		synchronized (mutex) {
			fieldSet = this.source.readFieldSet();
			if (fieldSet != null) {
				return transform(fieldSet);
			}
		}
		return null;
	}

	protected abstract Object transform(FieldSet fieldSet);
}
