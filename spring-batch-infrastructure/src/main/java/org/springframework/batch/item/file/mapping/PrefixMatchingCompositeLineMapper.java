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

package org.springframework.batch.item.file.mapping;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.PrefixMatchingCompositeLineTokenizer;
import org.springframework.util.Assert;

/**
 * <p>
 * A {@link LineMapper} implementation that stores a mapping of String prefixes
 * to delegate {@link LineTokenizer}s as well as a mapping of String prefixes
 * to delegate {@link LineTokenizer}s. Each line received will be tokenized and
 * then mapped to a field set.
 * 
 * <p>
 * Both the tokenizing and the mapping work in a similar way. The line will be
 * checked for its prefix. If the prefix matches a key in the map of delegates,
 * then the corresponding delegate will be used. Otherwise, the default
 * tokenizer or mapper will be used. The default can be configured in the
 * delegate map by setting its corresponding prefix to the empty string.
 * 
 * @author Dan Garrette
 */
public class PrefixMatchingCompositeLineMapper<T> extends PrefixMatchingCompositeLineTokenizer implements LineMapper<T> {

	private Map<String, FieldSetMapper<T>> fieldSetMappers = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.LineMapper#mapLine(java.lang.String,
	 *      int)
	 */
	public T mapLine(String line, int lineNumber) throws Exception {
		return this.matchPrefix(line, this.fieldSetMappers).mapFieldSet(this.tokenize(line));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.isTrue(this.fieldSetMappers != null && this.fieldSetMappers.size() > 0,
				"The 'fieldSetMappers' property must be non-empty");
	}

	public void setFieldSetMappers(Map<String, FieldSetMapper<T>> fieldSetMappers) {
		this.fieldSetMappers = new LinkedHashMap<String, FieldSetMapper<T>>(fieldSetMappers);
	}
}
