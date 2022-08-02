/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.item.database.support;

import static org.mockito.Mockito.mock;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lucas Ward
 * @author Will Schipp
 */
class ColumnMapExecutionContextRowMapperTests {

	private ColumnMapItemPreparedStatementSetter mapper;

	private Map<String, Object> key;

	private PreparedStatement ps;

	@BeforeEach
	void setUp() {
		ps = mock(PreparedStatement.class);
		mapper = new ColumnMapItemPreparedStatementSetter();

		key = new LinkedHashMap<>(2);
		key.put("1", Integer.valueOf(1));
		key.put("2", Integer.valueOf(2));
	}

	@Test
	void testCreateExecutionContextFromEmptyKeys() throws Exception {

		mapper.setValues(new HashMap<>(), ps);
	}

	@Test
	void testCreateSetter() throws Exception {

		ps.setObject(1, Integer.valueOf(1));
		ps.setObject(2, Integer.valueOf(2));
		mapper.setValues(key, ps);
	}

}
