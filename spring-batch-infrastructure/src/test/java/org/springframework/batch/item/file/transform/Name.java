package org.springframework.batch.item.file.transform;

public class Name {
	private String first;
	private String last;
	private int born;

	public Name() {

	}

	public Name(String first, String last, int born) {
		this.first = first;
		this.last = last;
		this.born = born;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	public int getBorn() {
		return born;
	}

	public void setBorn(int born) {
		this.born = born;
	}
}
