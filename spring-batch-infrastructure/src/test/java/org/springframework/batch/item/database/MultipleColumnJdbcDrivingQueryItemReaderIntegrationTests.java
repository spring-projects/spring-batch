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
package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.MultipleColumnJdbcKeyCollector;

/**
 * @author Lucas Ward
 *
 */
public class MultipleColumnJdbcDrivingQueryItemReaderIntegrationTests extends
		AbstractJdbcItemReaderIntegrationTests {

	protected ItemReader createItemReader() throws Exception {

		MultipleColumnJdbcKeyCollector keyGenerator =
			new MultipleColumnJdbcKeyCollector(getJdbcTemplate(),
					"SELECT ID, VALUE from T_FOOS order by ID, VALUE");

		keyGenerator.setRestartSql("SELECT ID, VALUE from T_FOOS where ID > ? and VALUE > ? order by ID");
		DrivingQueryItemReader inputSource = new DrivingQueryItemReader();
		inputSource.setSaveState(true);
		inputSource.setKeyCollector(keyGenerator);
		FooItemReader fooItemReader = new FooItemReader(inputSource, getJdbcTemplate());
		fooItemReader.setFooDao(new CompositeKeyFooDao(getJdbcTemplate()));
		return fooItemReader;
	}

}
