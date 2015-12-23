/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Before;
import org.junit.Test;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Chris Cranford
 * 
 */
public class JpaItemWriterPersistTests {

	EntityManagerFactory emf;

	JpaItemWriter<Object> writer;

	@Before
	public void setUp() throws Exception {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		writer = new JpaItemWriter<Object>();
		writer.setUsePersist(true);
		emf = mock(EntityManagerFactory.class,"emf");
		writer.setEntityManagerFactory(emf);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new JpaItemWriter<Object>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue("Wrong message for exception: " + e.getMessage(),
					e.getMessage().indexOf("EntityManagerFactory") >= 0);
		}
	}
	
	@Test
	public void testPersist() throws Exception {
		EntityManager em = mock(EntityManager.class, "em");
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		List<String> items = Arrays.asList(new String[] { "persist1", "persist2" });
		writer.write(items);		
		TransactionSynchronizationManager.unbindResource(emf);
	}
	
}
