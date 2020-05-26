/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.batch.item.file.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.item.file.transform.FormatterLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link FlatFileItemWriter}
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Drummond Dawson
 * @since 4.0
 * @see FlatFileItemWriter
 */
public class FlatFileItemWriterBuilder<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private Resource resource;

	private boolean forceSync = false;

	private String lineSeparator = FlatFileItemWriter.DEFAULT_LINE_SEPARATOR;

	private LineAggregator<T> lineAggregator;

	private String encoding = FlatFileItemWriter.DEFAULT_CHARSET;

	private boolean shouldDeleteIfExists = true;

	private boolean append = false;

	private boolean shouldDeleteIfEmpty = false;

	private FlatFileHeaderCallback headerCallback;

	private FlatFileFooterCallback footerCallback;

	private boolean transactional = FlatFileItemWriter.DEFAULT_TRANSACTIONAL;

	private boolean saveState = true;

	private String name;

	private DelimitedBuilder<T> delimitedBuilder;

	private FormattedBuilder<T> formattedBuilder;

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public FlatFileItemWriterBuilder<T> saveState(boolean saveState) {
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
	public FlatFileItemWriterBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * The {@link Resource} to be used as output.
	 *
	 * @param resource the output of the writer.
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setResource(Resource)
	 */
	public FlatFileItemWriterBuilder<T> resource(Resource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * A flag indicating that changes should be force-synced to disk on flush.  Defaults
	 * to false.
	 *
	 * @param forceSync value to set the flag to
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setForceSync(boolean)
	 */
	public FlatFileItemWriterBuilder<T> forceSync(boolean forceSync) {
		this.forceSync = forceSync;

		return this;
	}

	/**
	 * String used to separate lines in output.  Defaults to the System property
	 * line.separator.
	 *
	 * @param lineSeparator value to use for a line separator
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setLineSeparator(String)
	 */
	public FlatFileItemWriterBuilder<T> lineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;

		return this;
	}

	/**
	 * Line aggregator used to build the String version of each item.
	 *
	 * @param lineAggregator {@link LineAggregator} implementation
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setLineAggregator(LineAggregator)
	 */
	public FlatFileItemWriterBuilder<T> lineAggregator(LineAggregator<T> lineAggregator) {
		this.lineAggregator = lineAggregator;

		return this;
	}

	/**
	 * Encoding used for output.
	 *
	 * @param encoding encoding type.
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setEncoding(String)
	 */
	public FlatFileItemWriterBuilder<T> encoding(String encoding) {
		this.encoding = encoding;

		return this;
	}

	/**
	 * If set to true, once the step is complete, if the resource previously provided is
	 * empty, it will be deleted.
	 *
	 * @param shouldDelete defaults to false
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setShouldDeleteIfEmpty(boolean)
	 */
	public FlatFileItemWriterBuilder<T> shouldDeleteIfEmpty(boolean shouldDelete) {
		this.shouldDeleteIfEmpty = shouldDelete;

		return this;
	}

	/**
	 * If set to true, upon the start of the step, if the resource already exists, it will
	 * be deleted and recreated.
	 *
	 * @param shouldDelete defaults to true
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setShouldDeleteIfExists(boolean)
	 */
	public FlatFileItemWriterBuilder<T> shouldDeleteIfExists(boolean shouldDelete) {
		this.shouldDeleteIfExists = shouldDelete;

		return this;
	}

	/**
	 * If set to true and the file exists, the output will be appended to the existing
	 * file.
	 *
	 * @param append defaults to false
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setAppendAllowed(boolean)
	 */
	public FlatFileItemWriterBuilder<T> append(boolean append) {
		this.append = append;

		return this;
	}

	/**
	 * A callback for header processing.
	 *
	 * @param callback {@link FlatFileHeaderCallback} impl
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setHeaderCallback(FlatFileHeaderCallback)
	 */
	public FlatFileItemWriterBuilder<T> headerCallback(FlatFileHeaderCallback callback) {
		this.headerCallback = callback;

		return this;
	}

	/**
	 * A callback for footer processing
	 * @param callback {@link FlatFileFooterCallback} impl
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setFooterCallback(FlatFileFooterCallback)
	 */
	public FlatFileItemWriterBuilder<T> footerCallback(FlatFileFooterCallback callback) {
		this.footerCallback = callback;

		return this;
	}

	/**
	 * If set to true, the flushing of the buffer is delayed while a transaction is active.
	 *
	 * @param transactional defaults to true
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setTransactional(boolean)
	 */
	public FlatFileItemWriterBuilder<T> transactional(boolean transactional) {
		this.transactional = transactional;

		return this;
	}

	/**
	 * Returns an instance of a {@link DelimitedBuilder} for building a
	 * {@link DelimitedLineAggregator}. The {@link DelimitedLineAggregator} configured by
	 * this builder will only be used if one is not explicitly configured via
	 * {@link FlatFileItemWriterBuilder#lineAggregator}
	 *
	 * @return a {@link DelimitedBuilder}
	 *
	 */
	public DelimitedBuilder<T> delimited() {
		this.delimitedBuilder = new DelimitedBuilder<>(this);
		return this.delimitedBuilder;
	}

	/**
	 * Returns an instance of a {@link FormattedBuilder} for building a
	 * {@link FormatterLineAggregator}. The {@link FormatterLineAggregator} configured by
	 * this builder will only be used if one is not explicitly configured via
	 * {@link FlatFileItemWriterBuilder#lineAggregator}
	 *
	 * @return a {@link FormattedBuilder}
	 *
	 */
	public FormattedBuilder<T> formatted() {
		this.formattedBuilder = new FormattedBuilder<>(this);
		return this.formattedBuilder;
	}

	/**
	 * A builder for constructing a {@link FormatterLineAggregator}.
	 *
	 * @param <T> the type of the parent {@link FlatFileItemWriterBuilder}
	 */
	public static class FormattedBuilder<T> {

		private FlatFileItemWriterBuilder<T> parent;

		private String format;

		private Locale locale = Locale.getDefault();

		private int maximumLength = 0;

		private int minimumLength = 0;

		private FieldExtractor<T> fieldExtractor;

		private List<String> names = new ArrayList<>();

		protected FormattedBuilder(FlatFileItemWriterBuilder<T> parent) {
			this.parent = parent;
		}

		/**
		 * Set the format string used to aggregate items
		 * @param format used to aggregate items
		 * @return The instance of the builder for chaining.
		 */
		public FormattedBuilder<T> format(String format) {
			this.format = format;
			return this;
		}

		/**
		 * Set the locale.
		 * @param locale to use
		 * @return The instance of the builder for chaining.
		 */
		public FormattedBuilder<T> locale(Locale locale) {
			this.locale = locale;
			return this;
		}

		/**
		 * Set the minimum length of the formatted string. If this is not set
		 * the default is to allow any length.
		 * @param minimumLength of the formatted string
		 * @return The instance of the builder for chaining.
		 */
		public FormattedBuilder<T> minimumLength(int minimumLength) {
			this.minimumLength = minimumLength;
			return this;
		}

		/**
		 * Set the maximum length of the formatted string. If this is not set
		 * the default is to allow any length.
		 * @param maximumLength of the formatted string
		 * @return The instance of the builder for chaining.
		 */
		public FormattedBuilder<T> maximumLength(int maximumLength) {
			this.maximumLength = maximumLength;
			return this;
		}

		/**
		 * Set the {@link FieldExtractor} to use to extract fields from each item.
		 * @param fieldExtractor to use to extract fields from each item
		 * @return The current instance of the builder
		 */
		public FlatFileItemWriterBuilder<T> fieldExtractor(FieldExtractor<T> fieldExtractor) {
			this.fieldExtractor = fieldExtractor;
			return this.parent;
		}

		/**
		 * Names of each of the fields within the fields that are returned in the order
		 * they occur within the formatted file. These names will be used to create
		 * a {@link BeanWrapperFieldExtractor} only if no explicit field extractor
		 * is set via {@link FormattedBuilder#fieldExtractor(FieldExtractor)}.
		 *
		 * @param names names of each field
		 * @return The parent {@link FlatFileItemWriterBuilder}
		 * @see BeanWrapperFieldExtractor#setNames(String[])
		 */
		public FlatFileItemWriterBuilder<T> names(String... names) {
			this.names.addAll(Arrays.asList(names));
			return this.parent;
		}

		public FormatterLineAggregator<T> build() {
			Assert.notNull(this.format, "A format is required");
			Assert.isTrue((this.names != null && !this.names.isEmpty()) || this.fieldExtractor != null,
					"A list of field names or a field extractor is required");

			FormatterLineAggregator<T> formatterLineAggregator = new FormatterLineAggregator<>();
			formatterLineAggregator.setFormat(this.format);
			formatterLineAggregator.setLocale(this.locale);
			formatterLineAggregator.setMinimumLength(this.minimumLength);
			formatterLineAggregator.setMaximumLength(this.maximumLength);

			if (this.fieldExtractor == null) {
				BeanWrapperFieldExtractor<T> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
				beanWrapperFieldExtractor.setNames(this.names.toArray(new String[this.names.size()]));
				try {
					beanWrapperFieldExtractor.afterPropertiesSet();
				}
				catch (Exception e) {
					throw new IllegalStateException("Unable to initialize FormatterLineAggregator", e);
				}
				this.fieldExtractor = beanWrapperFieldExtractor;
			}

			formatterLineAggregator.setFieldExtractor(this.fieldExtractor);
			return formatterLineAggregator;
		}
	}

	/**
	 * A builder for constructing a {@link DelimitedLineAggregator}
	 *
	 * @param <T> the type of the parent {@link FlatFileItemWriterBuilder}
	 */
	public static class DelimitedBuilder<T> {

		private FlatFileItemWriterBuilder<T> parent;

		private List<String> names = new ArrayList<>();

		private String delimiter = ",";

		private FieldExtractor<T> fieldExtractor;

		protected DelimitedBuilder(FlatFileItemWriterBuilder<T> parent) {
			this.parent = parent;
		}

		/**
		 * Define the delimiter for the file.
		 *
		 * @param delimiter String used as a delimiter between fields.
		 * @return The instance of the builder for chaining.
		 * @see DelimitedLineAggregator#setDelimiter(String)
		 */
		public DelimitedBuilder<T> delimiter(String delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		/**
		 * Names of each of the fields within the fields that are returned in the order
		 * they occur within the delimited file. These names will be used to create
		 * a {@link BeanWrapperFieldExtractor} only if no explicit field extractor
		 * is set via {@link DelimitedBuilder#fieldExtractor(FieldExtractor)}.
		 *
		 * @param names names of each field
		 * @return The parent {@link FlatFileItemWriterBuilder}
		 * @see BeanWrapperFieldExtractor#setNames(String[])
		 */
		public FlatFileItemWriterBuilder<T> names(String... names) {
			this.names.addAll(Arrays.asList(names));
			return this.parent;
		}

		/**
		 * Set the {@link FieldExtractor} to use to extract fields from each item.
		 * @param fieldExtractor to use to extract fields from each item
		 * @return The parent {@link FlatFileItemWriterBuilder}
		 */
		public FlatFileItemWriterBuilder<T> fieldExtractor(FieldExtractor<T> fieldExtractor) {
			this.fieldExtractor = fieldExtractor;
			return this.parent;
		}

		public DelimitedLineAggregator<T> build() {
			Assert.isTrue((this.names != null && !this.names.isEmpty()) || this.fieldExtractor != null,
					"A list of field names or a field extractor is required");

			DelimitedLineAggregator<T> delimitedLineAggregator = new DelimitedLineAggregator<>();
			if (this.delimiter != null) {
				delimitedLineAggregator.setDelimiter(this.delimiter);
			}

			if (this.fieldExtractor == null) {
				BeanWrapperFieldExtractor<T> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
				beanWrapperFieldExtractor.setNames(this.names.toArray(new String[this.names.size()]));
				try {
					beanWrapperFieldExtractor.afterPropertiesSet();
				}
				catch (Exception e) {
					throw new IllegalStateException("Unable to initialize DelimitedLineAggregator", e);
				}
				this.fieldExtractor = beanWrapperFieldExtractor;
			}

			delimitedLineAggregator.setFieldExtractor(this.fieldExtractor);
			return delimitedLineAggregator;
		}
	}

	/**
	 * Validates and builds a {@link FlatFileItemWriter}.
	 *
	 * @return a {@link FlatFileItemWriter}
	 */
	public FlatFileItemWriter<T> build() {

		Assert.isTrue(this.lineAggregator != null || this.delimitedBuilder != null || this.formattedBuilder != null,
				"A LineAggregator or a DelimitedBuilder or a FormattedBuilder is required");

		if(this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is true");
		}

		if(this.resource == null) {
			logger.debug("The resource is null. This is only a valid scenario when " +
					"injecting it later as in when using the MultiResourceItemWriter");
		}

		FlatFileItemWriter<T> writer = new FlatFileItemWriter<>();

		writer.setName(this.name);
		writer.setAppendAllowed(this.append);
		writer.setEncoding(this.encoding);
		writer.setFooterCallback(this.footerCallback);
		writer.setForceSync(this.forceSync);
		writer.setHeaderCallback(this.headerCallback);
		if (this.lineAggregator == null) {
			Assert.state(this.delimitedBuilder == null || this.formattedBuilder == null,
					"Either a DelimitedLineAggregator or a FormatterLineAggregator should be provided, but not both");
			if (this.delimitedBuilder != null) {
				this.lineAggregator = this.delimitedBuilder.build();
			}
			else {
				this.lineAggregator = this.formattedBuilder.build();
			}
		}
		writer.setLineAggregator(this.lineAggregator);
		writer.setLineSeparator(this.lineSeparator);
		writer.setResource(this.resource);
		writer.setSaveState(this.saveState);
		writer.setShouldDeleteIfEmpty(this.shouldDeleteIfEmpty);
		writer.setShouldDeleteIfExists(this.shouldDeleteIfExists);
		writer.setTransactional(this.transactional);

		return writer;
	}
}
