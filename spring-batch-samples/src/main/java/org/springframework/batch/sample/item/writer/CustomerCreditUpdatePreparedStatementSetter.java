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
package org.springframework.batch.sample.item.writer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.batch.io.support.ItemPreparedStatementSetter;
import org.springframework.batch.sample.domain.CustomerCredit;

/**
 * @author Dave Syer
 *
 */
public class CustomerCreditUpdatePreparedStatementSetter implements ItemPreparedStatementSetter {

	/* (non-Javadoc)
	 * @see org.springframework.batch.io.support.ItemPreparedStatementSetter#setValues(java.lang.Object, java.sql.PreparedStatement)
	 */
	public void setValues(Object item, PreparedStatement ps) throws SQLException {
		CustomerCredit customerCredit = (CustomerCredit) item;
		ps.setBigDecimal(1, customerCredit.getCredit());
		ps.setLong(2, customerCredit.getId());
	}

}
