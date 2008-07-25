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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.trade.CustomerDebit;
import org.springframework.jdbc.core.RowMapper;


public class CustomerDebitRowMapper implements RowMapper {
	
	public static final String CUSTOMER_COLUMN = "customer";
	public static final String PRICE_COLUMN = "price";
	
    public Object mapRow(ResultSet rs, int ignoredRowNumber)
        throws SQLException {
        CustomerDebit customerDebit = new CustomerDebit();

        customerDebit.setName(rs.getString(CUSTOMER_COLUMN));
        customerDebit.setDebit(rs.getBigDecimal(PRICE_COLUMN));

        return customerDebit;
    }
}
