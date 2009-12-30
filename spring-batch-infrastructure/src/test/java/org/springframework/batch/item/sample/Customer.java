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

package org.springframework.batch.item.sample;


/**
 * An XML customer.
 * 
 * This is a complex type.
 */
public class Customer {
	private String name;

	private String address;

	private int age;

	private int moo;

	private int poo;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMoo() {
		return moo;
	}

	public void setMoo(int moo) {
		this.moo = moo;
	}

	public int getPoo() {
		return poo;
	}

	public void setPoo(int poo) {
		this.poo = poo;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + age;
		result = prime * result + moo;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + poo;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Customer other = (Customer) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		}
		else if (!address.equals(other.address))
			return false;
		if (age != other.age)
			return false;
		if (moo != other.moo)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (poo != other.poo)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Customer [address=" + address + ", age=" + age + ", name=" + name + "]";
	}

}
