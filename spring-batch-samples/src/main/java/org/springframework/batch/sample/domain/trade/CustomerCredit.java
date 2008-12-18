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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "CUSTOMER")
public class CustomerCredit {

	@Id
	private int id;

	private String name;

	private BigDecimal credit;

	public CustomerCredit() {
	}

	public CustomerCredit(int id, String name, BigDecimal credit) {
		this.id = id;
		this.name = name;
		this.credit = credit;
	}

	public String toString() {
		return "CustomerCredit [id=" + id + ",name=" + name + ", credit=" + credit + "]";
	}

	public BigDecimal getCredit() {
		return credit;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setCredit(BigDecimal credit) {
		this.credit = credit;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CustomerCredit increaseCreditBy(BigDecimal sum) {
		CustomerCredit newCredit = new CustomerCredit();
		newCredit.credit = this.credit.add(sum);
		newCredit.name = this.name;
		newCredit.id = this.id;
		return newCredit;
	}

	public boolean equals(Object o) {
		return (o instanceof CustomerCredit) && ((CustomerCredit) o).id == id;
	}

	public int hashCode() {
		return id;
	}

}
