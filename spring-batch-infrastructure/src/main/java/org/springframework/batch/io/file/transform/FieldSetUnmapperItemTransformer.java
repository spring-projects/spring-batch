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
package org.springframework.batch.io.file.transform;

import org.springframework.batch.io.file.mapping.FieldSet;
import org.springframework.batch.io.file.mapping.FieldSetUnmapper;
import org.springframework.batch.item.writer.ItemTransformer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * An {@link ItemTransformer} that delegates to a {@link FieldSetUnmapper}, so
 * the result of the transformation is a {@link FieldSet}.
 * 
 * @author Dave Syer
 * 
 */
public class FieldSetUnmapperItemTransformer implements ItemTransformer, InitializingBean {

	private FieldSetUnmapper fieldSetUnmapper;

	/**
	 * Assert that mandatory properties are set.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(fieldSetUnmapper, "A FieldSetUnmapper must be provided.");
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.writer.ItemTransformer#transform(java.lang.Object)
	 */
	public Object transform(Object item) throws Exception {
		return fieldSetUnmapper.unmapItem(item);
	}
}
