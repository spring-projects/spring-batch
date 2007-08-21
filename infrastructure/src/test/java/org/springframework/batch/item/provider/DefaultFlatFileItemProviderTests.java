/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.item.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.ValidationException;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.io.file.support.DefaultFlatFileInputSource;
import org.springframework.batch.item.provider.DefaultFlatFileItemProvider;
import org.springframework.batch.item.validator.Validator;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.core.io.ByteArrayResource;

/**
 * Unit tests for {@link DefaultFlatFileItemProvider}
 * 
 * @author Robert Kasanicky
 */
public class DefaultFlatFileItemProviderTests extends TestCase {
	
	public static String FOO = "foo";
	// object under test
	private DefaultFlatFileItemProvider itemProvider = new DefaultFlatFileItemProvider();
	
	// Input source
	private DefaultFlatFileInputSource source;
	
	//mock mapper
	private FieldSetMapper mapper;
	
	private List list = new ArrayList();
	
	// create mock objects and inject them into data provider
	protected void setUp() throws Exception {
		source = new DefaultFlatFileInputSource();
		source.setResource(new ByteArrayResource("a,b".getBytes()));
		mapper = new FieldSetMapper() {
			public Object mapLine(FieldSet fs) {
				return FOO;
			}
		};
		itemProvider.setSource(source);
		itemProvider.setMapper(mapper);
		assertTrue(Restartable.class.isAssignableFrom(DefaultFlatFileInputSource.class));
		assertTrue(FieldSetInputSource.class.isAssignableFrom(DefaultFlatFileInputSource.class));
		assertTrue(StatisticsProvider.class.isAssignableFrom(DefaultFlatFileInputSource.class));
	}

	/**
	 * Uses input template to provide the domain object.
	 */
	public void testNext() {	
		Object result = itemProvider.next();
		assertSame("domain object is provided by the input template", FOO, result);
	}
	
	/**
	 * Uses input template to provide the domain object.
	 */
	public void testNextWithValidator() {	
		itemProvider.setValidator(new Validator() {
			public void validate(Object value) throws ValidationException {
				list.add(value);
			}
		});
		itemProvider.next();
		assertSame("domain object is provided by the input template", FOO, list.get(0));
	}

	/**
	 * Uses input template to provide the domain object.
	 */
	public void testNextWithValidatorAndInvalidData() {	
		itemProvider.setValidator(new Validator() {
			public void validate(Object value) throws ValidationException {
				throw new ValidationException("Invalid input");
			}
		});
		try {
			itemProvider.next();
			fail("Expected ValidationException");
		} catch (ValidationException e) {
			// expected
			assertEquals("Invalid input", e.getMessage());
		}
	}

	/**
	 * Gets statistics from the input template
	 */
	public void testGetStatistics() {
		Properties statistics = ((StatisticsProvider) source).getStatistics();		
		assertEquals(statistics, itemProvider.getStatistics());
	}
	
	/**
	 * Gets statistics from the input template
	 */
	public void testGetStatisticsWithoutStatisticsProvider() {
		itemProvider.setSource(null);
		Properties props = itemProvider.getStatistics();
		assertEquals(null, props.getProperty("a"));
	}

	/**
	 * Gets restart data from the input template
	 */
	public void testGetRestartData() {
		RestartData data = ((Restartable) source).getRestartData();
		assertEquals(data.getProperties(), itemProvider.getRestartData().getProperties());
	}
	
	/**
	 * Forwarded restart data to input template
	 */
	public void testRestoreFrom() {
		
		final List list = new ArrayList();
		
		RestartData data = new RestartData() {

			public Properties getProperties() {
				list.add(FOO);
				return ((Restartable) source).getRestartData().getProperties();
			}};
		
		itemProvider.restoreFrom(data);
		
		//assertEquals(1, list.size()); getProperties are called multiple times due to null checks
		assertTrue(list.size() > 0);
	}

	/**
	 * Forward restart data to input template
	 * @throws Exception 
	 */
	public void testRestoreFromWithoutRestartable() throws Exception {
		itemProvider.setSource(null);
		try {
			itemProvider.restoreFrom(new GenericRestartData(PropertiesConverter.stringToProperties("value=bar")));
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Forward restart data to input template
	 * @throws Exception 
	 */
	public void testGetRestartDataWithoutRestartable() throws Exception {
		itemProvider.setSource(null);
		try {
			itemProvider.getRestartData();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}


}
