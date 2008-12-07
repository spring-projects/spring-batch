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

package org.springframework.batch.sample.domain.trade.internal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.trade.CustomerCredit;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class CustomerCreditFieldSetMapper implements FieldSetMapper<CustomerCredit> {

	public static final int ID_COLUMN = 0;
	public static final int NAME_COLUMN = 1;
	public static final int CREDIT_COLUMN = 2;

	public CustomerCredit mapFieldSet(FieldSet fieldSet) {

		CustomerCredit trade = new CustomerCredit();
		trade.setId(fieldSet.readInt(ID_COLUMN));
		trade.setName(fieldSet.readString(NAME_COLUMN));
		trade.setCredit(fieldSet.readBigDecimal(CREDIT_COLUMN));

		return trade;
	}
}
