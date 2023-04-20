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
  public Searcher(String queryString, String content) throws IOException, ParseException {
	   
       // Open index directory
	   indexDirectory =  FSDirectory.open(Paths.get(LuceneConstants.INDEX_FILE_PATH));

	   // Create IndexSearcher
	   indexReader = DirectoryReader.open(indexDirectory);
	   indexSearcher = new IndexSearcher(indexReader);
	   
 	   // Create a query parser with the specified field and analyzer
	   queryParser = new QueryParser(content, new StandardAnalyzer());
 	   // Parse the query string and create a Query object
 	   query = queryParser.parse(queryString);
 	   
       // Search the index and get the top results
       topDocs = indexSearcher.search(query, LuceneConstants.MAX_SEARCH);

   }

   public List<Document>  searchFiles() throws IOException {

	    List<Document> results = new ArrayList<>();

	    // Get the hits from the topDocs and iterate through them
	    ScoreDoc[] hits = indexSearcher.search(query, LuceneConstants.MAX_SEARCH).scoreDocs;
	    StoredFields storedFields = indexSearcher.storedFields();
	    for (int i = 0; i < hits.length; i++) {
	        // Get the document object for the current hit
	        Document hitDoc = storedFields.document(hits[i].doc);
	        // Add the document object to the list of results
	        results.add(hitDoc);
	    }

	    // Close the index reader and directory
	    indexReader.close();
	    indexDirectory.close();
	    // Return the list of results
	    return results;
   }
}