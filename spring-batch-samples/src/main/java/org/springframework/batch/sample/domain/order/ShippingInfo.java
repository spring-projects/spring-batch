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


public class ShippingInfo {
	public static final String LINE_ID_SHIPPING_INFO = "SIN";

	private String shipperId;

	private String shippingTypeId;

	private String shippingInfo;

	public String getShipperId() {
		return shipperId;
	}

	public void setShipperId(String shipperId) {
		this.shipperId = shipperId;
	}

	public String getShippingInfo() {
		return shippingInfo;
	}

	public void setShippingInfo(String shippingInfo) {
		this.shippingInfo = shippingInfo;
	}

	public String getShippingTypeId() {
		return shippingTypeId;
	}

	public void setShippingTypeId(String shippingTypeId) {
		this.shippingTypeId = shippingTypeId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((shipperId == null) ? 0 : shipperId.hashCode());
		result = prime * result + ((shippingInfo == null) ? 0 : shippingInfo.hashCode());
		result = prime * result + ((shippingTypeId == null) ? 0 : shippingTypeId.hashCode());
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
		ShippingInfo other = (ShippingInfo) obj;
		if (shipperId == null) {
			if (other.shipperId != null)
				return false;
		}
		else if (!shipperId.equals(other.shipperId))
			return false;
		if (shippingInfo == null) {
			if (other.shippingInfo != null)
				return false;
		}
		else if (!shippingInfo.equals(other.shippingInfo))
			return false;
		if (shippingTypeId == null) {
			if (other.shippingTypeId != null)
				return false;
		}
		else if (!shippingTypeId.equals(other.shippingTypeId))
			return false;
		return true;
	}

}
