/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.support.DatabaseType;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.Isolation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Types;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableJdbcJobRepository {

	/**
	 * Set the type of the data source to use in the job repository. The default type will
	 * be introspected from the datasource's metadata.
	 * @since 5.1
	 * @see DatabaseType
	 * @return the type of data source.
	 */
	String databaseType() default "";

	/**
	 * Set the value of the {@code validateTransactionState} parameter. Defaults to
	 * {@code true}.
	 * @return true if the transaction state should be validated, false otherwise
	 */
	boolean validateTransactionState() default true;

	/**
	 * Set the isolation level for create parameter value. Defaults to
	 * {@link Isolation#SERIALIZABLE}.
	 * @return the value of the isolation level for create parameter
	 */
	Isolation isolationLevelForCreate() default Isolation.SERIALIZABLE;

	/**
	 * The charset to use in the job repository
	 * @return the charset to use. Defaults to {@literal UTF-8}.
	 */
	String charset() default "UTF-8";

	/**
	 * The Batch tables prefix. Defaults to {@literal "BATCH_"}.
	 * @return the Batch table prefix
	 */
	String tablePrefix() default AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

	/**
	 * The maximum length of exit messages in the database.
	 * @return the maximum length of exit messages in the database
	 */
	int maxVarCharLength() default AbstractJdbcBatchMetadataDao.DEFAULT_EXIT_MESSAGE_LENGTH;

	/**
	 * The type of large objects.
	 * @return the type of large objects.
	 */
	int clobType() default Types.CLOB;

	/**
	 * Set the data source to use in the job repository.
	 * @return the bean name of the data source to use. Default to {@literal dataSource}.
	 */
	String dataSourceRef() default "dataSource";

	/**
	 * Set the {@link DataSourceTransactionManager} to use in the job repository.
	 * @return the bean name of the transaction manager to use. Defaults to
	 * {@literal transactionManager}
	 */
	String transactionManagerRef() default "transactionManager";

	String jdbcOperationsRef() default "jdbcTemplate";

	/**
	 * The generator that determines a unique key for identifying job instance objects
	 * @return the bean name of the job key generator to use. Defaults to
	 * {@literal jobKeyGenerator}.
	 *
	 * @since 5.1
	 */
	String jobKeyGeneratorRef() default "jobKeyGenerator";

	/**
	 * Set the execution context serializer to use in the job repository.
	 * @return the bean name of the execution context serializer to use. Default to
	 * {@literal executionContextSerializer}.
	 */
	String executionContextSerializerRef() default "executionContextSerializer";

	/**
	 * The incrementer factory to use in various DAOs.
	 * @return the bean name of the incrementer factory to use. Defaults to
	 * {@literal incrementerFactory}.
	 */
	String incrementerFactoryRef() default "incrementerFactory";

	/**
	 * Set the conversion service to use in the job repository. This service is used to
	 * convert job parameters from String literal to typed values and vice versa.
	 * @return the bean name of the conversion service to use. Defaults to
	 * {@literal conversionService}
	 */
	String conversionServiceRef() default "conversionService";

}
