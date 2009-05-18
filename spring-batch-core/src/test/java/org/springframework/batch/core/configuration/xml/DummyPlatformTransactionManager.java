/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class DummyPlatformTransactionManager implements PlatformTransactionManager, BeanNameAware {

	private String name;

	public String getName() {
		return name;
	}

	public void setBeanName(String name) {
		this.name = name;
	}

	public void commit(TransactionStatus status) throws TransactionException {
	}

	public void rollback(TransactionStatus status) throws TransactionException {
	}

	public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
		return null;
	}
}
