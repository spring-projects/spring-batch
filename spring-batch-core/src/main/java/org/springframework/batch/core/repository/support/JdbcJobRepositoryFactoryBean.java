/*
 * Copyright 2002-2025 the original author or authors.
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
package org.springframework.batch.core.repository.support;

import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.nio.charset.Charset;

/**
 * A {@link FactoryBean} that automates the creation of a {@link SimpleJobRepository}
 * using JDBC DAO implementations which persist batch metadata in a relational database.
 * Requires the user to describe what kind of database they are using.
 *
 * @author Ben Hale
 * @author Lucas Ward
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class JdbcJobRepositoryFactoryBean extends JobRepositoryFactoryBean {

    /**
     * @param type a value from the {@link java.sql.Types} class to indicate the type to
     * use for a CLOB
     */
    public void setClobType(int type) {
        super.setClobType(type);
    }

    /**
     * A custom implementation of the {@link ExecutionContextSerializer}. The default, if
     * not injected, is the {@link DefaultExecutionContextSerializer}.
     * @param serializer used to serialize/deserialize
     * {@link org.springframework.batch.item.ExecutionContext}
     * @see ExecutionContextSerializer
     */
    public void setSerializer(ExecutionContextSerializer serializer) {
        super.setSerializer(serializer);
    }

    /**
     * Public setter for the length of long string columns in database. Do not set this if
     * you haven't modified the schema. Note this value will be used for the exit message
     * in both {@link JdbcJobExecutionDao} and {@link JdbcStepExecutionDao} and also the
     * short version of the execution context in {@link JdbcExecutionContextDao} . If you
     * want to use separate values for exit message and short context, then use
     * {@link #setMaxVarCharLengthForExitMessage(int)} and
     * {@link #setMaxVarCharLengthForShortContext(int)}. For databases with multi-byte
     * character sets this number can be smaller (by up to a factor of 2 for 2-byte
     * characters) than the declaration of the column length in the DDL for the tables.
     * @param maxVarCharLength the exitMessageLength to set
     */
    public void setMaxVarCharLength(int maxVarCharLength) {
        super.setMaxVarCharLength(maxVarCharLength);
    }

    /**
     * Public setter for the length of short context string column in database. Do not set
     * this if you haven't modified the schema. For databases with multi-byte character
     * sets this number can be smaller (by up to a factor of 2 for 2-byte characters) than
     * the declaration of the column length in the DDL for the tables. Defaults to
     * {@link AbstractJdbcBatchMetadataDao#DEFAULT_SHORT_CONTEXT_LENGTH}
     * @param maxVarCharLengthForShortContext the short context length to set
     * @since 5.1
     */
    public void setMaxVarCharLengthForShortContext(int maxVarCharLengthForShortContext) {
        super.setMaxVarCharLengthForShortContext(maxVarCharLengthForShortContext);
    }

    /**
     * Public setter for the length of the exit message in both
     * {@link JdbcJobExecutionDao} and {@link JdbcStepExecutionDao}. Do not set this if
     * you haven't modified the schema. For databases with multi-byte character sets this
     * number can be smaller (by up to a factor of 2 for 2-byte characters) than the
     * declaration of the column length in the DDL for the tables. Defaults to
     * {@link AbstractJdbcBatchMetadataDao#DEFAULT_EXIT_MESSAGE_LENGTH}.
     * @param maxVarCharLengthForExitMessage the exitMessageLength to set
     * @since 5.1
     */
    public void setMaxVarCharLengthForExitMessage(int maxVarCharLengthForExitMessage) {
        super.setMaxVarCharLengthForExitMessage(maxVarCharLengthForExitMessage);
    }

    /**
     * Public setter for the {@link DataSource}.
     * @param dataSource a {@link DataSource}
     */
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
    }

    /**
     * Public setter for the {@link JdbcOperations}. If this property is not set
     * explicitly, a new {@link JdbcTemplate} will be created for the configured
     * DataSource by default.
     * @param jdbcOperations a {@link JdbcOperations}
     */
    public void setJdbcOperations(JdbcOperations jdbcOperations) {
        super.setJdbcOperations(jdbcOperations);
    }

    /**
     * Sets the database type.
     * @param dbType as specified by {@link DefaultDataFieldMaxValueIncrementerFactory}
     */
    public void setDatabaseType(String dbType) {
        super.setDatabaseType(dbType);
    }

    /**
     * Sets the table prefix for all the batch meta-data tables.
     * @param tablePrefix prefix prepended to batch meta-data tables
     */
    public void setTablePrefix(String tablePrefix) {
        super.setTablePrefix(tablePrefix);
    }

    public void setIncrementerFactory(DataFieldMaxValueIncrementerFactory incrementerFactory) {
        super.setIncrementerFactory(incrementerFactory);
    }

    /**
     * * Sets the generator for creating the key used in identifying unique {link
     * JobInstance} objects
     * @param jobKeyGenerator a {@link JobKeyGenerator}
     * @since 5.1
     */
    public void setJobKeyGenerator(JobKeyGenerator jobKeyGenerator) {
        super.setJobKeyGenerator(jobKeyGenerator);
    }

    /**
     * Set the {@link Charset} to use when serializing/deserializing the execution
     * context. Defaults to "UTF-8". Must not be {@code null}.
     * @param charset to use when serializing/deserializing the execution context.
     * @see JdbcExecutionContextDao#setCharset(Charset)
     * @since 5.0
     */
    public void setCharset(@NonNull Charset charset) {
        super.setCharset(charset);
    }

    /**
     * Set the conversion service to use in the job repository. This service is used to
     * convert job parameters from String literal to typed values and vice versa.
     * @param conversionService the conversion service to use
     * @since 5.0
     */
    public void setConversionService(@NonNull ConfigurableConversionService conversionService) {
        super.setConversionService(conversionService);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }
}
