package org.springframework.batch.item.sample;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;

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
	
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	public void fail() throws Exception {
		throw new Exception(FAILURE_MESSAGE);
	}
	
	public void failUgly() throws Throwable {
		throw new Throwable(UGLY_FAILURE_MESSAGE);
	}

}
