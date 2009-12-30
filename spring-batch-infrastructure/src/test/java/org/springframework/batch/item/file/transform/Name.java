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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + born;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((last == null) ? 0 : last.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Name other = (Name) obj;
		if (born != other.born)
			return false;
		if (first == null) {
			if (other.first != null)
				return false;
		}
		else if (!first.equals(other.first))
			return false;
		if (last == null) {
			if (other.last != null)
				return false;
		}
		else if (!last.equals(other.last))
			return false;
		return true;
	}

	
}
