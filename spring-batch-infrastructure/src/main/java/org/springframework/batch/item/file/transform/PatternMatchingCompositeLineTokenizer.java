/*
 * Copyright 2006-2019 the original author or authors.
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

import java.util.Map;

import org.springframework.batch.support.PatternMatcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link LineTokenizer} implementation that stores a mapping of String
 * patterns to delegate {@link LineTokenizer}s. Each line tokenized will be
 * checked to see if it matches a pattern. If the line matches a key in the map
 * of delegates, then the corresponding delegate {@link LineTokenizer} will be
 * used. Patterns are sorted starting with the most specific, and the first
 * match succeeds.
 * 
 * @author Ben Hale
 * @author Dan Garrette
 * @author Dave Syer
 */
public class PatternMatchingCompositeLineTokenizer implements LineTokenizer, InitializingBean {

	private PatternMatcher<LineTokenizer> tokenizers = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.transform.LineTokenizer#tokenize(
	 * java.lang.String)
	 */
    @Override
	public FieldSet tokenize(@Nullable String line) {
		return tokenizers.match(line).tokenize(line);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
    @Override
	public void afterPropertiesSet() throws Exception {
		Assert.isTrue(this.tokenizers != null, "The 'tokenizers' property must be non-empty");
	}

	public void setTokenizers(Map<String, LineTokenizer> tokenizers) {
		Assert.isTrue(!tokenizers.isEmpty(), "The 'tokenizers' property must be non-empty");
		this.tokenizers = new PatternMatcher<>(tokenizers);
	}
}
