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

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Game implements Serializable {
	
	private String id;
	private int year;
	private String team;
	private int week;
	private String opponent;
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
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @return the year
	 */
	public int getYear() {
		return year;
	}
	/**
	 * @return the team
	 */
	public String getTeam() {
		return team;
	}
	/**
	 * @return the week
	 */
	public int getWeek() {
		return week;
	}
	/**
	 * @return the opponent
	 */
	public String getOpponent() {
		return opponent;
	}
	/**
	 * @return the completes
	 */
	public int getCompletes() {
		return completes;
	}
	/**
	 * @return the attempts
	 */
	public int getAttempts() {
		return attempts;
	}
	/**
	 * @return the passingYards
	 */
	public int getPassingYards() {
		return passingYards;
	}
	/**
	 * @return the passingTd
	 */
	public int getPassingTd() {
		return passingTd;
	}
	/**
	 * @return the interceptions
	 */
	public int getInterceptions() {
		return interceptions;
	}
	/**
	 * @return the rushes
	 */
	public int getRushes() {
		return rushes;
	}
	/**
	 * @return the rushYards
	 */
	public int getRushYards() {
		return rushYards;
	}
	/**
	 * @return the receptions
	 */
	public int getReceptions() {
		return receptions;
	}
	/**
	 * @return the receptionYards
	 */
	public int getReceptionYards() {
		return receptionYards;
	}
	/**
	 * @return the totalTd
	 */
	public int getTotalTd() {
		return totalTd;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @param year the year to set
	 */
	public void setYear(int year) {
		this.year = year;
	}
	/**
	 * @param team the team to set
	 */
	public void setTeam(String team) {
		this.team = team;
	}
	/**
	 * @param week the week to set
	 */
	public void setWeek(int week) {
		this.week = week;
	}
	/**
	 * @param opponent the opponent to set
	 */
	public void setOpponent(String opponent) {
		this.opponent = opponent;
	}
	/**
	 * @param completes the completes to set
	 */
	public void setCompletes(int completes) {
		this.completes = completes;
	}
	/**
	 * @param attempts the attempts to set
	 */
	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}
	/**
	 * @param passingYards the passingYards to set
	 */
	public void setPassingYards(int passingYards) {
		this.passingYards = passingYards;
	}
	/**
	 * @param passingTd the passingTd to set
	 */
	public void setPassingTd(int passingTd) {
		this.passingTd = passingTd;
	}
	/**
	 * @param interceptions the interceptions to set
	 */
	public void setInterceptions(int interceptions) {
		this.interceptions = interceptions;
	}
	/**
	 * @param rushes the rushes to set
	 */
	public void setRushes(int rushes) {
		this.rushes = rushes;
	}
	/**
	 * @param rushYards the rushYards to set
	 */
	public void setRushYards(int rushYards) {
		this.rushYards = rushYards;
	}
	/**
	 * @param receptions the receptions to set
	 */
	public void setReceptions(int receptions) {
		this.receptions = receptions;
	}
	/**
	 * @param receptionYards the receptionYards to set
	 */
	public void setReceptionYards(int receptionYards) {
		this.receptionYards = receptionYards;
	}
	/**
	 * @param totalTd the totalTd to set
	 */
	public void setTotalTd(int totalTd) {
		this.totalTd = totalTd;
	}
	
	
	public String toString() {

		return "Game: ID=" + id + " " + team + " vs. " + opponent + 
		" - " + year;
	}
	
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	
}
