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

package org.springframework.batch.sample.domain.trade;

import java.math.BigDecimal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

/**
 * {@link FieldSetMapper} for mapping to a {@link CustomerUpdate}.
 * 
 * @author Lucas Ward
 * 
 */
public class CustomerUpdateFieldSetMapper implements FieldSetMapper<CustomerUpdate> {

	public CustomerUpdate mapFieldSet(FieldSet fs) {

		if (fs == null) {
			return null;
		}

		CustomerOperation operation = CustomerOperation.fromCode(fs.readChar(0));
		String name = fs.readString(1);
		BigDecimal credit = fs.readBigDecimal(2);

		return new CustomerUpdate(operation, name, credit);

	}

}
