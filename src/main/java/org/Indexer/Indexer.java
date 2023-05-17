package org.Indexer;

import java.io.IOException;

public interface Indexer {
	void close() throws IOException;
	void createIndexer() throws IOException;
}
