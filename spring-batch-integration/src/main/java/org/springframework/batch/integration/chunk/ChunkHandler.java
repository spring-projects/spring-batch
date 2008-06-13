package org.springframework.batch.integration.chunk;


public interface ChunkHandler {

	ChunkResponse handleChunk(ChunkRequest chunk);

}