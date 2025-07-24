/*
 * Copyright 2016-2025 the original author or authors.
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
import java.util.function.Consumer;

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
import org.springframework.batch.item.file.transform.RecordFieldExtractor;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link FlatFileItemWriter}
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Drummond Dawson
 * @author Daeho Kwon
 * @since 4.0
 * @see FlatFileItemWriter
 */
public class FlatFileItemWriterBuilder<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private WritableResource resource;

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

	private DelimitedSpec<T> delimitedSpec;

	private FormattedSpec<T> formattedSpec;

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
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
	 * @param name name of the writer instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public FlatFileItemWriterBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * The {@link WritableResource} to be used as output.
	 * @param resource the output of the writer.
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setResource(WritableResource)
	 */
	public FlatFileItemWriterBuilder<T> resource(WritableResource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * A flag indicating that changes should be force-synced to disk on flush. Defaults to
	 * false.
	 * @param forceSync value to set the flag to
	 * @return The current instance of the builder.
	 * @see FlatFileItemWriter#setForceSync(boolean)
	 */
	public FlatFileItemWriterBuilder<T> forceSync(boolean forceSync) {
		this.forceSync = forceSync;

		return this;
	}

	/**
	 * String used to separate lines in output. Defaults to the System property
	 * line.separator.
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
	 * If set to true, the flushing of the buffer is delayed while a transaction is
	 * active.
	 * @param transactional defaults to true
	 * @return The current instance of the builder
	 * @see FlatFileItemWriter#setTransactional(boolean)
	 */
	public FlatFileItemWriterBuilder<T> transactional(boolean transactional) {
		this.transactional = transactional;

		return this;
	}

	/**
	 * Returns an instance of a {@link DelimitedSpec} for building a
	 * {@link DelimitedLineAggregator}. The {@link DelimitedLineAggregator} configured by
	 * this builder will only be used if one is not explicitly configured via
	 * {@link FlatFileItemWriterBuilder#lineAggregator}
	 * @return a {@link DelimitedSpec}
	 *
	 */
	public DelimitedSpec<T> delimited() {
		this.delimitedSpec = new DelimitedSpec<>(this);
		return this.delimitedSpec;
	}

	/**
	 * Configure a {@link DelimitedSpec} using a lambda.
	 * @param consumer the spec to configure
	 * @return the current builder instance
	 */
	public FlatFileItemWriterBuilder<T> delimited(Consumer<DelimitedSpec<T>> consumer) {
		DelimitedSpec<T> builder = new DelimitedSpec<>(this);
		consumer.accept(builder);
		this.delimitedSpec = builder;
		return this;
	}

	/**
	 * Returns an instance of a {@link FormattedSpec} for building a
	 * {@link FormatterLineAggregator}. The {@link FormatterLineAggregator} configured by
	 * this builder will only be used if one is not explicitly configured via
	 * {@link FlatFileItemWriterBuilder#lineAggregator}
	 * @return a {@link FormattedSpec}
	 *
	 */
	public FormattedSpec<T> formatted() {
		this.formattedSpec = new FormattedSpec<>(this);
		return this.formattedSpec;
	}

	/**
	 * Configure a {@link FormattedSpec} using a lambda.
	 * @param consumer the spec to configure
	 * @return the current builder instance
	 */
	public FlatFileItemWriterBuilder<T> formatted(Consumer<FormattedSpec<T>> consumer) {
		FormattedSpec<T> builder = new FormattedSpec<>(this);
		consumer.accept(builder);
		this.formattedSpec = builder;
		return this;
	}

	/**
	 * A builder for constructing a {@link FormatterLineAggregator}.
	 *
	 * @param <T> the type of the parent {@link FlatFileItemWriterBuilder}
	 */
	public static class FormattedSpec<T> {

		private final FlatFileItemWriterBuilder<T> parent;

		private String format;

		private Locale locale = Locale.getDefault();

		private int maximumLength = 0;

		private int minimumLength = 0;

		private FieldExtractor<T> fieldExtractor;

		private final List<String> names = new ArrayList<>();

		private Class<T> sourceType;

		protected FormattedSpec(FlatFileItemWriterBuilder<T> parent) {
			this.parent = parent;
		}

		/**
		 * Set the format string used to aggregate items
		 * @param format used to aggregate items
		 * @return The instance of the builder for chaining.
		 */
		public FormattedSpec<T> format(String format) {
			this.format = format;
			return this;
		}

		/**
		 * Set the locale.
		 * @param locale to use
		 * @return The instance of the builder for chaining.
		 */
		public FormattedSpec<T> locale(Locale locale) {
			this.locale = locale;
			return this;
		}

		/**
		 * Set the minimum length of the formatted string. If this is not set the default
		 * is to allow any length.
		 * @param minimumLength of the formatted string
		 * @return The instance of the builder for chaining.
		 */
		public FormattedSpec<T> minimumLength(int minimumLength) {
			this.minimumLength = minimumLength;
			return this;
		}

		/**
		 * Set the maximum length of the formatted string. If this is not set the default
		 * is to allow any length.
		 * @param maximumLength of the formatted string
		 * @return The instance of the builder for chaining.
		 */
		public FormattedSpec<T> maximumLength(int maximumLength) {
			this.maximumLength = maximumLength;
			return this;
		}

		/**
		 * Specify the type of items from which fields will be extracted. This is used to
		 * configure the right {@link FieldExtractor} based on the given type (ie a record
		 * or a regular class).
		 * @param sourceType type of items from which fields will be extracted
		 * @return The current instance of the builder.
		 * @since 5.0
		 */
		public FormattedSpec<T> sourceType(Class<T> sourceType) {
			this.sourceType = sourceType;

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
		 * they occur within the formatted file. These names will be used to create a
		 * {@link BeanWrapperFieldExtractor} only if no explicit field extractor is set
		 * via {@link FormattedSpec#fieldExtractor(FieldExtractor)}.
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
				if (this.sourceType != null && this.sourceType.isRecord()) {
					this.fieldExtractor = new RecordFieldExtractor<>(this.sourceType);
				}
				else {
					BeanWrapperFieldExtractor<T> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
					beanWrapperFieldExtractor.setNames(this.names.toArray(new String[this.names.size()]));
					try {
						beanWrapperFieldExtractor.afterPropertiesSet();
						this.fieldExtractor = beanWrapperFieldExtractor;
					}
					catch (Exception e) {
						throw new IllegalStateException("Unable to initialize FormatterLineAggregator", e);
					}
				}
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
	public static class DelimitedSpec<T> {

		private final FlatFileItemWriterBuilder<T> parent;

		private final List<String> names = new ArrayList<>();

		private String delimiter = ",";

		private String quoteCharacter = "";

		private FieldExtractor<T> fieldExtractor;

		private Class<T> sourceType;

		protected DelimitedSpec(FlatFileItemWriterBuilder<T> parent) {
			this.parent = parent;
		}

		/**
		 * Define the delimiter for the file.
		 * @param delimiter String used as a delimiter between fields.
		 * @return The instance of the builder for chaining.
		 * @see DelimitedLineAggregator#setDelimiter(String)
		 */
		public DelimitedSpec<T> delimiter(String delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		/**
		 * Specify the type of items from which fields will be extracted. This is used to
		 * configure the right {@link FieldExtractor} based on the given type (ie a record
		 * or a regular class).
		 * @param sourceType type of items from which fields will be extracted
		 * @return The current instance of the builder.
		 * @since 5.0
		 */
		public DelimitedSpec<T> sourceType(Class<T> sourceType) {
			this.sourceType = sourceType;

			return this;
		}

		/**
		 * Define the quote character for each delimited field. Default is empty string.
		 * @param quoteCharacter String used as a quote for the aggregate.
		 * @return The instance of the builder for chaining.
		 * @see DelimitedLineAggregator#setQuoteCharacter(String)
		 * @since 5.1
		 */
		public DelimitedSpec<T> quoteCharacter(String quoteCharacter) {
			this.quoteCharacter = quoteCharacter;
			return this;
		}

		/**
		 * Names of each of the fields within the fields that are returned in the order
		 * they occur within the delimited file. These names will be used to create a
		 * {@link BeanWrapperFieldExtractor} only if no explicit field extractor is set
		 * via {@link DelimitedSpec#fieldExtractor(FieldExtractor)}.
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
			if (StringUtils.hasLength(this.quoteCharacter)) {
				delimitedLineAggregator.setQuoteCharacter(this.quoteCharacter);
			}

			if (this.fieldExtractor == null) {
				if (this.sourceType != null && this.sourceType.isRecord()) {
					this.fieldExtractor = new RecordFieldExtractor<>(this.sourceType);
				}
				else {
					BeanWrapperFieldExtractor<T> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
					beanWrapperFieldExtractor.setNames(this.names.toArray(new String[this.names.size()]));
					try {
						beanWrapperFieldExtractor.afterPropertiesSet();
						this.fieldExtractor = beanWrapperFieldExtractor;
					}
					catch (Exception e) {
						throw new IllegalStateException("Unable to initialize DelimitedLineAggregator", e);
					}
				}
			}

			delimitedLineAggregator.setFieldExtractor(this.fieldExtractor);
			return delimitedLineAggregator;
		}

	}

	/**
	 * Validates and builds a {@link FlatFileItemWriter}.
	 * @return a {@link FlatFileItemWriter}
	 */
	public FlatFileItemWriter<T> build() {

		Assert.isTrue(this.lineAggregator != null || this.delimitedSpec != null || this.formattedSpec != null,
				"A LineAggregator or a DelimitedSpec or a FormattedSpec is required");

		if (this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is true");
		}

		if (this.resource == null) {
			logger.debug("The resource is null. This is only a valid scenario when "
					+ "injecting it later as in when using the MultiResourceItemWriter");
		}

		FlatFileItemWriter<T> writer = new FlatFileItemWriter<>();

		writer.setName(this.name);
		writer.setAppendAllowed(this.append);
		writer.setEncoding(this.encoding);
		writer.setFooterCallback(this.footerCallback);
		writer.setForceSync(this.forceSync);
		writer.setHeaderCallback(this.headerCallback);
		if (this.lineAggregator == null) {
			Assert.state(this.delimitedSpec == null || this.formattedSpec == null,
					"Either a DelimitedLineAggregator or a FormatterLineAggregator should be provided, but not both");
			if (this.delimitedSpec != null) {
				this.lineAggregator = this.delimitedSpec.build();
			}
			else {
				this.lineAggregator = this.formattedSpec.build();
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
