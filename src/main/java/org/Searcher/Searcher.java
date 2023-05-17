package org.Searcher;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.util.BytesRef;

public interface Searcher {
	void printIndexer() throws IOException;
	List<Document> search(String currentQuery, String currentField, boolean isGrouped, int currentPage) throws IOException, ParseException;
	TopGroups<BytesRef> groupingResults(Query query) throws IOException;
	TopDocs getTopDocs();
	TopGroups<BytesRef> getTopGroups();
	void close() throws IOException;
}
