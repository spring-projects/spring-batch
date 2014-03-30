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
import org.springframework.batch.sample.domain.order.internal.mapper.BillingFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class BillingFieldSetMapperTests extends AbstractFieldSetMapperTests {
	private static final String PAYMENT_ID = "777";
	private static final String PAYMENT_DESC = "My last penny";

	@Override
	protected Object expectedDomainObject() {
		BillingInfo bInfo = new BillingInfo();
		bInfo.setPaymentDesc(PAYMENT_DESC);
		bInfo.setPaymentId(PAYMENT_ID);
		return bInfo;
	}

	@Override
	protected FieldSet fieldSet() {
		String[] tokens = new String[] { PAYMENT_ID, PAYMENT_DESC };
		String[] columnNames = new String[] { BillingFieldSetMapper.PAYMENT_TYPE_ID_COLUMN,
				BillingFieldSetMapper.PAYMENT_DESC_COLUMN };
		return new DefaultFieldSet(tokens, columnNames);
	}

	@Override
	protected FieldSetMapper<BillingInfo> fieldSetMapper() {
		return new BillingFieldSetMapper();
	}
}
