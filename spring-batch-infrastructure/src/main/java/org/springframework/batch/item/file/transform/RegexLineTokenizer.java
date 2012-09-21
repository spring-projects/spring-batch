/*
 * Copyright 2006-2012 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Line-tokenizer using a regular expression to filter out data (by using matching and non-matching groups).
 * Consider the following regex which picks only the first and last name (notice the non-matching group in the middle):
 * <pre>
 * (.*?)(?: .*)* (.*) 
 * </pre>
 * For the names:
 * <ul>  
 *  <li>"Graham James Edward Miller"</li>
 *  <li>"Andrew Gregory Macintyre"</li>
 *  <li>"No MiddleName"</li>
 * </ul> 
 * 
 * the output will be:
 * <ul>
 * <li>"Miller", "Graham"</li>
 * <li>"Macintyre", "Andrew"</li>
 * <li>"MiddleName", "No"</li>
 * </ul>
 * 
 * An empty list is returned, in case of a non-match.
 * 
 * @see Matcher#group(int)
 * @author Costin Leau
 */
public class RegexLineTokenizer extends AbstractLineTokenizer {

	private Pattern pattern;

	@Override
	protected List<String> doTokenize(String line) {
		Matcher matcher = pattern.matcher(line);
		boolean matchFound = matcher.find();

		if (matchFound) {
			List<String> tokens = new ArrayList<String>(matcher.groupCount());
			for (int i = 1; i <= matcher.groupCount(); i++) {
				tokens.add(matcher.group(i));
			}
			return tokens;
		}
		return Collections.emptyList();
	}

	/**
	 * Sets the regex pattern to use.
	 * 
	 * @param pattern Regular Expression pattern
	 */
	public void setPattern(Pattern pattern) {
		Assert.notNull(pattern, "a non-null pattern is required");
		this.pattern = pattern;
	}

	/**
	 * Sets the regular expression to use. 
	 * 
	 * @param regex regular expression (as a String)
	 */
	public void setRegex(String regex) {
		Assert.hasText(regex, "a valid regex is required");
		this.pattern = Pattern.compile(regex);
	}
}