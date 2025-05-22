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

import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.Isolation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableMongoJobRepository {

	String mongoOperationsRef() default "mongoTemplate";

	/**
	 * Set the {@link MongoTransactionManager} to use in the job repository.
	 * @return the bean name of the transaction manager to use. Defaults to
	 * {@literal transactionManager}
	 */
	String transactionManagerRef() default "transactionManager";

	/**
	 * Set the isolation level for create parameter value. Defaults to
	 * {@link Isolation#SERIALIZABLE}.
	 * @return the value of the isolation level for create parameter
	 */
	Isolation isolationLevelForCreate() default Isolation.SERIALIZABLE;

	/**
	 * Set the value of the {@code validateTransactionState} parameter. Defaults to
	 * {@code true}.
	 * @return true if the transaction state should be validated, false otherwise
	 */
	boolean validateTransactionState() default true;

	/**
	 * The generator that determines a unique key for identifying job instance objects
	 * @return the bean name of the job key generator to use. Defaults to
	 * {@literal jobKeyGenerator}.
	 *
	 */
	String jobKeyGeneratorRef() default "jobKeyGenerator";

}
