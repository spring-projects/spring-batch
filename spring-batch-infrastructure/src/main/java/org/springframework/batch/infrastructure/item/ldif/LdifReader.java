/*
 * Copyright 2005-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.ldif;

import org.jspecify.annotations.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.LdapAttributes;
import org.springframework.ldap.ldif.parser.LdifParser;
import org.springframework.util.Assert;

/**
 * The {@link LdifReader LdifReader} is an adaptation of the {@link FlatFileItemReader
 * FlatFileItemReader} built around an {@link LdifParser LdifParser}.
 * <p>
 * Unlike the {@link FlatFileItemReader FlatFileItemReader}, the {@link LdifReader
 * LdifReader} does not require a mapper. Instead, this version of the {@link LdifReader
 * LdifReader} simply returns an {@link LdapAttributes LdapAttributes} object which can be
 * consumed and manipulated as necessary by {@link ItemProcessor ItemProcessor} or any
 * output service. Alternatively, the {@link RecordMapper RecordMapper} interface can be
 * implemented and set in a {@link MappingLdifReader MappingLdifReader} to map records to
 * objects for return.
 * <p>
 * {@link LdifReader LdifReader} usage is mimics that of the {@link FlatFileItemReader
 * FlatFileItemReader} for all intensive purposes. Adjustments have been made to process
 * records instead of lines, however. As such, the {@link #recordsToSkip recordsToSkip}
 * attribute indicates the number of records from the top of the file that should not be
 * processed. Implementations of the {@link RecordCallbackHandler RecordCallbackHandler}
 * interface can be used to execute operations on those skipped records.
 * <p>
 * As with the {@link FlatFileItemReader FlatFileItemReader}, the {@link #strict strict}
 * option differentiates between whether or not to require the resource to exist before
 * processing. In the case of a value set to false, a warning is logged instead of an
 * exception being thrown.
 *
 * <p>
 * This reader is <b>not</b> thread-safe.
 * </p>
 *
 * @author Keith Barlow
 * @author Mahmoud Ben Hassine
 * @author Jimmy Praet
 *
 */
public class LdifReader extends AbstractItemCountingItemStreamItemReader<LdapAttributes>
		implements ResourceAwareItemReaderItemStream<LdapAttributes>, InitializingBean {

	private static final Log LOG = LogFactory.getLog(LdifReader.class);

	private Resource resource;

	private @Nullable LdifParser ldifParser;

	private int recordCount = 0;

	private int recordsToSkip = 0;

	private boolean strict = true;

	private @Nullable RecordCallbackHandler skippedRecordsCallback;

	/**
	 * Create a new {@link LdifReader} instance with no resource. A resource must be set
	 * before calling {@link #open(ExecutionContext)}.
	 * @since 6.0
	 */
	public LdifReader(Resource resource) {
		Assert.notNull(resource, "The resource must not be null");
		this.resource = resource;
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link #open(ExecutionContext)} if the input resource does not exist.
	 * @param strict true by default
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	/**
	 * {@link RecordCallbackHandler RecordCallbackHandler} implementations can be used to
	 * take action on skipped records.
	 * @param skippedRecordsCallback will be called for each one of the initial skipped
	 * lines before any items are read.
	 */
	public void setSkippedRecordsCallback(RecordCallbackHandler skippedRecordsCallback) {
		this.skippedRecordsCallback = skippedRecordsCallback;
	}

	/**
	 * Public setter for the number of lines to skip at the start of a file. Can be used
	 * if the file contains a header without useful (column name) information, and without
	 * a comment delimiter at the beginning of the lines.
	 * @param recordsToSkip the number of lines to skip
	 */
	public void setRecordsToSkip(int recordsToSkip) {
		this.recordsToSkip = recordsToSkip;
	}

	@Override
	protected void doClose() throws Exception {
		if (ldifParser != null) {
			ldifParser.close();
		}
		this.recordCount = 0;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	protected void doOpen() throws Exception {
		if (!resource.exists()) {
			if (strict) {
				throw new IllegalStateException("Input resource must exist (reader is in 'strict' mode): " + resource);
			}
			else {
				LOG.warn("Input resource does not exist " + resource.getDescription());
				return;
			}
		}

		ldifParser.open();

		for (int i = 0; i < recordsToSkip; i++) {
			LdapAttributes record = ldifParser.getRecord();
			if (skippedRecordsCallback != null) {
				skippedRecordsCallback.handleRecord(record);
			}
		}
	}

	@Override
	protected @Nullable LdapAttributes doRead() throws Exception {
		LdapAttributes attributes = null;

		try {
			if (ldifParser != null) {
				while (attributes == null && ldifParser.hasMoreRecords()) {
					attributes = ldifParser.getRecord();
				}
				recordCount++;
			}

			return attributes;

		}
		catch (Exception ex) {
			LOG.error("Parsing error at record " + recordCount + " in resource=" + resource.getDescription()
					+ ", input=[" + attributes + "]", ex);
			throw ex;
		}
	}

	/**
	 * Establishes the resource that will be used as the input for the LdifReader.
	 * @param resource the resource that will be read.
	 */
	@Override
	public void setResource(Resource resource) {
		this.resource = resource;
		this.ldifParser = new LdifParser(resource);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.ldifParser == null) {
			this.ldifParser = new LdifParser(this.resource);
		}
	}

}
