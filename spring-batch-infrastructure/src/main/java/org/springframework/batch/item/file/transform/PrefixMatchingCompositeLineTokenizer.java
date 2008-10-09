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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;

public class PrefixMatchingCompositeLineTokenizer implements LineTokenizer {

	private Map<String, LineTokenizer> tokenizers = new HashMap<String, LineTokenizer>();

	public void setTokenizers(Map<String, LineTokenizer> tokenizers) {
		this.tokenizers = new LinkedHashMap<String, LineTokenizer>(tokenizers);
	}

	public FieldSet process(String line) {

		if (line == null) {
			return new DefaultFieldSet(new String[0]);
		}

		LineTokenizer tokenizer = null;
		LineTokenizer defaultTokenizer = null;

		for (String key : tokenizers.keySet()) {

			if ("".equals(key)) {
				defaultTokenizer = (LineTokenizer) tokenizers.get(key);
				// don't break here or the tokenizer may not be found
				continue;
			}
			if (line.startsWith(key)) {
				tokenizer = (LineTokenizer) tokenizers.get(key);
				break;
			}
		}

		if (tokenizer == null) {
			tokenizer = defaultTokenizer;
		}

		if (tokenizer == null) {
			throw new IllegalStateException("Could not match record to tokenizer for line=[" + line + "]");
		}

		return tokenizer.process(line);
	}

}
