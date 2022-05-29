/*
 * Copyright 2005-2019 the original author or authors.
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
package org.springframework.batch.item.ldif;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.ldap.core.LdapAttributes;
import org.springframework.ldap.ldif.parser.LdifParser;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * The {@link MappingLdifReader MappingLdifReader} is an adaptation of the
 * {@link org.springframework.batch.item.file.FlatFileItemReader FlatFileItemReader} built
 * around an {@link LdifParser LdifParser}. It differs from the standard {@link LdifReader
 * LdifReader} in its ability to map {@link LdapAttributes LdapAttributes} objects to
 * POJOs.
 * <p>
 * The {@link MappingLdifReader MappingLdifReader} <i>requires</i> an {@link RecordMapper
 * RecordMapper} implementation. If mapping is not required, the {@link LdifReader
 * LdifReader} should be used instead. It simply returns an {@link LdapAttributes
 * LdapAttributes} object which can be consumed and manipulated as necessary by
 * {@link org.springframework.batch.item.ItemProcessor ItemProcessor} or any output
 * service.
 * <p>
 * As with the {@link org.springframework.batch.item.file.FlatFileItemReader
 * FlatFileItemReader}, the {@link #strict strict} option differentiates between whether
 * or not to require the resource to exist before processing. In the case of a value set
 * to false, a warning is logged instead of an exception being thrown.
 *
 * @author Keith Barlow
 *
 */
public class MappingLdifReader<T> extends AbstractItemCountingItemStreamItemReader<T>
		implements ResourceAwareItemReaderItemStream<T>, InitializingBean {

	private static final Logger LOG = LoggerFactory.getLogger(MappingLdifReader.class);

	private Resource resource;

	private LdifParser ldifParser;

	private int recordCount = 0;

	private int recordsToSkip = 0;

	private boolean strict = true;

	private RecordCallbackHandler skippedRecordsCallback;

	private RecordMapper<T> recordMapper;

	public MappingLdifReader() {
		setName(ClassUtils.getShortName(MappingLdifReader.class));
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link #open(org.springframework.batch.item.ExecutionContext)} if the input
	 * resource does not exist.
	 * @param strict false by default
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

	/**
	 * Setter for object mapper. This property is required to be set.
	 * @param recordMapper maps record to an object
	 */
	public void setRecordMapper(RecordMapper<T> recordMapper) {
		this.recordMapper = recordMapper;
	}

	@Override
	protected void doClose() throws Exception {
		if (ldifParser != null) {
			ldifParser.close();
		}
		this.recordCount = 0;
	}

	@Override
	protected void doOpen() throws Exception {
		if (resource == null)
			throw new IllegalStateException("A resource has not been set.");

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

	@Nullable
	@Override
	protected T doRead() throws Exception {
		LdapAttributes attributes = null;

		try {
			if (ldifParser != null) {
				while (attributes == null && ldifParser.hasMoreRecords()) {
					attributes = ldifParser.getRecord();
				}
				recordCount++;
				return recordMapper.mapRecord(attributes);
			}

			return null;
		}
		catch (Exception ex) {
			LOG.error("Parsing error at record " + recordCount + " in resource=" + resource.getDescription()
					+ ", input=[" + attributes + "]", ex);
			throw ex;
		}
	}

	public void setResource(Resource resource) {
		this.resource = resource;
		this.ldifParser = new LdifParser(resource);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resource, "A resource is required to parse.");
		Assert.notNull(ldifParser, "A parser is required");
	}

}
