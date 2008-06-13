package org.springframework.integration.batch.chunk;


public interface ChunkHandler {

	ChunkResponse handleChunk(ChunkRequest chunk);

}