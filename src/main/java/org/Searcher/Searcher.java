package org.Searcher;

import org.Constants.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
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
   
   /*
    * 
    * @param content: The name of the field in the index that the searcher should search for matching documents.
    * @param queryString: The user's search query string
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

   public List<Document>  search(String queryString, String content) throws IOException, ParseException {
		List<Document> results = new ArrayList<>();
		// Get the hits from the topDocs and iterate through them
		queryString = preprocessText(queryString);
		//content = preprocessText(content);
		System.out.println(content);
		// Create a query parser with the specified field and analyzer
		this.queryParser = new QueryParser(content, new StandardAnalyzer());
		// Parse the query string and create a Query object
		this.query = queryParser.parse(queryString);
		   
		// Search the index and get the top results
		this.topDocs = indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
		ScoreDoc[] hits = topDocs.scoreDocs;
		StoredFields storedFields = indexSearcher.storedFields();
		for (int i = 0; i < hits.length; i++) {
		    // Get the document object for the current hit
		    Document hitDoc = storedFields.document(hits[i].doc);
		    // Add the document object to the list of results
		    results.add(hitDoc);
		}
		//printIndexer();
		// Return the list of results
	    return results;
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
       text = text.replaceAll("[^a-zA-Z0-9 ]", "");
       // Lowercase the text
       text = text.toLowerCase();
       // Trim leading and trailing whitespace
       text = text.trim();
       return text;
   }
}