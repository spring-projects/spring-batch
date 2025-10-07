/*
 * Copyright 2010-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database;

import org.junit.jupiter.api.Disabled;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Disabled("see FIXME in init-foo-schema.sql")
@SpringJUnitConfig(locations = "classpath:data-source-context.xml")
public class StoredProcedureItemReaderIntegrationTests extends AbstractDataSourceItemReaderIntegrationTests {

	@Override
	protected ItemReader<Foo> createItemReader() {
		StoredProcedureItemReader<Foo> reader = new StoredProcedureItemReader<>(dataSource, "read_foos",
				new FooRowMapper());
		reader.setVerifyCursorPosition(false);
		return reader;
	}

}
