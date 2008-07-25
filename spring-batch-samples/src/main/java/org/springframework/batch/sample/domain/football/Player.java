package org.springframework.batch.sample.domain.football;

import java.io.Serializable;

public class Player implements Serializable {
	
	private String ID; 
	private String lastName; 
	private String firstName; 
	private String position; 
	private int birthYear; 
	private int debutYear;
	
	public String toString() {
		
		return "PLAYER:ID=" + ID + ",Last Name=" + lastName + 
		",First Name=" + firstName + ",Position=" + position + 
		",Birth Year=" + birthYear + ",DebutYear=" + 
		debutYear;
	}
	
	public String getID() {
		return ID;
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
	public void setID(String id) {
		ID = id;
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
