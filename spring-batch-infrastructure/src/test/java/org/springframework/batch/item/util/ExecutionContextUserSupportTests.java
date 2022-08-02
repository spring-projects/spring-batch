/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.item.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ExecutionContextUserSupport}.
 */
class ExecutionContextUserSupportTests {

	private final ExecutionContextUserSupport tested = new ExecutionContextUserSupport();

	/**
	 * Regular usage scenario - prepends the name (supposed to be unique) to argument.
	 */
	@Test
	void testGetKey() {
		tested.setName("uniqueName");
		assertEquals("uniqueName.key", tested.getKey("key"));
	}

	/**
	 * Exception scenario - name must not be empty.
	 */
	@Test
	void testGetKeyWithNoNameSet() {
		tested.setName("");
		assertThrows(IllegalArgumentException.class, () -> tested.getKey("arbitrary string"));
	}

}
