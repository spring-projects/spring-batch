package org.springframework.batch.core.jsr;

import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;

import org.springframework.batch.core.SkipListener;

public class SkipListenerAdapter<T, S> implements SkipListener<T, S> {

	private final SkipReadListener skipReadDelegate;
	private final SkipProcessListener skipProcessDelegate;
	private final SkipWriteListener skipWriteDelegate;

	public SkipListenerAdapter(SkipReadListener skipReadDelgate, SkipProcessListener skipProcessDelegate, SkipWriteListener skipWriteDelegate) {
		this.skipReadDelegate = skipReadDelgate;
		this.skipProcessDelegate = skipProcessDelegate;
		this.skipWriteDelegate = skipWriteDelegate;
	}

	@Override
	public void onSkipInRead(Throwable t) {
		if(skipReadDelegate != null && t instanceof Exception) {
			try {
				skipReadDelegate.onSkipReadItem((Exception) t);
			} catch (Exception e) {
				//TODO: Do something here
			}
		}
	}

	@Override
	public void onSkipInWrite(S item, Throwable t) {
		//TODO: Awating information on the JSR's method
	}

	@Override
	public void onSkipInProcess(T item, Throwable t) {
		if(skipProcessDelegate != null && t instanceof Exception) {
			try {
				skipProcessDelegate.onSkipProcessItem(item, (Exception) t);
			} catch (Exception e) {
				//TODO: Do something here
			}
		}
	}
}
