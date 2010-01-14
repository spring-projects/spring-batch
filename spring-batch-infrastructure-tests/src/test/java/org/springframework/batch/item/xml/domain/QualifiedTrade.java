package org.springframework.batch.item.xml.domain;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Rob Harrop
 */
@XmlRootElement(name="trade", namespace="urn:org.springframework.batch.io.oxm.domain")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class QualifiedTrade {
	
	@XmlElement(namespace="urn:org.springframework.batch.io.oxm.domain")
	private String isin = "";

	@XmlElement(namespace="urn:org.springframework.batch.io.oxm.domain")
	private long quantity = 0;

	@XmlElement(namespace="urn:org.springframework.batch.io.oxm.domain")
	private BigDecimal price = new BigDecimal(0);

	@XmlElement(namespace="urn:org.springframework.batch.io.oxm.domain")
	private String customer = "";

	public QualifiedTrade() {
	}

	public QualifiedTrade(String isin, long quantity, BigDecimal price, String customer) {
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
		return "Trade: [isin=" + this.isin + ",quantity=" + this.quantity + ",price=" + this.price + ",customer="
				+ this.customer + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customer == null) ? 0 : customer.hashCode());
		result = prime * result + ((isin == null) ? 0 : isin.hashCode());
		result = prime * result + ((price == null) ? 0 : price.hashCode());
		result = prime * result + (int) (quantity ^ (quantity >>> 32));
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
		QualifiedTrade other = (QualifiedTrade) obj;
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
		return true;
	}

}
