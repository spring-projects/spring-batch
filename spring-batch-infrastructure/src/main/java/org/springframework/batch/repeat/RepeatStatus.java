package org.springframework.batch.repeat;

public enum RepeatStatus {

	UNKNOWN(true), CONTINUABLE(true), FINISHED(false);

	private final boolean continuable;

	private RepeatStatus(boolean continuable) {
		this.continuable = continuable;
	}

	public static RepeatStatus continueIf(boolean continuable) {
		return continuable ? CONTINUABLE : FINISHED;
	}

	public boolean isContinuable() {
		return this == CONTINUABLE;
	}

	public RepeatStatus and(boolean value) {
		return value && continuable ? CONTINUABLE : FINISHED;
	}

}
