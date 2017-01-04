package org.springframework.batch.core.test.infinite_loop_on_retry;

public class Item {
	String id;
	int nbProcessed = 0;
	int nbWritten = 0;

	@Override
	public String toString() {
		return "Item:id=" + id + ",nbProcessed=" + nbProcessed + ",nbWritten="
				+ nbWritten;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getNbProcessed() {
		return nbProcessed;
	}

	public void incNbProcessed() {
		this.nbProcessed++;
	}

	public int getNbWritten() {
		return nbWritten;
	}

	public void incNbWritten() {
		this.nbWritten++;
	}

	public Item(String id) {
		super();
		this.id = id;
	}

}
