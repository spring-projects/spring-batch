/*
 * Copyright 2006-2014 the original author or authors.
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

package org.springframework.batch.sample.domain.trade;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing on of 3 possible actions on a customer update: Add, update, or delete
 *
 * @author Lucas Ward
 *
 */
public enum CustomerOperation {

	ADD('A'), UPDATE('U'), DELETE('D');

	private final char code;

	private static final Map<Character, CustomerOperation> CODE_MAP;

	private CustomerOperation(char code) {
		this.code = code;
	}

	static {
		CODE_MAP = new HashMap<>();
		for (CustomerOperation operation : values()) {
			CODE_MAP.put(operation.getCode(), operation);
		}
	}

	public static CustomerOperation fromCode(char code) {
		if (CODE_MAP.containsKey(code)) {
			return CODE_MAP.get(code);
		}
		else {
			throw new IllegalArgumentException("Invalid code: [" + code + "]");
		}
	}

	public char getCode() {
		return code;
	}

}
