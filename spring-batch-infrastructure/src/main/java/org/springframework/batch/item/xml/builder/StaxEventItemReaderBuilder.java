/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.batch.item.xml.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxUtils;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A fluent builder for the {@link StaxEventItemReader}
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Parikshit Dutta
 * @since 4.0
 */
public class StaxEventItemReaderBuilder<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private boolean strict = true;

	private Resource resource;

	private Unmarshaller unmarshaller;

	private List<String> fragmentRootElements = new ArrayList<>();

	private boolean saveState = true;

	private String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	private XMLInputFactory xmlInputFactory = StaxUtils.createXmlInputFactory();

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public StaxEventItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 *
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public StaxEventItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 *
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public StaxEventItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 *
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public StaxEventItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * The {@link Resource} to be used as input.
	 *
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
	 *
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
	 *
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
	 *
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
	 *
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
	 *
	 * @param xmlInputFactory to use
	 * @return The current instance of the builder
	 * @see StaxEventItemReader#setXmlInputFactory(XMLInputFactory)
	 */
	public StaxEventItemReaderBuilder<T> xmlInputFactory(XMLInputFactory xmlInputFactory) {
		this.xmlInputFactory = xmlInputFactory;

		return this;
	}

	/**
	 * Validates the configuration and builds a new {@link StaxEventItemReader}
	 *
	 * @return a new instance of the {@link StaxEventItemReader}
	 */
	public StaxEventItemReader<T> build() {
		StaxEventItemReader<T> reader = new StaxEventItemReader<>();

		if (this.resource == null) {
			logger.debug("The resource is null. This is only a valid scenario when " +
					"injecting resource later as in when using the MultiResourceItemReader");
		}

		if (this.saveState) {
			Assert.state(StringUtils.hasText(this.name), "A name is required when saveState is set to true.");
		}

		Assert.notEmpty(this.fragmentRootElements, "At least one fragment root element is required");

		reader.setName(this.name);
		reader.setSaveState(this.saveState);
		reader.setResource(this.resource);
		reader.setFragmentRootElementNames(
				this.fragmentRootElements.toArray(new String[this.fragmentRootElements.size()]));

		reader.setStrict(this.strict);
		reader.setUnmarshaller(this.unmarshaller);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setXmlInputFactory(this.xmlInputFactory);

		return reader;
	}
}
