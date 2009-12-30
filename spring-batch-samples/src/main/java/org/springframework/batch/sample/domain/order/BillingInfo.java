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


public class BillingInfo {
	public static final String LINE_ID_BILLING_INFO = "BIN";

	private String paymentId;

	private String paymentDesc;

	public String getPaymentDesc() {
		return paymentDesc;
	}

	public void setPaymentDesc(String paymentDesc) {
		this.paymentDesc = paymentDesc;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	@Override
	public String toString() {
		return "BillingInfo [paymentDesc=" + paymentDesc + ", paymentId=" + paymentId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((paymentDesc == null) ? 0 : paymentDesc.hashCode());
		result = prime * result + ((paymentId == null) ? 0 : paymentId.hashCode());
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
		BillingInfo other = (BillingInfo) obj;
		if (paymentDesc == null) {
			if (other.paymentDesc != null)
				return false;
		}
		else if (!paymentDesc.equals(other.paymentDesc))
			return false;
		if (paymentId == null) {
			if (other.paymentId != null)
				return false;
		}
		else if (!paymentId.equals(other.paymentId))
			return false;
		return true;
	}

}
