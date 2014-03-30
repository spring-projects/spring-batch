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
import org.springframework.batch.sample.domain.order.internal.mapper.AddressFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class AddressFieldSetMapperTests extends AbstractFieldSetMapperTests {
	private static final String ADDRESSEE = "Jan Hrach";
	private static final String ADDRESS_LINE_1 = "Plynarenska 7c";
	private static final String ADDRESS_LINE_2 = "";
	private static final String CITY = "Bratislava";
	private static final String STATE = "";
	private static final String COUNTRY = "Slovakia";
	private static final String ZIP_CODE = "80000";

	@Override
	protected Object expectedDomainObject() {
		Address address = new Address();
		address.setAddressee(ADDRESSEE);
		address.setAddrLine1(ADDRESS_LINE_1);
		address.setAddrLine2(ADDRESS_LINE_2);
		address.setCity(CITY);
		address.setState(STATE);
		address.setCountry(COUNTRY);
		address.setZipCode(ZIP_CODE);
		return address;
	}

	@Override
	protected FieldSet fieldSet() {
		String[] tokens = new String[] { ADDRESSEE, ADDRESS_LINE_1, ADDRESS_LINE_2, CITY, STATE, COUNTRY, ZIP_CODE };
		String[] columnNames = new String[] { AddressFieldSetMapper.ADDRESSEE_COLUMN,
				AddressFieldSetMapper.ADDRESS_LINE1_COLUMN, AddressFieldSetMapper.ADDRESS_LINE2_COLUMN,
				AddressFieldSetMapper.CITY_COLUMN, AddressFieldSetMapper.STATE_COLUMN,
				AddressFieldSetMapper.COUNTRY_COLUMN, AddressFieldSetMapper.ZIP_CODE_COLUMN };

		return new DefaultFieldSet(tokens, columnNames);
	}

	@Override
	protected FieldSetMapper<Address> fieldSetMapper() {
		return new AddressFieldSetMapper();
	}
}
