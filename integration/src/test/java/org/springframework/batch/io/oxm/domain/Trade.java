package org.springframework.batch.io.oxm.domain;

import java.math.BigDecimal;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * @author Rob Harrop
 */
public class Trade {
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
		return EqualsBuilder.reflectionEquals(this, o);
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
}
