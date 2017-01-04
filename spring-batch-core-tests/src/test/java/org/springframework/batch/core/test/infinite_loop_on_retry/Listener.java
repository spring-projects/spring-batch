package org.springframework.batch.core.test.infinite_loop_on_retry;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

public class Listener implements ChunkListener {

	int cpt = 0;

	@Override
	public void beforeChunk(ChunkContext context) {
	}

	@Override
	public void afterChunk(ChunkContext context) {
		cpt++;
		if (cpt > 200) {
			throw new RuntimeException("more chunks than expected");
		}
	}

	@Override
	public void afterChunkError(ChunkContext context) {
	}

}
