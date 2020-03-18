/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.batch.item.database;

import java.util.Collections;

import javax.persistence.EntityManagerFactory;

import org.junit.runner.RunWith;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JpaPagingItemReaderParameterTests extends AbstractPagingItemReaderParameterTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

    @Override
	protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {

		String jpqlQuery = "select f from Foo f where f.value >= :limit";

		JpaPagingItemReader<Foo> reader = new JpaPagingItemReader<>();
		reader.setQueryString(jpqlQuery);
		reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 2));
		reader.setEntityManagerFactory(entityManagerFactory);
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}

}
