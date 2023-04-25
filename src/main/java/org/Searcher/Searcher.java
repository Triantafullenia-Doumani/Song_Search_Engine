package org.Searcher;

import org.Constants.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Searcher {
	
   Directory indexDirectory;
   IndexSearcher indexSearcher;
   IndexReader indexReader;
   Query query;
   QueryParser queryParser;
   TopDocs topDocs;
   Analyzer queryAnalyzer;
   
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
	   
       // Open index directory
	   this.indexDirectory =  FSDirectory.open(Paths.get(LuceneConstants.INDEX_FILE_PATH));

	   // Create IndexSearcher
	   this.indexReader = DirectoryReader.open(indexDirectory);
	   this.indexSearcher = new IndexSearcher(indexReader);
	   if (indexReader == null) {
		   System.out.println("Could not read Indexer");
		   System.exit(1);
	   }
   }

  /**
   * Searches the index for documents matching the given query and field, and returns a list of documents.
   *
   * @param currentQuery The query string.
   * @param currentField The field to search in.
   * @param numResults The maximum number of results to return.
   * @return A list of documents matching the query.
   */
  public List<Document> search(String currentQuery, String currentField, int numResults) throws IOException, ParseException {
      List<Document> results = new ArrayList<>();
      // Preprocess the query text
      currentQuery = preprocessText(currentQuery);
      // Create a query parser with the specified field and analyzer
      this.queryParser = new QueryParser(currentField, new StandardAnalyzer());
      // Parse the query string and create a Query object
      this.query = queryParser.parse(currentQuery);
      // Get the top hits from the search and iterate through them
      int numHits = numResults * currentPage;
      topDocs = indexSearcher.search(query, numHits);
      ScoreDoc[] hits = topDocs.scoreDocs;
      // Calculate the start and end indices for the current page
      int start = numResults * (currentPage - 1);
      int end = Math.min(numHits, start + numResults);
      StoredFields storedFields = indexSearcher.storedFields();
      for (int i = start; i < end; i++) {
          // Get the document object for the current hit
          Document hitDoc = storedFields.document(hits[i].doc);
          // Add the document object to the list of results
          results.add(hitDoc);
      }
      printIndexer();
      // Return the list of results
      return results;
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
          return search(currentQuery, currentField, numResultsPerPage);
      }
      return Collections.emptyList();
  }

  // @return The previous page of search results.
  public List<Document> getPreviousPage() throws IOException, ParseException {
      if (hasPreviousPage()) {
          currentPage--;
          return search(currentQuery, currentField, numResultsPerPage);
      }
      return Collections.emptyList();
  }

   // Print method, for debugging to verify that Indexer has been created correctly
   public void printIndexer() throws IOException {
	// Print number of documents in the index
	System.out.println("Number of documents in the index: " + this.indexReader.numDocs());
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
	   this.indexDirectory.close();
	   this.indexReader.close();
   }
   
   private static String preprocessText(String text) {
       // Remove punctuation
	   text = text.replaceAll("[^a-zA-Z0-9\\s.-]", "");
       // Lower case the text
       text = text.toLowerCase();
       // Trim leading and trailing whitespace
       text = text.trim();
       return text;
   }
}