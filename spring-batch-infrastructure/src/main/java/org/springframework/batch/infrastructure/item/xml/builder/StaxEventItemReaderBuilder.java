/*
 * Copyright 2017-2025 the original author or authors.
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
package org.springframework.batch.infrastructure.item.xml.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemReader;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * A fluent builder for the {@link StaxEventItemReader}
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 * @author Andrey Litvitski
 * @since 4.0
 */
public class StaxEventItemReaderBuilder<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private boolean strict = true;

	private @Nullable Resource resource;

	private @Nullable Unmarshaller unmarshaller;

	private final List<String> fragmentRootElements = new ArrayList<>();

	private boolean saveState = true;

	private @Nullable String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	private XMLInputFactory xmlInputFactory = StaxUtils.createDefensiveInputFactory();

	private String encoding = StaxEventItemReader.DEFAULT_ENCODING;

	/**
	 * Configure if the state of the {@link ItemStreamSupport} should be persisted within
	 * the {@link ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public StaxEventItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the {@link ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see ItemStreamSupport#setName(String)
	 */
	public StaxEventItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public StaxEventItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public StaxEventItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * The {@link Resource} to be used as input.
	 * @param resource the input to the reader.
	 * @return The current instance of the builder.
	 * @see StaxEventItemReader#setResource(Resource)
	 */
	public StaxEventItemReaderBuilder<T> resource(Resource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * An implementation of the {@link Unmarshaller} from Spring's OXM module.
	 * @param unmarshaller component responsible for unmarshalling XML chunks
	 * @return The current instance of the builder.
	 * @see StaxEventItemReader#setUnmarshaller
	 */
	public StaxEventItemReaderBuilder<T> unmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;

		return this;
	}

	/**
	 * Adds the list of fragments to be used as the root of each chunk to the
	 * configuration.
	 * @param fragmentRootElements the XML root elements to be used to identify XML
	 * chunks.
	 * @return The current instance of the builder.
	 * @see StaxEventItemReader#setFragmentRootElementNames(String[])
	 */
	public StaxEventItemReaderBuilder<T> addFragmentRootElements(String... fragmentRootElements) {
		this.fragmentRootElements.addAll(Arrays.asList(fragmentRootElements));

		return this;
	}

	/**
	 * Adds the list of fragments to be used as the root of each chunk to the
	 * configuration.
	 * @param fragmentRootElements the XML root elements to be used to identify XML
	 * chunks.
	 * @return The current instance of the builder.
	 * @see StaxEventItemReader#setFragmentRootElementNames(String[])
	 */
	public StaxEventItemReaderBuilder<T> addFragmentRootElements(List<String> fragmentRootElements) {
		this.fragmentRootElements.addAll(fragmentRootElements);

		return this;
	}

	/**
	 * Setting this value to true indicates that it is an error if the input does not
	 * exist and an exception will be thrown. Defaults to true.
	 * @param strict indicates the input file must exist
	 * @return The current instance of the builder
	 * @see StaxEventItemReader#setStrict(boolean)
	 */
	public StaxEventItemReaderBuilder<T> strict(boolean strict) {
		this.strict = strict;

		return this;
	}

	/**
	 * Set the {@link XMLInputFactory}.
	 * @param xmlInputFactory to use
	 * @return The current instance of the builder
	 * @see StaxEventItemReader#setXmlInputFactory(XMLInputFactory)
	 */
	public StaxEventItemReaderBuilder<T> xmlInputFactory(XMLInputFactory xmlInputFactory) {
		this.xmlInputFactory = xmlInputFactory;

		return this;
	}

	/**
	 * Encoding for the input file. Defaults to
	 * {@link StaxEventItemReader#DEFAULT_ENCODING}. Can be {@code null}, in which case
	 * the XML event reader will attempt to auto-detect the encoding from tht input file.
	 * @param encoding String encoding algorithm
	 * @return the current instance of the builder
	 * @see StaxEventItemReader#setEncoding(String)
	 */
	public StaxEventItemReaderBuilder<T> encoding(String encoding) {
		this.encoding = encoding;

		return this;
	}

	/**
	 * Validates the configuration and builds a new {@link StaxEventItemReader}
	 * @return a new instance of the {@link StaxEventItemReader}
	 */
	public StaxEventItemReader<T> build() {
		Assert.notNull(this.unmarshaller, "An unmarshaller is required");
		StaxEventItemReader<T> reader = new StaxEventItemReader<>(this.unmarshaller);

		if (this.resource != null) {
			reader.setResource(this.resource);
		}
		else {
			logger.debug("The resource is null. This is only a valid scenario when "
					+ "injecting resource later as in when using the MultiResourceItemReader");
		}

		Assert.notEmpty(this.fragmentRootElements, "At least one fragment root element is required");

		if (this.name != null) {
			reader.setName(this.name);
		}
		reader.setSaveState(this.saveState);
		reader.setFragmentRootElementNames(this.fragmentRootElements.toArray(new String[0]));

		reader.setStrict(this.strict);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setXmlInputFactory(this.xmlInputFactory);
		reader.setEncoding(this.encoding);

		return reader;
	}

}
