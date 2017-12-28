package org.springframework.batch.item.xmlpathreader.test.entities;

public class TSuper {

	private String verwendungszweck;

	private int nummer;

	public String getVerwendungszweck() {
		return verwendungszweck;
	}

	public void setVerwendungszweck(String verwendungszweck) {
		this.verwendungszweck = verwendungszweck;
	}

	@Override
	public String toString() {
		return "TSuper [verwendungszweck=" + verwendungszweck + "]";
	}

	public int getNummer() {
		return nummer;
	}

	public void setNummer(int nummer) {
		this.nummer = nummer;
	}

}
