/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * @author Michael Minella
 */
public class FlatFileItemReaderBuilder<T> {

	private String name;

	private boolean strict = true;

	private RecordSeparatorPolicy recordSeparatorPolicy =
			new SimpleRecordSeparatorPolicy();

	private Resource resource;

	private int maxItemCount = Integer.MAX_VALUE;

	private List<String> comments = new ArrayList<>();

	private int linesToSkip = 0;

	private LineCallbackHandler skippedLinesCallback;

	private LineMapper<T> lineMapper;

	private FieldSetMapper<T> fieldSetMapper;

	private LineTokenizer lineTokenizer;

	private DelimitedBuilder<T> delimitedBuilder;

	private FixedLengthBuilder<T> fixedLengthBuilder;

	private Class<? extends T> beanMapperClass;

	private boolean saveState = true;

	public FlatFileItemReaderBuilder<T> strict(boolean strict) {
		this.strict = strict;
		return this;
	}

	public FlatFileItemReaderBuilder<T> recordSeparatorPolicy(RecordSeparatorPolicy policy) {
		this.recordSeparatorPolicy = policy;
		return this;
	}

	public FlatFileItemReaderBuilder<T> resource(Resource resource) {
		this.resource = resource;
		return this;
	}

	public FlatFileItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;
		return this;
	}

	public FlatFileItemReaderBuilder<T> comments(String[] comments) {
		this.comments.addAll(Arrays.asList(comments));
		return this;
	}

	public FlatFileItemReaderBuilder<T> addComment(String comment) {
		this.comments.add(comment);
		return this;
	}

	public FlatFileItemReaderBuilder<T> linesToSkip(int linesToSkip) {
		this.linesToSkip = linesToSkip;
		return this;
	}

	public FlatFileItemReaderBuilder<T> skippedLinesCallback(LineCallbackHandler callback) {
		this.skippedLinesCallback = callback;
		return this;
	}

	public FlatFileItemReaderBuilder<T> lineMapper(LineMapper<T> lineMapper) {
		this.lineMapper = lineMapper;
		return this;
	}

	public FlatFileItemReaderBuilder<T> fieldSetMapper(FieldSetMapper<T> mapper) {
		this.fieldSetMapper = mapper;
		return this;
	}

	public FlatFileItemReaderBuilder<T> lineTokenizer(LineTokenizer tokenizer) {
		this.lineTokenizer = tokenizer;
		return this;
	}

	public DelimitedBuilder<T> delimited() {
		this.delimitedBuilder = new DelimitedBuilder<>(this);
		return this.delimitedBuilder;
	}

	public FixedLengthBuilder<T> fixedLength() {
		this.fixedLengthBuilder = new FixedLengthBuilder<>(this);
		return this.fixedLengthBuilder;
	}

	public FlatFileItemReaderBuilder<T> beanMapperClass(Class beanMapperClass) {
		this.beanMapperClass = beanMapperClass;
		return this;
	}

	public FlatFileItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;
		return this;
	}

	public FlatFileItemReaderBuilder<T> name(String name) {
		this.name = name;
		return this;
	}

	public FlatFileItemReader<T> build() throws Exception {
		FlatFileItemReader<T> reader = new FlatFileItemReader<>();

		reader.setName(this.name);
		reader.setResource(this.resource);

		if(this.lineMapper != null) {
			reader.setLineMapper(this.lineMapper);
		}
		else {
			DefaultLineMapper<T> lineMapper = new DefaultLineMapper<>();

			if(this.lineTokenizer != null) {
				lineMapper.setLineTokenizer(this.lineTokenizer);
				lineMapper.setFieldSetMapper(this.fieldSetMapper);
			}
			else if(this.fixedLengthBuilder != null) {
				lineMapper.setLineTokenizer(this.fixedLengthBuilder.build());
			}
			else if(this.delimitedBuilder != null) {
				lineMapper.setLineTokenizer(this.delimitedBuilder.build());
			}
			else {
				throw new IllegalStateException("No LineTokenizer implementation was provided");
			}

			if(this.beanMapperClass != null) {
				BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
				mapper.setTargetType(this.beanMapperClass);
				mapper.afterPropertiesSet();

				lineMapper.setFieldSetMapper(mapper);
			}

			reader.setLineMapper(lineMapper);
		}

		reader.setLinesToSkip(this.linesToSkip);
		reader.setComments(this.comments.toArray(new String[this.comments.size()]));
		reader.setSkippedLinesCallback(this.skippedLinesCallback);
		reader.setRecordSeparatorPolicy(this.recordSeparatorPolicy);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setSaveState(this.saveState);
		reader.setStrict(this.strict);

		return reader;
	}

	public static class DelimitedBuilder<T> {
		private FlatFileItemReaderBuilder<T> parent;

		private List<String> names = new ArrayList<>();

		private String delimiter;

		private Character quoteCharacter;

		private List<Integer> includedFields = new ArrayList<>();

		private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

		private boolean strict = true;

		public DelimitedBuilder(FlatFileItemReaderBuilder<T> parent) {
			this.parent = parent;
		}

		public DelimitedBuilder<T> delimiter(String delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		public DelimitedBuilder<T> quoteCharacter(char quoteCharacter) {
			this.quoteCharacter = quoteCharacter;
			return this;
		}

		public DelimitedBuilder<T> includedFields(Integer[] fields) {
			this.includedFields.addAll(Arrays.asList(fields));
			return this;
		}

		public DelimitedBuilder<T> addIncludedField(int field) {
			this.includedFields.add(field);
			return this;
		}

		public DelimitedBuilder<T> fieldSetFactory(FieldSetFactory fieldSetFactory) {
			this.fieldSetFactory = fieldSetFactory;
			return this;
		}

		public FlatFileItemReaderBuilder<T> names(String [] names) {
			this.names.addAll(Arrays.asList(names));
			return this.parent;
		}

		public DelimitedLineTokenizer build() throws Exception {
			DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

			tokenizer.setNames(this.names.toArray(new String[this.names.size()]));

			if(StringUtils.hasText(this.delimiter)) {
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

			tokenizer.afterPropertiesSet();

			return tokenizer;
		}
	}

	public static class FixedLengthBuilder<T> {
		private FlatFileItemReaderBuilder<T> parent;

		private List<Range> ranges = new ArrayList<>();

		private int maxRange = 0;

		private List<String> names = new ArrayList<>();

		private boolean strict = true;

		private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

		public FixedLengthBuilder(FlatFileItemReaderBuilder<T> parent) {
			this.parent = parent;
		}

		public FixedLengthBuilder<T> ranges(Range[] ranges) {
			this.ranges.addAll(Arrays.asList(ranges));
			return this;
		}

		public FixedLengthBuilder<T> addRange(Range range) {
			this.ranges.add(range);
			return this;
		}

		public FixedLengthBuilder<T> addRange(Range range, int index) {
			this.ranges.add(index, range);
			return this;
		}

		public FixedLengthBuilder<T> maxRange(int maxRange) {
			this.maxRange = maxRange;
			return this;
		}

		public FlatFileItemReaderBuilder<T> names(String [] names) {
			this.names.addAll(Arrays.asList(names));
			return this.parent;
		}

		public FixedLengthBuilder<T> strict(boolean strict) {
			this.strict = strict;
			return this;
		}

		public FixedLengthBuilder<T> fieldSetFactory(FieldSetFactory factory) {
			this.fieldSetFactory = factory;
			return this;
		}

		public FixedLengthTokenizer build() {
			FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();

			tokenizer.setNames(this.names.toArray(new String[this.names.size()]));
			tokenizer.setColumns(this.ranges.toArray(new Range[this.ranges.size()]));
			tokenizer.setFieldSetFactory(this.fieldSetFactory);
			tokenizer.setStrict(this.strict);

			return tokenizer;
		}
	}
}
