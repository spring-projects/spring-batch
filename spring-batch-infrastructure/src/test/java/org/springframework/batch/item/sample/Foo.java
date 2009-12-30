package org.springframework.batch.item.sample;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Simple domain object for testing purposes.
 */
@Entity
@Table(name = "T_FOOS")
public class Foo {
	
	public static final String FAILURE_MESSAGE = "Foo Failure!";
	
	public static final String UGLY_FAILURE_MESSAGE = "Ugly Foo Failure!";
	
	@Id
	private int id;
	private String name;
	private int value;
	
	public Foo(){}
	
	public Foo(int id, String name, int value) {
		this.id = id;
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
		this.value = value;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public String toString() {
		return "Foo[id=" +id +",name=" + name + ",value=" + value + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + value;
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
		Foo other = (Foo) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (value != other.value)
			return false;
		return true;
	}

	public void fail() throws Exception {
		throw new Exception(FAILURE_MESSAGE);
	}
	
	public void failUgly() throws Throwable {
		throw new Throwable(UGLY_FAILURE_MESSAGE);
	}

}
