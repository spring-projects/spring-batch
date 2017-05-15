/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.file.builder;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.builder.AbstractItemStreamSupportBuilder;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.ResourceSuffixCreator;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link MultiResourceItemWriter}.
 *
 * @author Glenn Renfro
 * @author Glenn Renfro
 * @since 4.0
 * @see MultiResourceItemWriter
 */
public class MultiResourceItemWriterBuilder<T>
		extends AbstractItemStreamSupportBuilder<MultiResourceItemWriterBuilder<T>> {

	private Resource resource;

	private ResourceAwareItemWriterItemStream<? super T> delegate;

	private int itemCountLimitPerResource = Integer.MAX_VALUE;

	private ResourceSuffixCreator suffixCreator;

	/**
	 * Allows customization of the suffix of the created resources based on the index.
	 *
	 * @param suffixCreator the customizable ResourceSuffixCreator to use.
	 * @return The current instance of the builder.
	 * @see MultiResourceItemWriter#setResourceSuffixCreator(ResourceSuffixCreator)
	 */
	public MultiResourceItemWriterBuilder<T> resourceSuffixCreator(ResourceSuffixCreator suffixCreator) {
		this.suffixCreator = suffixCreator;

		return this;
	}

	/**
	 * After this limit is exceeded the next chunk will be written into newly created
	 * resource.
	 *
	 * @param itemCountLimitPerResource the max numbers of items to be written per chunk.
	 * @return The current instance of the builder.
	 * @see MultiResourceItemWriter#setItemCountLimitPerResource(int)
	 */
	public MultiResourceItemWriterBuilder<T> itemCountLimitPerResource(int itemCountLimitPerResource) {
		this.itemCountLimitPerResource = itemCountLimitPerResource;

		return this;
	}

	/**
	 * Delegate used for actual writing of the output.
	 * @param delegate The delegate to use for writing.
	 * @return The current instance of the builder.
	 * @see MultiResourceItemWriter#setDelegate(ResourceAwareItemWriterItemStream)
	 */
	public MultiResourceItemWriterBuilder<T> delegate(ResourceAwareItemWriterItemStream<? super T> delegate) {
		this.delegate = delegate;

		return this;
	}

	/**
	 * Prototype for output resources. Actual output files will be created in the same
	 * directory and use the same name as this prototype with appended suffix (according
	 * to {@link MultiResourceItemWriter#setResourceSuffixCreator(ResourceSuffixCreator)}.
	 *
	 * @param resource the prototype resource to use as the basis for creating resources.
	 * @return The current instance of the builder.
	 * @see MultiResourceItemWriter#setResource(Resource)
	 */
	public MultiResourceItemWriterBuilder<T> resource(Resource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * Builds the {@link MultiResourceItemWriter}.
	 *
	 * @return a {@link MultiResourceItemWriter}
	 */
	public MultiResourceItemWriter<T> build() {
		Assert.notNull(this.resource, "resource is required.");
		Assert.notNull(this.delegate, "delegate is required.");

		if(this.saveState) {
			org.springframework.util.Assert.hasText(this.name, "A name is required when saveState is true.");
		}

		MultiResourceItemWriter<T> writer = new MultiResourceItemWriter<>();
		writer.setResource(this.resource);
		writer.setDelegate(this.delegate);
		writer.setItemCountLimitPerResource(this.itemCountLimitPerResource);
		if(this.suffixCreator != null) {
			writer.setResourceSuffixCreator(this.suffixCreator);
		}
		writer.setSaveState(this.saveState);
		writer.setName(this.name);

		return writer;
	}

}
