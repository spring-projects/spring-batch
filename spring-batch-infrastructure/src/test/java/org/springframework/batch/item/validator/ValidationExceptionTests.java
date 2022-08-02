/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.item.validator;

import org.springframework.batch.common.AbstractExceptionTests;

class ValidationExceptionTests extends AbstractExceptionTests {

	@Override
	protected Exception getException(String msg) {
		return new ValidationException(msg);
	}

	@Override
	protected Exception getException(String msg, Throwable t) {
		return new ValidationException(msg, t);
	}

}
