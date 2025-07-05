/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.batch.core;

import org.jspecify.annotations.Nullable;

/**
 * Class that exposes the Spring Batch version. Fetches the "Implementation-Version"
 * manifest attribute from the jar file.
 *
 * <p>
 * Note that some ClassLoaders do not expose the package metadata, hence this class might
 * not be able to determine the Spring Batch version in all environments.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public final class SpringBatchVersion {

	/**
	 * The key to use in the execution context for batch version.
	 */
	public static final String BATCH_VERSION_KEY = "batch.version";

	private SpringBatchVersion() {
	}

	/**
	 * Return the full version string of the present Spring Batch codebase, or
	 * {@code "N/A"} if it cannot be determined.
	 * @see Package#getImplementationVersion()
	 */
	public static @Nullable String getVersion() {
		Package pkg = SpringBatchVersion.class.getPackage();
		if (pkg != null && pkg.getImplementationVersion() != null) {
			return pkg.getImplementationVersion();
		}
		return "N/A";
	}

}