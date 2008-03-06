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

package org.springframework.batch.io.file.mapping;

import java.math.BigDecimal;
import java.util.Date;

public class TestObject {
	String varString;

	boolean varBoolean;

	char varChar;

	byte varByte;

	short varShort;

	int varInt;

	long varLong;

	float varFloat;

	double varDouble;

	BigDecimal varBigDecimal;

	Date varDate;

	public Date getVarDate() {
		return (Date)varDate.clone();
	}

	public void setVarDate(Date varDate) {
		this.varDate = varDate == null ? null : (Date)varDate.clone();
	}

	public TestObject() {
	}

	public BigDecimal getVarBigDecimal() {
		return varBigDecimal;
	}

	public void setVarBigDecimal(BigDecimal varBigDecimal) {
		this.varBigDecimal = varBigDecimal;
	}

	public boolean isVarBoolean() {
		return varBoolean;
	}

	public void setVarBoolean(boolean varBoolean) {
		this.varBoolean = varBoolean;
	}

	public byte getVarByte() {
		return varByte;
	}

	public void setVarByte(byte varByte) {
		this.varByte = varByte;
	}

	public char getVarChar() {
		return varChar;
	}

	public void setVarChar(char varChar) {
		this.varChar = varChar;
	}

	public double getVarDouble() {
		return varDouble;
	}

	public void setVarDouble(double varDouble) {
		this.varDouble = varDouble;
	}

	public float getVarFloat() {
		return varFloat;
	}

	public void setVarFloat(float varFloat) {
		this.varFloat = varFloat;
	}

	public long getVarLong() {
		return varLong;
	}

	public void setVarLong(long varLong) {
		this.varLong = varLong;
	}

	public short getVarShort() {
		return varShort;
	}

	public void setVarShort(short varShort) {
		this.varShort = varShort;
	}

	public String getVarString() {
		return varString;
	}

	public void setVarString(String varString) {
		this.varString = varString;
	}

	public int getVarInt() {
		return varInt;
	}

	public void setVarInt(int varInt) {
		this.varInt = varInt;
	}
}
