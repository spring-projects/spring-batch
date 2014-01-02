package org.springframework.batch.core.jsr;

import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.SkipListener;

import java.util.List;

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
				throw new BatchRuntimeException(e);
			}
		}
	}

	@Override
	public void onSkipInWrite(S item, Throwable t) {
		if(skipWriteDelegate != null && t instanceof Exception) {
			try {
				skipWriteDelegate.onSkipWriteItem((List<Object>) item, (Exception) t);
			} catch (Exception e) {
				throw new BatchRuntimeException(e);
			}
		}
	}

	@Override
	public void onSkipInProcess(T item, Throwable t) {
		if(skipProcessDelegate != null && t instanceof Exception) {
			try {
				skipProcessDelegate.onSkipProcessItem(item, (Exception) t);
			} catch (Exception e) {
				throw new BatchRuntimeException(e);
			}
		}
	}
}
