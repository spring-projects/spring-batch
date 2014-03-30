/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.batch.sample.domain.order;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.mapper.ShippingFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class ShippingFieldSetMapperTests extends AbstractFieldSetMapperTests {
	private static final String SHIPPER_ID = "1";
	private static final String SHIPPING_INFO = "most interesting and informative shipping info ever";
	private static final String SHIPPING_TYPE_ID = "X";

	@Override
	protected Object expectedDomainObject() {
		ShippingInfo info = new ShippingInfo();
		info.setShipperId(SHIPPER_ID);
		info.setShippingInfo(SHIPPING_INFO);
		info.setShippingTypeId(SHIPPING_TYPE_ID);
		return info;
	}

	@Override
	protected FieldSet fieldSet() {
		String[] tokens = new String[] { SHIPPER_ID, SHIPPING_INFO, SHIPPING_TYPE_ID };
		String[] columnNames = new String[] { ShippingFieldSetMapper.SHIPPER_ID_COLUMN,
				ShippingFieldSetMapper.ADDITIONAL_SHIPPING_INFO_COLUMN, ShippingFieldSetMapper.SHIPPING_TYPE_ID_COLUMN };
		return new DefaultFieldSet(tokens, columnNames);
	}

	@Override
	protected FieldSetMapper<ShippingInfo> fieldSetMapper() {
		return new ShippingFieldSetMapper();
	}
}
