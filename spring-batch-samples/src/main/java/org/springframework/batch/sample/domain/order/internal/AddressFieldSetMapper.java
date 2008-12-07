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

package org.springframework.batch.sample.domain.order.internal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.Address;



public class AddressFieldSetMapper implements FieldSetMapper<Address> {
	
	public static final String ADDRESSEE_COLUMN = "ADDRESSEE";
	public static final String ADDRESS_LINE1_COLUMN = "ADDR_LINE1";
	public static final String ADDRESS_LINE2_COLUMN = "ADDR_LINE2";
	public static final String CITY_COLUMN = "CITY";
	public static final String ZIP_CODE_COLUMN = "ZIP_CODE";
	public static final String STATE_COLUMN = "STATE";
	public static final String COUNTRY_COLUMN = "COUNTRY";
	
	
    public Address mapFieldSet(FieldSet fieldSet) {
        Address address = new Address();

        address.setAddressee(fieldSet.readString(ADDRESSEE_COLUMN));
        address.setAddrLine1(fieldSet.readString(ADDRESS_LINE1_COLUMN));
        address.setAddrLine2(fieldSet.readString(ADDRESS_LINE2_COLUMN));
        address.setCity(fieldSet.readString(CITY_COLUMN));
        address.setZipCode(fieldSet.readString(ZIP_CODE_COLUMN));
        address.setState(fieldSet.readString(STATE_COLUMN));
        address.setCountry(fieldSet.readString(COUNTRY_COLUMN));

        return address;
    }
}
