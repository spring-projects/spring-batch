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

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * @author Rob Harrop
 * @author Dave Syer
 */
public class Trade implements Serializable {
    private String isin = "";
    private long quantity = 0;
    private BigDecimal price = new BigDecimal(0);
    private String customer = "";
	private Long id;
	private long version = 0;

    public Trade() {
    }
    
    public Trade(String isin, long quantity, BigDecimal price, String customer){
    	this.isin = isin;
    	this.quantity = quantity;
    	this.price = price;
    	this.customer = customer;
    }

    /**
	 * @param id
	 */
	public Trade(long id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public void setCustomer(String customer) {
		this.customer = customer;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}

	public String getIsin() {
        return isin;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public String getCustomer() {
        return customer;
    }

    public String toString() {
        return "Trade: [isin=" + this.isin + ",quantity=" + this.quantity + ",price="
            + this.price + ",customer=" + this.customer + "]";
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customer == null) ? 0 : customer.hashCode());
		result = prime * result + ((isin == null) ? 0 : isin.hashCode());
		result = prime * result + ((price == null) ? 0 : price.hashCode());
		result = prime * result + (int) (quantity ^ (quantity >>> 32));
		result = prime * result + (int) (version ^ (version >>> 32));
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
		Trade other = (Trade) obj;
		if (customer == null) {
			if (other.customer != null)
				return false;
		}
		else if (!customer.equals(other.customer))
			return false;
		if (isin == null) {
			if (other.isin != null)
				return false;
		}
		else if (!isin.equals(other.isin))
			return false;
		if (price == null) {
			if (other.price != null)
				return false;
		}
		else if (!price.equals(other.price))
			return false;
		if (quantity != other.quantity)
			return false;
		if (version != other.version)
			return false;
		return true;
	}
    
 }
