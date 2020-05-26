/*
 * Copyright 2006-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample.domain.trade;

import static org.springframework.batch.sample.domain.trade.CustomerOperation.*;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

/**
 * @author Lucas Ward
 *
 */
public class CustomerUpdateProcessor implements ItemProcessor<CustomerUpdate, CustomerUpdate>{

	private CustomerDao customerDao;
	private InvalidCustomerLogger invalidCustomerLogger;
	
	@Nullable
	@Override
	public CustomerUpdate process(CustomerUpdate item) throws Exception {
		
		if(item.getOperation() == DELETE){
			//delete is not supported
			invalidCustomerLogger.log(item);
			return null;
		}
		
		CustomerCredit customerCredit = customerDao.getCustomerByName(item.getCustomerName());
		
		if(item.getOperation() == ADD && customerCredit == null){
			return item;
		}
		else if(item.getOperation() == ADD && customerCredit != null){
			//veto processing
			invalidCustomerLogger.log(item);
			return null;
		}
		
		if(item.getOperation() == UPDATE && customerCredit != null){
			return item;
		}
		else if(item.getOperation() == UPDATE && customerCredit == null){
			//veto processing
			invalidCustomerLogger.log(item);
			return null;
		}
		
		//if an item makes it through all these checks it can be assumed to be bad, logged, and skipped
		invalidCustomerLogger.log(item);
		return null;
	}
	
	public void setCustomerDao(CustomerDao customerDao) {
		this.customerDao = customerDao;
	}
	
	public void setInvalidCustomerLogger(
			InvalidCustomerLogger invalidCustomerLogger) {
		this.invalidCustomerLogger = invalidCustomerLogger;
	}

}
