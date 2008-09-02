package org.springframework.batch.integration.chunk;

public interface ChunkHandler<T> {

	ChunkResponse handleChunk(ChunkRequest<T> chunk);

}