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
package org.springframework.batch.core.configuration;

import java.util.Collection;

/**
 * A listable extension of {@link JobLocator}.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @deprecated since 6.0, scheduled for removal in 6.2 or later. Use {@link JobRegistry}
 * instead.
 */
@Deprecated(since = "6.0", forRemoval = true)
public interface ListableJobLocator extends JobLocator {

	/**
	 * Provides the currently registered job names. The return value is unmodifiable and
	 * disconnected from the underlying registry storage.
	 * @return a collection of String. Empty if none are registered.
	 */
	Collection<String> getJobNames();

}
