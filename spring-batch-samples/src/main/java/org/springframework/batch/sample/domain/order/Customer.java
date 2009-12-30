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

package org.springframework.batch.sample.domain.order;


public class Customer {
	public static final String LINE_ID_BUSINESS_CUST = "BCU";

	public static final String LINE_ID_NON_BUSINESS_CUST = "NCU";

	private boolean businessCustomer;

	private boolean registered;

	private long registrationId;

	// non-business customer
	private String firstName;

	private String lastName;

	private String middleName;

	private boolean vip;

	// business customer
	private String companyName;

	public boolean isBusinessCustomer() {
		return businessCustomer;
	}

	public void setBusinessCustomer(boolean bussinessCustomer) {
		this.businessCustomer = bussinessCustomer;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public long getRegistrationId() {
		return registrationId;
	}

	public void setRegistrationId(long registrationId) {
		this.registrationId = registrationId;
	}

	public boolean isVip() {
		return vip;
	}

	public void setVip(boolean vip) {
		this.vip = vip;
	}

	@Override
	public String toString() {
		return "Customer [companyName=" + companyName + ", firstName=" + firstName + ", lastName=" + lastName + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (businessCustomer ? 1231 : 1237);
		result = prime * result + ((companyName == null) ? 0 : companyName.hashCode());
		result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
		result = prime * result + ((middleName == null) ? 0 : middleName.hashCode());
		result = prime * result + (registered ? 1231 : 1237);
		result = prime * result + (int) (registrationId ^ (registrationId >>> 32));
		result = prime * result + (vip ? 1231 : 1237);
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
		if (businessCustomer != other.businessCustomer)
			return false;
		if (companyName == null) {
			if (other.companyName != null)
				return false;
		}
		else if (!companyName.equals(other.companyName))
			return false;
		if (firstName == null) {
			if (other.firstName != null)
				return false;
		}
		else if (!firstName.equals(other.firstName))
			return false;
		if (lastName == null) {
			if (other.lastName != null)
				return false;
		}
		else if (!lastName.equals(other.lastName))
			return false;
		if (middleName == null) {
			if (other.middleName != null)
				return false;
		}
		else if (!middleName.equals(other.middleName))
			return false;
		if (registered != other.registered)
			return false;
		if (registrationId != other.registrationId)
			return false;
		if (vip != other.vip)
			return false;
		return true;
	}

}
