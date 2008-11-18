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

import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * @author Rob Harrop
 * @author Dave Syer
 */
public class Trade implements Serializable {
    private String isin = "";
    private long quantity = 0;
    private BigDecimal price = new BigDecimal(0);
    private String customer = "";

    public Trade() {
    }
    
    public Trade(String isin, long quantity, BigDecimal price, String customer){
    	this.isin = isin;
    	this.quantity = quantity;
    	this.price = price;
    	this.customer = customer;
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
    
    public boolean equals(Object o) {
    	if(!(o instanceof Trade)){
    		return false;
    	}
    	
    	if(o == this){
    		return true;
    		
    	}
    	
    	Trade t = (Trade)o;
		return isin.equals(t.getIsin()) && quantity == t.getQuantity() && 
			price.equals(t.getPrice()) && customer.equals(t.getCustomer())  ;
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
}
