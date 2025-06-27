/*
 * Copyright 2023-2025 the original author or authors.
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
package org.springframework.batch.core.converter;

import java.time.format.DateTimeFormatter;

/**
 * Base class for date/time converters.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0.1
 * @deprecated since 6.0 with no replacement, scheduled for removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
class AbstractDateTimeConverter {

	protected DateTimeFormatter instantFormatter = DateTimeFormatter.ISO_INSTANT;

	protected DateTimeFormatter localDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

	protected DateTimeFormatter localTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;

	protected DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

}
