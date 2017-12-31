/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.test.entities;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.test.Const;

/**
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

@XmlPath(path = Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document)
public class TWrongFunctionNameAnnotated extends TSuper {
	private String name;

	private boolean bValue;

	private float fValue;

	private double dValue;

	private long lValue;

	@XmlPath(path = Const.AtCcy_Amt_Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "TObject [name=" + name + ", nummer=" + getNummer() + "]";
	}

	public boolean isbValue() {
		return bValue;
	}

	public void setbValue(boolean bValue) {
		this.bValue = bValue;
	}

	public float getfValue() {
		return fValue;
	}

	public void setfValue(float fValue) {
		this.fValue = fValue;
	}

	public double getdValue() {
		return dValue;
	}

	public void setdValue(double dValue) {
		this.dValue = dValue;
	}

	public long getlValue() {
		return lValue;
	}

	public void setlValue(long lValue) {
		this.lValue = lValue;
	}

}
