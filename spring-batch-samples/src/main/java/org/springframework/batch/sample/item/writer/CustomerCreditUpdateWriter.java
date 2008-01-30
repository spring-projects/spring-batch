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

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.writer.AbstractItemWriter;
import org.springframework.batch.sample.dao.CustomerCreditDao;
import org.springframework.batch.sample.domain.CustomerCredit;



public class CustomerCreditUpdateWriter extends AbstractItemWriter implements ItemWriter {
    private double creditFilter = 800;
    private CustomerCreditDao dao;

    public void write(Object data) throws Exception {
        CustomerCredit customerCredit = (CustomerCredit) data;

        if (customerCredit.getCredit().doubleValue() > creditFilter) {
            dao.writeCredit(customerCredit);
        }
    }

    public void setCreditFilter(double creditFilter) {
        this.creditFilter = creditFilter;
    }

    public void setWriter(CustomerCreditDao writer) {
        this.dao = writer;
    }

	public void close() throws Exception {
	}

}
