package org.springframework.batch.item.xmlpathreader.test.entities;

public class TObject extends TSuper {
	private String name;

	private boolean bValue;

	private float fValue;

	private double dValue;

	private long lValue;

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
