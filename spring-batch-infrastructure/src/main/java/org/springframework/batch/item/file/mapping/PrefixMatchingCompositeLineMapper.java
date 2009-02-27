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
import org.springframework.batch.support.PatternMatcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>
 * A {@link LineMapper} implementation that stores a mapping of String prefixes
 * to delegate {@link LineTokenizer}s as well as a mapping of String prefixes
 * to delegate {@link FieldSetMapper}s. Each line received will be tokenized
 * and then mapped to a field set.
 * 
 * <p>
 * Both the tokenizing and the mapping work in a similar way. The line will be
 * checked for its prefix. If the prefix matches a key in the map of delegates,
 * then the corresponding delegate will be used. Prefixes are sorted starting
 * with the most specific, and the first match always succeeds.
 * 
 * @see PrefixMatchingCompositeLineTokenizer
 * @see PatternMatcher#match(String, Map)
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class PrefixMatchingCompositeLineMapper<T> implements LineMapper<T>, InitializingBean {

	private PrefixMatchingCompositeLineTokenizer tokenizer = new PrefixMatchingCompositeLineTokenizer();
	private Map<String, FieldSetMapper<T>> fieldSetMappers = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.LineMapper#mapLine(java.lang.String,
	 *      int)
	 */
	public T mapLine(String line, int lineNumber) throws Exception {
		return PatternMatcher.match(line, this.fieldSetMappers).mapFieldSet(this.tokenizer.tokenize(line));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		this.tokenizer.afterPropertiesSet();
		Assert.isTrue(this.fieldSetMappers != null && this.fieldSetMappers.size() > 0,
				"The 'fieldSetMappers' property must be non-empty");
	}

	public void setTokenizers(Map<String, LineTokenizer> tokenizers) {
		this.tokenizer.setTokenizers(tokenizers);
	}

	public void setFieldSetMappers(Map<String, FieldSetMapper<T>> fieldSetMappers) {
		this.fieldSetMappers = new LinkedHashMap<String, FieldSetMapper<T>>();
		for (String key : fieldSetMappers.keySet()) {
			FieldSetMapper<T> value = fieldSetMappers.get(key);
			if (!key.endsWith("*")) {
				key = key + "*";
			}
			this.fieldSetMappers.put(key, value);
		}
	}
}
