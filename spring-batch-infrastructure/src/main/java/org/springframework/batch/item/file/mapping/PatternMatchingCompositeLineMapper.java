/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.Map;

import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.PatternMatchingCompositeLineTokenizer;
import org.springframework.batch.support.PatternMatcher;
import org.springframework.util.Assert;

/**
 * <p>
 * A {@link LineMapper} implementation that stores a mapping of String patterns to
 * delegate {@link LineTokenizer}s as well as a mapping of String patterns to delegate
 * {@link FieldSetMapper}s. Each line received will be tokenized and then mapped to a
 * field set.
 *
 * <p>
 * Both the tokenizing and the mapping work in a similar way. The line will be checked for
 * its matching pattern. If the key matches a pattern in the map of delegates, then the
 * corresponding delegate will be used. Patterns are sorted starting with the most
 * specific, and the first match succeeds.
 *
 * @see PatternMatchingCompositeLineTokenizer
 * @author Dan Garrette
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class PatternMatchingCompositeLineMapper<T> implements LineMapper<T> {

	private final PatternMatchingCompositeLineTokenizer tokenizer;

	private PatternMatcher<FieldSetMapper<T>> patternMatcher;

	/**
	 * Construct a {@link PatternMatchingCompositeLineMapper} with the provided maps of
	 * tokenizers and field set mappers. Both maps must be non-empty.
	 * @param tokenizers the map of patterns to tokenizers
	 * @param fieldSetMappers the map of patterns to field set mappers
	 * @since 6.0
	 */
	public PatternMatchingCompositeLineMapper(Map<String, LineTokenizer> tokenizers,
			Map<String, FieldSetMapper<T>> fieldSetMappers) {
		Assert.isTrue(!tokenizers.isEmpty(), "The 'tokenizers' property must be non-empty");
		Assert.isTrue(!fieldSetMappers.isEmpty(), "The 'fieldSetMappers' property must be non-empty");
		this.tokenizer = new PatternMatchingCompositeLineTokenizer(tokenizers);
		this.patternMatcher = new PatternMatcher<>(fieldSetMappers);
	}

	@Override
	public T mapLine(String line, int lineNumber) throws Exception {
		return patternMatcher.match(line).mapFieldSet(this.tokenizer.tokenize(line));
	}

	public void setTokenizers(Map<String, LineTokenizer> tokenizers) {
		this.tokenizer.setTokenizers(tokenizers);
	}

	public void setFieldSetMappers(Map<String, FieldSetMapper<T>> fieldSetMappers) {
		Assert.isTrue(!fieldSetMappers.isEmpty(), "The 'fieldSetMappers' property must be non-empty");
		this.patternMatcher = new PatternMatcher<>(fieldSetMappers);
	}

}
