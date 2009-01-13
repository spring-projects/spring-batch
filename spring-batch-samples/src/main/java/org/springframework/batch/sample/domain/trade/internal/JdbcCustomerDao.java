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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerDao;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * @author Lucas Ward
 *
 */
public class JdbcCustomerDao extends JdbcDaoSupport implements CustomerDao{

	private static final String GET_CUSTOMER_BY_NAME = "SELECT ID, NAME, CREDIT from CUSTOMER where NAME = ?";
	private static final String INSERT_CUSTOMER = "INSERT into CUSTOMER(NAME, CREDIT) values(?,?)";
	private static final String UPDATE_CUSTOMER = "UPDATE CUSTOMER set CREDIT = ? where NAME = ?";
	
	public CustomerCredit getCustomerByName(String name) {
		
		@SuppressWarnings("unchecked")
		List<CustomerCredit> customers = (List<CustomerCredit>) getJdbcTemplate().query(GET_CUSTOMER_BY_NAME, new Object[]{name}, 
				
				new RowMapper(){

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				CustomerCredit customer = new CustomerCredit();
				customer.setName(rs.getString("NAME"));
				customer.setId(rs.getInt("ID"));
				customer.setCredit(rs.getBigDecimal("CREDIT"));
				return customer;
			}
			
		});
	
		if(customers.size() == 0){
			return null;
		}
		else{
			return customers.get(0);
		}
		
	}

	public void insertCustomer(String name, BigDecimal credit) {
		
		getJdbcTemplate().update(INSERT_CUSTOMER, new Object[]{name, credit});
	}

	public void updateCustomer(String name, BigDecimal credit) {
		getJdbcTemplate().update(UPDATE_CUSTOMER, new Object[]{credit, name});
	}

}
