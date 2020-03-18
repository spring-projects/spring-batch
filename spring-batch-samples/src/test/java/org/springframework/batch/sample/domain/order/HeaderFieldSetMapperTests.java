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
package org.springframework.batch.sample.domain.order;

import java.util.Calendar;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.mapper.HeaderFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class HeaderFieldSetMapperTests extends AbstractFieldSetMapperTests {
	private static final long ORDER_ID = 1;
	private static final String DATE = "2007-01-01";

	@Override
	protected Object expectedDomainObject() {
		Order order = new Order();
		Calendar calendar = Calendar.getInstance();
		calendar.set(2007, 0, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		order.setOrderDate(calendar.getTime());
		order.setOrderId(ORDER_ID);
		return order;
	}

	@Override
	protected FieldSet fieldSet() {
		String[] tokens = new String[] { String.valueOf(ORDER_ID), DATE };
		String[] columnNames = new String[] { HeaderFieldSetMapper.ORDER_ID_COLUMN,
				HeaderFieldSetMapper.ORDER_DATE_COLUMN };
		return new DefaultFieldSet(tokens, columnNames);
	}

	@Override
	protected FieldSetMapper<Order> fieldSetMapper() {
		return new HeaderFieldSetMapper();
	}
}
