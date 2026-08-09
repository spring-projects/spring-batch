/*
 * Copyright 2026 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.infrastructure.item.file.builder;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.file.ResourcesItemReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link ResourcesItemReader}.
 *
 * @author Sanghyuk Jung
 * @since 6.1
 * @see ResourcesItemReader
 */
public class ResourcesItemReaderBuilder {

	private Resource @Nullable [] resources;

	private @Nullable String filesPattern;

	private @Nullable String name;

	/**
	 * The name used to calculate the key within the {@link ExecutionContext}.
	 * @param name name of the reader instance
	 * @return this instance for method chaining.
	 * @see ItemStreamSupport#setName(String)
	 */
	public ResourcesItemReaderBuilder name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * The array of resources that the {@link ResourcesItemReader} will serve up as items.
	 * @param resources the array of resources to use.
	 * @return this instance for method chaining.
	 * @see ResourcesItemReader#setResources(Resource[])
	 */
	public ResourcesItemReaderBuilder resources(Resource... resources) {
		this.resources = resources;

		return this;
	}

	/**
	 * The location pattern of files that the {@link ResourcesItemReader} will serve up as
	 * items. This is an Ant-style pattern that supports wildcards like `*`, `**` and
	 * `?`(for example `/data/*.csv`or `data/**\/user?.txt`).
	 * @param filesPattern the location pattern of files to use.
	 * @return this instance for method chaining.
	 */
	public ResourcesItemReaderBuilder filesPattern(String filesPattern) {
		this.filesPattern = filesPattern;

		return this;
	}

	/**
	 * Builds the {@link ResourcesItemReader}.
	 * @return a {@link ResourcesItemReader}
	 */
	public ResourcesItemReader build() {
		Assert.isTrue(this.resources != null || this.filesPattern != null,
				"resources array or filesPattern is required.");

		ResourcesItemReader reader = new ResourcesItemReader();

		if (this.resources != null) {
			reader.setResources(this.resources);
		}
		else if (this.filesPattern != null) {
			ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
			try {
				Resource[] resources = patternResolver.getResources("file:" + this.filesPattern);
				reader.setResources(resources);
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Unable to initialize resources by the pattern " + this.filesPattern,
						e);
			}
		}

		if (StringUtils.hasText(this.name)) {
			reader.setName(this.name);
		}

		return reader;
	}

}
