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

package org.springframework.batch.sample.domain.football;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Domain object representing the summary of a given Player's 
 * year.
 * 
 * @author Lucas Ward
 *
 */
public class PlayerSummary {

	private String id;
	private int year;
	private int completes;
	private int attempts;
	private int passingYards;
	private int passingTd;
	private int interceptions;
	private int rushes;
	private int rushYards;
	private int receptions;
	private int receptionYards;
	private int totalTd;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public int getCompletes() {
		return completes;
	}
	public void setCompletes(int completes) {
		this.completes = completes;
	}
	public int getAttempts() {
		return attempts;
	}
	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}
	public int getPassingYards() {
		return passingYards;
	}
	public void setPassingYards(int passingYards) {
		this.passingYards = passingYards;
	}
	public int getPassingTd() {
		return passingTd;
	}
	public void setPassingTd(int passingTd) {
		this.passingTd = passingTd;
	}
	public int getInterceptions() {
		return interceptions;
	}
	public void setInterceptions(int interceptions) {
		this.interceptions = interceptions;
	}
	public int getRushes() {
		return rushes;
	}
	public void setRushes(int rushes) {
		this.rushes = rushes;
	}
	public int getRushYards() {
		return rushYards;
	}
	public void setRushYards(int rushYards) {
		this.rushYards = rushYards;
	}
	public int getReceptions() {
		return receptions;
	}
	public void setReceptions(int receptions) {
		this.receptions = receptions;
	}
	public int getReceptionYards() {
		return receptionYards;
	}
	public void setReceptionYards(int receptionYards) {
		this.receptionYards = receptionYards;
	}
	public int getTotalTd() {
		return totalTd;
	}
	public void setTotalTd(int totalTd) {
		this.totalTd = totalTd;
	}
	
	
	public String toString() {
		return "Player Summary: ID=" + id + " Year=" + year + "[" + completes + ";" + attempts + ";" + passingYards +
				";" + passingTd + ";" + interceptions + ";" + rushes + ";" + rushYards + ";" + receptions +
				";" + receptionYards + ";" + totalTd;
	}
	
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
}
