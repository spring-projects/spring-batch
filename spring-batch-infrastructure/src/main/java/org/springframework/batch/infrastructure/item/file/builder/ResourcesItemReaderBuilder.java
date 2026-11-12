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
import java.util.ArrayList;
import java.util.List;

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
	 * The location patterns of resources that the {@link ResourcesItemReader} will serve
	 * up as items. Each pattern is resolved through a
	 * {@link PathMatchingResourcePatternResolver}, so it can use any resource prefix
	 * supported by Spring (for example {@code file:}, {@code classpath:} or
	 * {@code classpath*:}) combined with Ant-style wildcards like {@code *}, {@code **}
	 * and {@code ?} (for example {@code file:/data/*.csv} or
	 * {@code classpath*:data/**&#47;user?.txt}).
	 * @param locationPatterns the location patterns of resources to use.
	 * @return this instance for method chaining.
	 */
	public ResourcesItemReaderBuilder resources(String... locationPatterns) {
		ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
		List<Resource> resolvedResources = new ArrayList<>();
		for (String locationPattern : locationPatterns) {
			try {
				resolvedResources.addAll(List.of(patternResolver.getResources(locationPattern)));
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Unable to resolve resources for pattern " + locationPattern, e);
			}
		}
		this.resources = resolvedResources.toArray(new Resource[0]);

		return this;
	}

	/**
	 * Builds the {@link ResourcesItemReader}.
	 * @return a {@link ResourcesItemReader}
	 */
	public ResourcesItemReader build() {
		Assert.notNull(this.resources, "resources array is required.");

		ResourcesItemReader reader = new ResourcesItemReader();
		reader.setResources(this.resources);

		if (StringUtils.hasText(this.name)) {
			reader.setName(this.name);
		}

		return reader;
	}

}
