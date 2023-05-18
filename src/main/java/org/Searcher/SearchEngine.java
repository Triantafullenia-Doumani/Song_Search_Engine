package org.Searcher;

import org.Constants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.grouping.TopGroups;

public class SearchEngine {

	private TopDocs topDocs;
	private TopGroups<BytesRef> topGroups;
	
	private int currentPage = 1;
	
	private String currentQuery;
	private String currentField;
	private boolean isGrouped;
	private boolean semanticSearch;
	
	Searcher searcher;
	History history;
	
	 public SearchEngine() throws IOException{	 
		 history = new History();
	}
  /**
   * Searches both standard and keyword indexers for documents matching the given query and field, and returns a list of documents.
   *
   * @param currentQuery The query string.
   * @param currentField The field to search in.
   * @param numResults The maximum number of results to return.
   * @return A list of documents matching the query.
   */
	 public List<Document> search(String currentQuery, String currentField,boolean isGrouped, boolean semanticSearch) throws IOException, ParseException {
		  List<Document> results = new ArrayList<Document>();
		  
		  this.currentQuery = currentQuery;
		  this.currentField = currentField;
		  this.isGrouped = isGrouped;
		  this.semanticSearch = semanticSearch;
		  
		  history.addHistory(currentQuery);
		  
	      searcher = SearcherFactory.selectSearcher(currentField,semanticSearch);

		  results = searcher.search(currentQuery, currentField, isGrouped, currentPage);
		  topDocs = searcher.getTopDocs();
		  topGroups = searcher.getTopGroups();
		  
		  if(results.isEmpty()) {
			  System.out.println("No results found for Query '" + currentQuery + "' in " + (currentField.equals("As Keyword") ? "any field" : "'"+currentField+"'") );
		  }
	      return results;
	  }
  
	//@return True if there is a next page, false otherwise.
	public boolean hasNextPage() {
	 int numHits;
	 int maxPage;
	 if (isGrouped) {
	     numHits = topGroups.groups.length;
	     maxPage = numHits;
	 } else {
	     numHits = (int) topDocs.totalHits.value;
	     maxPage = numHits / LuceneConstants.PAGE_SIZE + (numHits % LuceneConstants.PAGE_SIZE == 0 ? 0 : 1);
	 }
	 return currentPage < maxPage;
	}


	// @return True if there is a previous page, false otherwise.
	public boolean hasPreviousPage() {
		return currentPage > 1;
	}

	// @return The next page of search results.
	public List<Document> getNextPage() throws IOException, ParseException {
		if (hasNextPage()) {
			currentPage++;
			return search(currentQuery, currentField,isGrouped, semanticSearch);
		}
		return Collections.emptyList();
  }

	// @return The previous page of search results.
	public List<Document> getPreviousPage() throws IOException, ParseException {
		if (hasPreviousPage()) {
			currentPage--;
			return search(currentQuery, currentField,isGrouped,semanticSearch);
		}
		return Collections.emptyList();
	}
	
	public List<String> searchHistory(String text) {
		return history.getHistory(text);
	}
	public void close() throws IOException {
		searcher.close();
	}

}