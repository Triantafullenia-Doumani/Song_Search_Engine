package org.Searcher;

import org.Constants.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Searcher {
	
	IndexSearcher standardIndexSearcher;
	IndexSearcher keywordIndexSearcher;
	Query query;

	Directory standardIndexDirectory;
	Directory keywordIndexDirectory;
	IndexSearcher indexSearcher;
	TopDocs topDocs;
	
	int currentPage = 1;
	int numResultsPerPage = 20;
	   
	String currentQuery;
	String currentField;
	   
   /*
    * 
    * @param currentField: The name of the field in the index that the searcher should search for matching documents(Artist, Title, Album, Date, Lyrics, Year).
    * @param currentQuery: The user's search query string
    */
  public Searcher() throws IOException{

	// Open keyword index directory
	this.keywordIndexDirectory = FSDirectory.open(Paths.get(LuceneConstants.KEYWORD_INDEX_FILE_PATH));
	IndexReader keywordIndexReader = DirectoryReader.open(this.keywordIndexDirectory);
	this.keywordIndexSearcher = new IndexSearcher(keywordIndexReader);
	if (keywordIndexReader == null) {
	    System.out.println("Could not read keyword Indexer");
	    System.exit(1);
	} 
    // Open standard index directory
	this.standardIndexDirectory = FSDirectory.open(Paths.get(LuceneConstants.STANDARD_INDEX_FILE_PATH));
	IndexReader standardIndexReader = DirectoryReader.open(this.standardIndexDirectory);
	this.standardIndexSearcher = new IndexSearcher(standardIndexReader);
	if (standardIndexReader == null) {
	    System.out.println("Could not read standard Indexer");
	    System.exit(1);
	}
   }

  /**
   * Searches both standard and keyword indexers for documents matching the given query and field, and returns a list of documents.
   *
   * @param currentQuery The query string.
   * @param currentField The field to search in.
   * @param numResults The maximum number of results to return.
   * @return A list of documents matching the query.
   */
  public List<Document> search(String currentQuery, String currentField) throws IOException, ParseException {
      List<Document> results = new ArrayList<>();
      int numHits = LuceneConstants.PAGE_SIZE * currentPage;
      this.currentField = currentField;
      this.currentQuery = currentQuery;
      // Preprocess the query text
      currentQuery = preprocessText(currentQuery);
      currentField = "Entire document";

      BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
      // Get the query for the standard index
      Query standardQuery = getQueryForSingleField(currentField, currentQuery);
      booleanQueryBuilder.add(new BooleanClause(standardQuery, BooleanClause.Occur.SHOULD));

      // Get the query for the keyword index
      Query keywordQuery = getQueryForSingleField(currentField, currentQuery);
      booleanQueryBuilder.add(new BooleanClause(keywordQuery, BooleanClause.Occur.SHOULD));

      this.query = booleanQueryBuilder.build();

      // Get the top hits from the search and iterate through them
      TopDocs topDocs = indexSearcher.search(query, numHits);
      ScoreDoc[] hits = topDocs.scoreDocs;
      // Calculate the start and end indices for the current page
      int start = LuceneConstants.PAGE_SIZE * (currentPage - 1);
      int end = Math.min(numHits, start + LuceneConstants.PAGE_SIZE);
      StoredFields storedFields = indexSearcher.storedFields();
      if (hits == null) {
          System.out.println("No hits");
      }
      if (hits.length > 0) {
          for (int i = start; i < end; i++) {
              // Get the document object for the current hit
              Document hitDoc = storedFields.document(hits[i].doc);
              // Add the document object to the list of results
              results.add(hitDoc);
          }
      }
      if (results.isEmpty()) {
          System.out.println("No results found");
      }
      //printIndexer();
      return results;
  }



	public Query getQueryForAllFields(String currentQuery) throws ParseException {
		String[] fields = {"Artist", "Title", "Album", "Date", "Lyrics", "Year"}; 
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fields, new StandardAnalyzer());
		this.query = queryParser.parse(currentQuery);
		return query;
	}
	public Query getQueryForSingleField(String currentField, String currentQuery) throws ParseException {
		QueryParser queryParser = new QueryParser(currentField, new StandardAnalyzer());
		this.query = queryParser.parse(currentQuery);
		return query;
	}
  
	// @return True if there is a next page, false otherwise.
	public boolean hasNextPage() {
		int numHits = (int) topDocs.totalHits.value;
		int maxPage = numHits / numResultsPerPage + (numHits % numResultsPerPage == 0 ? 0 : 1);
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
			return search(currentQuery, currentField);
		}
		return Collections.emptyList();
  }

	// @return The previous page of search results.
	public List<Document> getPreviousPage() throws IOException, ParseException {
		if (hasPreviousPage()) {
			currentPage--;
			return search(currentQuery, currentField);
		}
		return Collections.emptyList();
	}

	// Print method, just for debugging to verify that Indexer has been created correctly
	public void printIndexer(IndexReader indexReader) throws IOException {
		// Print number of documents in the index
		System.out.println("Number of documents in the index: " + indexReader.numDocs());
		// Print the contents of each document
		for (int docId = 0; docId < indexReader.maxDoc(); docId++) {
		    Document document = indexReader.document(docId);
		    System.out.println("Document ID: " + docId);
		    for (IndexableField field : document.getFields()) {
		        System.out.println("  " + field.name() + " = " + field.stringValue());
		    }
		}
   }
	public void close() throws IOException {
		this.keywordIndexDirectory.close();
		this.standardIndexDirectory.close();
	}
   
	private static String preprocessText(String text) {
		// Remove punctuation
	   text = text.replaceAll("[^a-zA-Z0-9 ]", "");
	   // Lower case the text
	   text = text.toLowerCase();
	   // Trim leading and trailing whitespace
       text = text.trim();
       return text;
	}
}