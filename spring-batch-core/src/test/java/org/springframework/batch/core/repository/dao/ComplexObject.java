package org.springframework.batch.core.repository.dao;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author trisberg
 */
public class ComplexObject {
	private String name;
	private BigDecimal number;
	private ComplexObject obj;
	private Map map;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getNumber() {
		return number;
	}

	public void setNumber(BigDecimal number) {
		this.number = number;
	}

	public ComplexObject getObj() {
		return obj;
	}

	public void setObj(ComplexObject obj) {
		this.obj = obj;
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}


	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ComplexObject that = (ComplexObject) o;

		if (map != null ? !map.equals(that.map) : that.map != null) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (number != null ? !number.equals(that.number) : that.number != null) return false;
		if (obj != null ? !obj.equals(that.obj) : that.obj != null) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (name != null ? name.hashCode() : 0);
		result = 31 * result + (number != null ? number.hashCode() : 0);
		result = 31 * result + (obj != null ? obj.hashCode() : 0);
		result = 31 * result + (map != null ? map.hashCode() : 0);
		return result;
	}
}
