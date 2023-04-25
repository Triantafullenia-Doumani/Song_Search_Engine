package org.Searcher;

import org.Constants.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.index.StoredFields;

public class Searcher {
	
	private IndexReader keywordIndexReader;
	private IndexReader standardIndexReader;
	
	private IndexSearcher standardIndexSearcher;
	private IndexSearcher keywordIndexSearcher;
	
	private Directory standardIndexDirectory;
	private Directory keywordIndexDirectory;
	
	private TopDocs topDocs;
	
	private int currentPage = 1;
	   
	private String currentQuery;
	private String currentField;
	   
  public Searcher() throws IOException{

	// Open keyword index directory
	this.keywordIndexDirectory = FSDirectory.open(Paths.get(LuceneConstants.KEYWORD_INDEX_FILE_PATH));
	this.keywordIndexReader = DirectoryReader.open(this.keywordIndexDirectory);
	this.keywordIndexSearcher = new IndexSearcher(keywordIndexReader);
	if (keywordIndexReader == null) {
	    System.out.println("Could not read keyword Indexer");
	    System.exit(1);
	} 
    // Open standard index directory
	this.standardIndexDirectory = FSDirectory.open(Paths.get(LuceneConstants.STANDARD_INDEX_FILE_PATH));
	this.standardIndexReader = DirectoryReader.open(this.standardIndexDirectory);
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
	  System.out.println("current query: "+currentQuery);
	  System.out.println("current Field: "+currentField+"\n");
	  
	  List<Document> results = new ArrayList<Document>();
	  
	  this.currentQuery = currentQuery;
	  this.currentField = currentField;
	  
	  if(currentField.equals("General Search")) {
		  System.out.println("Searching in keyword Indexer...");
		  results = searchKeyword();
	  }else {
		  this.currentField = currentField;
		  results = searchStandard();
		  System.out.println("Searching in Standard Indexer...");
	  }
	  if(results.isEmpty()) {
		  System.out.println("No results found");
	  }
      return results;
  }
  

private List<Document> searchStandard() throws IOException, ParseException {
	 List<Document> results = new ArrayList<Document>();
	  
	  // Preprocess the query text
    currentQuery = preprocessText(currentQuery);
    // Create a query parser with the specified field and analyzer
	QueryParser queryParser = new QueryParser(currentField, new StandardAnalyzer());
	Query query = queryParser.parse(currentQuery);

    // Get the top hits from the search and iterate through them
    int numHits = LuceneConstants.PAGE_SIZE * currentPage;
    topDocs = standardIndexSearcher.search(query, numHits);
    ScoreDoc[] hits = topDocs.scoreDocs;
    
    // Calculate the start and end indices for the current page
    int start = LuceneConstants.PAGE_SIZE * (currentPage - 1);
    int end = Math.min(numHits, start + LuceneConstants.PAGE_SIZE);
    
    StoredFields storedFields = standardIndexSearcher.storedFields();
    if (hits.length > 0) {
        for (int i = start; i < end; i++) {
            // Get the document object for the current hit
            Document hitDoc = storedFields.document(hits[i].doc);
            // Add the document object to the list of results
            results.add(hitDoc);
        }
    }
    //printStandardIndexer();
    // Return the list of results
    return results;
	
}
private List<Document> searchKeyword() throws IOException, ParseException {
    List<Document> results = new ArrayList<Document>();

    // Preprocess the query text
    currentQuery = preprocessText(currentQuery);

    // Create a query parser with the specified analyzer
    QueryParser queryParser = new MultiFieldQueryParser(getFieldNames(keywordIndexReader), new KeywordAnalyzer());
    Query query = queryParser.parse(currentQuery);

    // Get the top hits from the search and iterate through them
    int numHits = LuceneConstants.PAGE_SIZE * currentPage;
    topDocs = keywordIndexSearcher.search(query, numHits);
    ScoreDoc[] hits = topDocs.scoreDocs;
    // Calculate the start and end indices for the current page
    int start = LuceneConstants.PAGE_SIZE * (currentPage - 1);
    int end = Math.min(numHits, start + LuceneConstants.PAGE_SIZE);
    StoredFields storedFields = keywordIndexSearcher.storedFields();
    if (hits.length > 0) {
        for (int i = start; i < end; i++) {
            // Get the document object for the current hit
            Document hitDoc = storedFields.document(hits[i].doc);
            // Add the document object to the list of results
            results.add(hitDoc);
        }
    }
    printKeywordIndexer();
    // Return the list of results
    return results;
}

private String[] getFieldNames(IndexReader reader) throws IOException {
    Set<String> fieldSet = new HashSet<String>();
    for (int i = 0; i < reader.maxDoc(); i++) {
        Document doc = reader.document(i);
        for (IndexableField field : doc.getFields()) {
            fieldSet.add(field.name());
        }
    }
    return fieldSet.toArray(new String[fieldSet.size()]);
}

	// @return True if there is a next page, false otherwise.
	public boolean hasNextPage() {
		int numHits = (int) topDocs.totalHits.value;
		int maxPage = numHits / LuceneConstants.PAGE_SIZE + (numHits % LuceneConstants.PAGE_SIZE == 0 ? 0 : 1);
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
	public void printKeywordIndexer() throws IOException {
	    System.out.println("Contents of the standard index:");
	    int standardNumDocs = keywordIndexReader.numDocs();
	    for (int i = 0; i < standardNumDocs; i++) {
	        Document doc = keywordIndexReader.document(i);
	        System.out.println("Document " + i + ":");
	        List<IndexableField> fields = doc.getFields();
	        for (IndexableField field : fields) {
	            System.out.println("  " + field.name() + ": " + doc.get(field.name()));
	        }
	    }
	}
	    
	// Print method, just for debugging to verify that Indexer has been created correctly
	public void printStandardIndexer() throws IOException {
		System.out.println("Contents of the keyword index:");
		int keywordNumDocs = standardIndexReader.numDocs();
		for (int i = 0; i < keywordNumDocs; i++) {
		    Document doc = standardIndexReader.document(i);
		    System.out.println("Document " + i + ":");
		    List<IndexableField> fields = doc.getFields();
		    for (IndexableField field : fields) {
		        System.out.println("  " + field.name() + ": " + doc.get(field.name()));
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