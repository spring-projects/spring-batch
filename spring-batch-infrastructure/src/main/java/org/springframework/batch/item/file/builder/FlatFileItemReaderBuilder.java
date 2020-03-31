/*
 * Copyright 2016-2020 the original author or authors.
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

import java.beans.PropertyEditor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.file.BufferedReaderFactory;
import org.springframework.batch.item.file.DefaultBufferedReaderFactory;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.DefaultFieldSetFactory;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSetFactory;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link FlatFileItemReader}.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Drummond Dawson
 * @since 4.0
 * @see FlatFileItemReader
 */
public class FlatFileItemReaderBuilder<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private boolean strict = true;

	private String encoding = FlatFileItemReader.DEFAULT_CHARSET;

	private RecordSeparatorPolicy recordSeparatorPolicy =
			new SimpleRecordSeparatorPolicy();

	private BufferedReaderFactory bufferedReaderFactory =
			new DefaultBufferedReaderFactory();

	private Resource resource;

	private List<String> comments =
			new ArrayList<>(Arrays.asList(FlatFileItemReader.DEFAULT_COMMENT_PREFIXES));

	private int linesToSkip = 0;

	private LineCallbackHandler skippedLinesCallback;

	private LineMapper<T> lineMapper;

	private FieldSetMapper<T> fieldSetMapper;

	private LineTokenizer lineTokenizer;

	private DelimitedBuilder<T> delimitedBuilder;

	private FixedLengthBuilder<T> fixedLengthBuilder;

	private Class<? extends T> targetType;

	private String prototypeBeanName;

	private BeanFactory beanFactory;

	private Map<Class<?>, PropertyEditor> customEditors = new HashMap<>();

	private int distanceLimit = 5;

	private boolean beanMapperStrict = true;

	private BigInteger tokenizerValidator = new BigInteger("0");

	private boolean saveState = true;

	private String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public FlatFileItemReaderBuilder<T> saveState(boolean saveState) {
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
	public FlatFileItemReaderBuilder<T> name(String name) {
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
	public FlatFileItemReaderBuilder<T> maxItemCount(int maxItemCount) {
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
	public FlatFileItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * Add a string to the list of Strings that indicate commented lines.
	 * Defaults to {@link FlatFileItemReader#DEFAULT_COMMENT_PREFIXES}.
	 *
	 * @param comment the string to define a commented line.
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setComments(String[])
	 */
	public FlatFileItemReaderBuilder<T> addComment(String comment) {
		this.comments.add(comment);
		return this;
	}

	/**
	 * Set an array of Strings that indicate lines that are comments (and therefore skipped by
	 * the reader). This method overrides the default comment prefixes which are
	 * {@link FlatFileItemReader#DEFAULT_COMMENT_PREFIXES}.
	 *
	 * @param comments an array of strings to identify comments.
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setComments(String[])
	 */
	public FlatFileItemReaderBuilder<T> comments(String... comments) {
		this.comments = Arrays.asList(comments);
		return this;
	}

	/**
	 * Configure a custom {@link RecordSeparatorPolicy} for the reader.
	 *
	 * @param policy custom policy
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setRecordSeparatorPolicy(RecordSeparatorPolicy)
	 */
	public FlatFileItemReaderBuilder<T> recordSeparatorPolicy(RecordSeparatorPolicy policy) {
		this.recordSeparatorPolicy = policy;
		return this;
	}

	/**
	 * Configure a custom {@link BufferedReaderFactory} for the reader.
	 *
	 * @param factory custom factory
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setBufferedReaderFactory(BufferedReaderFactory)
	 */
	public FlatFileItemReaderBuilder<T> bufferedReaderFactory(BufferedReaderFactory factory) {
		this.bufferedReaderFactory = factory;
		return this;
	}


	/**
	 * The {@link Resource} to be used as input.
	 *
	 * @param resource the input to the reader.
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setResource(Resource)
	 */
	public FlatFileItemReaderBuilder<T> resource(Resource resource) {
		this.resource = resource;
		return this;
	}

	/**
	 * Configure if the reader should be in strict mode (require the input {@link Resource}
	 * to exist).
	 *
	 * @param strict true if the input file is required to exist.
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setStrict(boolean)
	 */
	public FlatFileItemReaderBuilder<T> strict(boolean strict) {
		this.strict = strict;
		return this;
	}

	/**
	 * Configure the encoding used by the reader to read the input source.
	 * Default value is {@link FlatFileItemReader#DEFAULT_CHARSET}.
	 *
	 * @param encoding to use to read the input source.
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setEncoding(String)
	 */
	public FlatFileItemReaderBuilder<T> encoding(String encoding) {
		this.encoding = encoding;
		return this;
	}

	/**
	 * The number of lines to skip at the beginning of reading the file.
	 *
	 * @param linesToSkip number of lines to be skipped.
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setLinesToSkip(int)
	 */
	public FlatFileItemReaderBuilder<T> linesToSkip(int linesToSkip) {
		this.linesToSkip = linesToSkip;
		return this;
	}

	/**
	 * A callback to be called for each line that is skipped.
	 *
	 * @param callback the callback
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setSkippedLinesCallback(LineCallbackHandler)
	 */
	public FlatFileItemReaderBuilder<T> skippedLinesCallback(LineCallbackHandler callback) {
		this.skippedLinesCallback = callback;
		return this;
	}

	/**
	 * A {@link LineMapper} implementation to be used.
	 *
	 * @param lineMapper {@link LineMapper}
	 * @return The current instance of the builder.
	 * @see FlatFileItemReader#setLineMapper(LineMapper)
	 */
	public FlatFileItemReaderBuilder<T> lineMapper(LineMapper<T> lineMapper) {
		this.lineMapper = lineMapper;
		return this;
	}

	/**
	 * A {@link FieldSetMapper} implementation to be used.
	 *
	 * @param mapper a {@link FieldSetMapper}
	 * @return The current instance of the builder.
	 * @see DefaultLineMapper#setFieldSetMapper(FieldSetMapper)
	 */
	public FlatFileItemReaderBuilder<T> fieldSetMapper(FieldSetMapper<T> mapper) {
		this.fieldSetMapper = mapper;
		return this;
	}

	/**
	 * A {@link LineTokenizer} implementation to be used.
	 *
	 * @param tokenizer a {@link LineTokenizer}
	 * @return The current instance of the builder.
	 * @see DefaultLineMapper#setLineTokenizer(LineTokenizer)
	 */
	public FlatFileItemReaderBuilder<T> lineTokenizer(LineTokenizer tokenizer) {
		updateTokenizerValidation(tokenizer, 0);

		this.lineTokenizer = tokenizer;
		return this;
	}

	/**
	 * Returns an instance of a {@link DelimitedBuilder} for building a
	 * {@link DelimitedLineTokenizer}.  The {@link DelimitedLineTokenizer} configured by
	 * this builder will only be used if one is not explicitly configured via
	 * {@link FlatFileItemReaderBuilder#lineTokenizer}
	 *
	 * @return a {@link DelimitedBuilder}
	 *
	 */
	public DelimitedBuilder<T> delimited() {
		this.delimitedBuilder = new DelimitedBuilder<>(this);
		updateTokenizerValidation(this.delimitedBuilder, 1);
		return this.delimitedBuilder;
	}

	/**
	 * Returns an instance of a {@link FixedLengthBuilder} for building a
	 * {@link FixedLengthTokenizer}.  The {@link FixedLengthTokenizer} configured by this
	 * builder will only be used if the {@link FlatFileItemReaderBuilder#lineTokenizer}
	 * has not been configured.
	 *
	 * @return a {@link FixedLengthBuilder}
	 */
	public FixedLengthBuilder<T> fixedLength() {
		this.fixedLengthBuilder = new FixedLengthBuilder<>(this);
		updateTokenizerValidation(this.fixedLengthBuilder, 2);
		return this.fixedLengthBuilder;
	}

	/**
	 * The class that will represent the "item" to be returned from the reader.  This
	 * class is used via the {@link BeanWrapperFieldSetMapper}.  If more complex logic is
	 * required, providing your own {@link FieldSetMapper} via
	 * {@link FlatFileItemReaderBuilder#fieldSetMapper} is required.
	 *
	 * @param targetType The class to map to
	 * @return The current instance of the builder.
	 * @see BeanWrapperFieldSetMapper#setTargetType(Class)
	 */
	public FlatFileItemReaderBuilder<T> targetType(Class<? extends T> targetType) {
		this.targetType = targetType;
		return this;
	}

	/**
	 * Configures the id of a prototype scoped bean to be used as the item returned by the
	 * reader.
	 *
	 * @param prototypeBeanName the name of a prototype scoped bean
	 * @return The current instance of the builder.
	 * @see BeanWrapperFieldSetMapper#setPrototypeBeanName(String)
	 */
	public FlatFileItemReaderBuilder<T> prototypeBeanName(String prototypeBeanName) {
		this.prototypeBeanName = prototypeBeanName;
		return this;
	}

	/**
	 * Configures the {@link BeanFactory} used to create the beans that are returned as
	 * items.
	 *
	 * @param beanFactory a {@link BeanFactory}
	 * @return The current instance of the builder.
	 * @see BeanWrapperFieldSetMapper#setBeanFactory(BeanFactory)
	 */
	public FlatFileItemReaderBuilder<T> beanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		return this;
	}

	/**
	 * Register custom type converters for beans being mapped.
	 *
	 * @param customEditors a {@link Map} of editors
	 * @return The current instance of the builder.
	 * @see BeanWrapperFieldSetMapper#setCustomEditors(Map)
	 */
	public FlatFileItemReaderBuilder<T> customEditors(Map<Class<?>, PropertyEditor> customEditors) {
		if(customEditors != null) {
			this.customEditors.putAll(customEditors);
		}

		return this;
	}

	/**
	 * Configures the maximum tolerance between the actual spelling of a field's name and
	 * the property's name.
	 *
	 * @param distanceLimit distance limit to set
	 * @return The current instance of the builder.
	 * @see BeanWrapperFieldSetMapper#setDistanceLimit(int)
	 */
	public FlatFileItemReaderBuilder<T> distanceLimit(int distanceLimit) {
		this.distanceLimit = distanceLimit;
		return this;
	}

	/**
	 * If set to true, mapping will fail if the {@link org.springframework.batch.item.file.transform.FieldSet}
	 * contains fields that cannot be mapped to the bean.
	 *
	 * @param beanMapperStrict defaults to false
	 * @return The current instance of the builder.
	 * @see BeanWrapperFieldSetMapper#setStrict(boolean)
	 */
	public FlatFileItemReaderBuilder<T> beanMapperStrict(boolean beanMapperStrict) {
		this.beanMapperStrict = beanMapperStrict;
		return this;
	}

	/**
	 * Builds the {@link FlatFileItemReader}.
	 *
	 * @return a {@link FlatFileItemReader}
	 */
	public FlatFileItemReader<T> build() {
		if(this.saveState) {
			Assert.state(StringUtils.hasText(this.name),
					"A name is required when saveState is set to true.");
		}

		if(this.resource == null) {
			logger.debug("The resource is null.  This is only a valid scenario when " +
					"injecting it later as in when using the MultiResourceItemReader");
		}

		Assert.notNull(this.recordSeparatorPolicy, "A RecordSeparatorPolicy is required.");
		Assert.notNull(this.bufferedReaderFactory, "A BufferedReaderFactory is required.");
		int validatorValue = this.tokenizerValidator.intValue();

		FlatFileItemReader<T> reader = new FlatFileItemReader<>();

		if(StringUtils.hasText(this.name)) {
			reader.setName(this.name);
		}

		if(StringUtils.hasText(this.encoding)) {
			reader.setEncoding(this.encoding);
		}

		reader.setResource(this.resource);

		if(this.lineMapper != null) {
			reader.setLineMapper(this.lineMapper);
		}
		else {
			Assert.state(validatorValue == 0 || validatorValue == 1 || validatorValue == 2 || validatorValue == 4,
					"Only one LineTokenizer option may be configured");

			DefaultLineMapper<T> lineMapper = new DefaultLineMapper<>();

			if(this.lineTokenizer != null) {
				lineMapper.setLineTokenizer(this.lineTokenizer);
			}
			else if(this.fixedLengthBuilder != null) {
				lineMapper.setLineTokenizer(this.fixedLengthBuilder.build());
			}
			else if(this.delimitedBuilder != null) {
				lineMapper.setLineTokenizer(this.delimitedBuilder.build());
			}
			else {
				throw new IllegalStateException("No LineTokenizer implementation was provided.");
			}

			if(this.targetType != null || StringUtils.hasText(this.prototypeBeanName)) {
				BeanWrapperFieldSetMapper<T> mapper = new BeanWrapperFieldSetMapper<>();
				mapper.setTargetType(this.targetType);
				mapper.setPrototypeBeanName(this.prototypeBeanName);
				mapper.setStrict(this.beanMapperStrict);
				mapper.setBeanFactory(this.beanFactory);
				mapper.setDistanceLimit(this.distanceLimit);
				mapper.setCustomEditors(this.customEditors);
				try {
					mapper.afterPropertiesSet();
				}
				catch (Exception e) {
					throw new IllegalStateException("Unable to initialize BeanWrapperFieldSetMapper", e);
				}

				lineMapper.setFieldSetMapper(mapper);
			}
			else if(this.fieldSetMapper != null) {
				lineMapper.setFieldSetMapper(this.fieldSetMapper);
			}
			else {
				throw new IllegalStateException("No FieldSetMapper implementation was provided.");
			}

			reader.setLineMapper(lineMapper);
		}

		reader.setLinesToSkip(this.linesToSkip);
		reader.setComments(this.comments.toArray(new String[this.comments.size()]));

		reader.setSkippedLinesCallback(this.skippedLinesCallback);
		reader.setRecordSeparatorPolicy(this.recordSeparatorPolicy);
		reader.setBufferedReaderFactory(this.bufferedReaderFactory);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setSaveState(this.saveState);
		reader.setStrict(this.strict);

		return reader;
	}

	private void updateTokenizerValidation(Object tokenizer, int index) {
		if(tokenizer != null) {
			this.tokenizerValidator = this.tokenizerValidator.flipBit(index);
		}
		else {
			this.tokenizerValidator = this.tokenizerValidator.clearBit(index);
		}
	}

	/**
	 * A builder for constructing a {@link DelimitedLineTokenizer}
	 *
	 * @param <T> the type of the parent {@link FlatFileItemReaderBuilder}
	 */
	public static class DelimitedBuilder<T> {
		private FlatFileItemReaderBuilder<T> parent;

		private List<String> names = new ArrayList<>();

		private String delimiter;

		private Character quoteCharacter;

		private List<Integer> includedFields = new ArrayList<>();

		private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

		private boolean strict = true;

		protected DelimitedBuilder(FlatFileItemReaderBuilder<T> parent) {
			this.parent = parent;
		}

		/**
		 * Define the delimiter for the file.
		 *
		 * @param delimiter String used as a delimiter between fields.
		 * @return The instance of the builder for chaining.
		 * @see DelimitedLineTokenizer#setDelimiter(String)
		 */
		public DelimitedBuilder<T> delimiter(String delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		/**
		 * Define the character used to quote fields.
		 *
		 * @param quoteCharacter char used to define quoted fields
		 * @return The instance of the builder for chaining.
		 * @see DelimitedLineTokenizer#setQuoteCharacter(char)
		 */
		public DelimitedBuilder<T> quoteCharacter(char quoteCharacter) {
			this.quoteCharacter = quoteCharacter;
			return this;
		}

		/**
		 * A list of indices of the fields within a delimited file to be included
		 *
		 * @param fields indices of the fields
		 * @return The instance of the builder for chaining.
		 * @see DelimitedLineTokenizer#setIncludedFields(int[])
		 */
		public DelimitedBuilder<T> includedFields(Integer... fields) {
			this.includedFields.addAll(Arrays.asList(fields));
			return this;
		}

		/**
		 * Add an index to the list of fields to be included from the file
		 *
		 * @param field the index to be included
		 * @return The instance of the builder for chaining.
		 * @see DelimitedLineTokenizer#setIncludedFields(int[])
		 */
		public DelimitedBuilder<T> addIncludedField(int field) {
			this.includedFields.add(field);
			return this;
		}

		/**
		 * A factory for creating the resulting
		 * {@link org.springframework.batch.item.file.transform.FieldSet}.  Defaults to
		 * {@link DefaultFieldSetFactory}.
		 *
		 * @param fieldSetFactory Factory for creating {@link org.springframework.batch.item.file.transform.FieldSet}
		 * @return The instance of the builder for chaining.
		 * @see DelimitedLineTokenizer#setFieldSetFactory(FieldSetFactory)
		 */
		public DelimitedBuilder<T> fieldSetFactory(FieldSetFactory fieldSetFactory) {
			this.fieldSetFactory = fieldSetFactory;
			return this;
		}

		/**
		 * Names of each of the fields within the fields that are returned in the order
		 * they occur within the delimited file.  Required.
		 *
		 * @param names names of each field
		 * @return The parent {@link FlatFileItemReaderBuilder}
		 * @see DelimitedLineTokenizer#setNames(String[])
		 */
		public FlatFileItemReaderBuilder<T> names(String... names) {
			this.names.addAll(Arrays.asList(names));
			return this.parent;
		}

		/**
		 * Returns a {@link DelimitedLineTokenizer}
		 *
		 * @return {@link DelimitedLineTokenizer}
		 */
		public DelimitedLineTokenizer build() {
			Assert.notNull(this.fieldSetFactory, "A FieldSetFactory is required.");
			Assert.notEmpty(this.names, "A list of field names is required");

			DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

			tokenizer.setNames(this.names.toArray(new String[this.names.size()]));

			if(StringUtils.hasLength(this.delimiter)) {
				tokenizer.setDelimiter(this.delimiter);
			}

			if(this.quoteCharacter != null) {
				tokenizer.setQuoteCharacter(this.quoteCharacter);
			}

			if(!this.includedFields.isEmpty()) {
				Set<Integer> deDupedFields = new HashSet<>(this.includedFields.size());
				deDupedFields.addAll(this.includedFields);
				deDupedFields.remove(null);

				int [] fields = new int[deDupedFields.size()];
				Iterator<Integer> iterator = deDupedFields.iterator();
				for(int i = 0; i < fields.length; i++) {
					fields[i] = iterator.next();
				}

				tokenizer.setIncludedFields(fields);
			}

			tokenizer.setFieldSetFactory(this.fieldSetFactory);
			tokenizer.setStrict(this.strict);

			try {
				tokenizer.afterPropertiesSet();
			}
			catch (Exception e) {
				throw new IllegalStateException("Unable to initialize DelimitedLineTokenizer", e);
			}

			return tokenizer;
		}
	}

	/**
	 * A builder for constructing a {@link FixedLengthTokenizer}
	 *
	 * @param <T> the type of the parent {@link FlatFileItemReaderBuilder}
	 */
	public static class FixedLengthBuilder<T> {
		private FlatFileItemReaderBuilder<T> parent;

		private List<Range> ranges = new ArrayList<>();

		private List<String> names = new ArrayList<>();

		private boolean strict = true;

		private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

		protected FixedLengthBuilder(FlatFileItemReaderBuilder<T> parent) {
			this.parent = parent;
		}

		/**
		 * The column ranges for each field
		 *
		 * @param ranges column ranges
		 * @return This instance for chaining
		 * @see FixedLengthTokenizer#setColumns(Range[])
		 */
		public FixedLengthBuilder<T> columns(Range... ranges) {
			this.ranges.addAll(Arrays.asList(ranges));
			return this;
		}

		/**
		 * Add a column range to the existing list
		 *
		 * @param range a new column range
		 * @return This instance for chaining
		 * @see FixedLengthTokenizer#setColumns(Range[])
		 */
		public FixedLengthBuilder<T> addColumns(Range range) {
			this.ranges.add(range);
			return this;
		}

		/**
		 * Insert a column range to the existing list
		 *
		 * @param range a new column range
		 * @param index index to add it at
		 * @return This instance for chaining
		 * @see FixedLengthTokenizer#setColumns(Range[])
		 */
		public FixedLengthBuilder<T> addColumns(Range range, int index) {
			this.ranges.add(index, range);
			return this;
		}

		/**
		 * The names of the fields to be parsed from the file.  Required.
		 *
		 * @param names names of fields
		 * @return The parent builder
		 * @see FixedLengthTokenizer#setNames(String[])
		 */
		public FlatFileItemReaderBuilder<T> names(String... names) {
			this.names.addAll(Arrays.asList(names));
			return this.parent;
		}

		/**
		 * Boolean indicating if the number of tokens in a line must match the number of
		 * fields (ranges) configured.  Defaults to true.
		 *
		 * @param strict defaults to true
		 * @return This instance for chaining
		 * @see FixedLengthTokenizer#setStrict(boolean)
		 */
		public FixedLengthBuilder<T> strict(boolean strict) {
			this.strict = strict;
			return this;
		}

		/**
		 * A factory for creating the resulting
		 * {@link org.springframework.batch.item.file.transform.FieldSet}.  Defaults to
		 * {@link DefaultFieldSetFactory}.
		 * @param fieldSetFactory Factory for creating {@link org.springframework.batch.item.file.transform.FieldSet}
		 * @return The instance of the builder for chaining.
		 * @see FixedLengthTokenizer#setFieldSetFactory(FieldSetFactory)
		 */
		public FixedLengthBuilder<T> fieldSetFactory(FieldSetFactory fieldSetFactory) {
			this.fieldSetFactory = fieldSetFactory;
			return this;
		}

		/**
		 * Returns a {@link FixedLengthTokenizer}
		 *
		 * @return a {@link FixedLengthTokenizer}
		 */
		public FixedLengthTokenizer build() {
			Assert.notNull(this.fieldSetFactory, "A FieldSetFactory is required.");
			Assert.notEmpty(this.names, "A list of field names is required.");
			Assert.notEmpty(this.ranges, "A list of column ranges is required.");

			FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();

			tokenizer.setNames(this.names.toArray(new String[this.names.size()]));
			tokenizer.setColumns(this.ranges.toArray(new Range[this.ranges.size()]));
			tokenizer.setFieldSetFactory(this.fieldSetFactory);
			tokenizer.setStrict(this.strict);

			return tokenizer;
		}
	}
}
