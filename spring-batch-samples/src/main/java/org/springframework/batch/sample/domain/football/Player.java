package org.springframework.batch.sample.domain.football;

import java.io.Serializable;

public class Player implements Serializable {
	
	private String id; 
	private String lastName; 
	private String firstName; 
	private String position; 
	private int birthYear; 
	private int debutYear;
	
	public String toString() {
		
		return "PLAYER:id=" + id + ",Last Name=" + lastName +
		",First Name=" + firstName + ",Position=" + position + 
		",Birth Year=" + birthYear + ",DebutYear=" + 
		debutYear;
	}
	
	public String getId() {
		return id;
	}
	public String getLastName() {
		return lastName;
	}
	public String getFirstName() {
		return firstName;
	}
	public String getPosition() {
		return position;
	}
	public int getBirthYear() {
		return birthYear;
	}
	public int getDebutYear() {
		return debutYear;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public void setBirthYear(int birthYear) {
		this.birthYear = birthYear;
	}
	public void setDebutYear(int debutYear) {
		this.debutYear = debutYear;
	}
	
	
	
	
	

}
