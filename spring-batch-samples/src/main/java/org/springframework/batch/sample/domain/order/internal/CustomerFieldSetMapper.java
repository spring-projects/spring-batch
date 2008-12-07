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
import org.springframework.batch.sample.domain.order.Customer;



public class CustomerFieldSetMapper implements FieldSetMapper<Customer> {
	
	public static final String LINE_ID_COLUMN = "LINE_ID";
	public static final String COMPANY_NAME_COLUMN = "COMPANY_NAME";
	public static final String LAST_NAME_COLUMN = "LAST_NAME";
	public static final String FIRST_NAME_COLUMN = "FIRST_NAME";
	public static final String MIDDLE_NAME_COLUMN = "MIDDLE_NAME";
	public static final String TRUE_SYMBOL = "T";
	public static final String REGISTERED_COLUMN = "REGISTERED";
	public static final String REG_ID_COLUMN = "REG_ID";
	public static final String VIP_COLUMN = "VIP";
	
    public Customer mapFieldSet(FieldSet fieldSet) {
        Customer customer = new Customer();

        if (Customer.LINE_ID_BUSINESS_CUST.equals(fieldSet.readString(LINE_ID_COLUMN))) {
            customer.setCompanyName(fieldSet.readString(COMPANY_NAME_COLUMN));
            //business customer must be always registered
            customer.setRegistered(true);
        }

        if (Customer.LINE_ID_NON_BUSINESS_CUST.equals(fieldSet.readString(LINE_ID_COLUMN))) {
            customer.setLastName(fieldSet.readString(LAST_NAME_COLUMN));
            customer.setFirstName(fieldSet.readString(FIRST_NAME_COLUMN));
            customer.setMiddleName(fieldSet.readString(MIDDLE_NAME_COLUMN));
            customer.setRegistered(TRUE_SYMBOL.equals(fieldSet.readString(REGISTERED_COLUMN)));
        }

        customer.setRegistrationId(fieldSet.readLong(REG_ID_COLUMN));
        customer.setVip(TRUE_SYMBOL.equals(fieldSet.readString(VIP_COLUMN)));

        return customer;
    }
}
