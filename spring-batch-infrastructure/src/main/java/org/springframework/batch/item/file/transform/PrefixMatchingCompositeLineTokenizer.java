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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link LineTokenizer} implementation that stores a mapping of String
 * prefixes to delegate {@link LineTokenizer}s. Each line tokenizied will be
 * checked for its prefix. If the prefix matches a key in the map of delegates,
 * then the corresponding delegate {@link LineTokenizer} will be used.
 * Otherwise, the default {@link LineTokenizer} will be used. The default
 * {@link LineTokenizer} can be configured in the delegate map by setting its
 * corresponding prefix to the empty string.
 * 
 * @author Dan Garrette
 */
public class PrefixMatchingCompositeLineTokenizer implements LineTokenizer, InitializingBean {

	private Map<String, LineTokenizer> tokenizers = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.transform.LineTokenizer#tokenize(java.lang.String)
	 */
	public FieldSet tokenize(String line) {
		return this.matchPrefix(line, this.tokenizers).tokenize(line);
	}

	/**
	 * @param line
	 * @return the delegate whose prefix matches the given line
	 */
	protected <S> S matchPrefix(String line, Map<String, S> delegates) {
		S delegate = null;
		S defaultDelegate = null;

		if (line != null) {
			for (String key : delegates.keySet()) {
				if (key != null) {
					if ("".equals(key)) {
						defaultDelegate = delegates.get(key);
					}
					else if (line.startsWith(key)) {
						delegate = delegates.get(key);
						break;
					}
				}
			}

			if (delegate == null) {
				delegate = defaultDelegate;
			}
		}
		else if (delegates.containsKey(null)) {
			delegate = delegates.get(null);
		}
		else {
			throw new IllegalStateException("Could not handle a null line");
		}

		if (delegate == null) {
			throw new IllegalStateException("Could not find a matching prefix for line=[" + line + "]");
		}
		return delegate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.isTrue(this.tokenizers != null && this.tokenizers.size() > 0,
				"The 'tokenizers' property must be non-empty");
	}

	public void setTokenizers(Map<String, LineTokenizer> tokenizers) {
		this.tokenizers = new LinkedHashMap<String, LineTokenizer>(tokenizers);
	}
}
