package org.Searcher;

import org.Constants.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.util.BytesRef;
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
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.GroupingSearch;
import org.apache.lucene.search.grouping.TopGroups;
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
	private TopGroups<BytesRef> topGroups;
	
	private int currentPage = 1;
	
	private String currentQuery;
	private String currentField;
	private boolean isGrouped;

	List<String> history = new ArrayList<String>();
	
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
  public List<Document> search(String currentQuery, String currentField,boolean isGrouped) throws IOException, ParseException {
	  System.out.println("current query: "+currentQuery);
	  System.out.println("current Field: "+currentField+"\n");
	  
	  List<Document> results = new ArrayList<Document>();
	  
	  this.currentQuery = currentQuery;
	  this.currentField = currentField;
	  this.isGrouped = isGrouped;
	  
	  if(currentField.equals("As Keyword")) {
		  System.out.println("Searching in keyword Indexer...");
		  results = searchKeyword(isGrouped);
	  }else {
		  this.currentField = currentField;
		  results = searchStandard(isGrouped);
		  System.out.println("Searching in Standard Indexer...");
	  }
	  if(results.isEmpty()) {
		  System.out.println("No results found");
	  }
      return results;
  }
  

private List<Document> searchStandard(boolean isGrouped) throws IOException, ParseException {
	List<Document> results = new ArrayList<Document>();

	 
	 // Preprocess the query text

    currentQuery = preprocessText(currentQuery);
    // Create a query parser with the specified field and analyzer
	QueryParser queryParser = new QueryParser(currentField, new StandardAnalyzer());
	Query query = queryParser.parse(currentQuery);
	

	addHistory(currentQuery);
    //System.out.println(currentQuery);
	
    if (isGrouped) {
        topGroups = groupingStandardResults(query);
        GroupDocs<BytesRef>[] groups = topGroups.groups;

        // Calculate the start index for the current page
        int numGroups = groups.length;
        int startIndex = (currentPage - 1);

        // Add the documents from the selected group to the list of results
        if (startIndex >= 0 && startIndex < numGroups) {
            GroupDocs<BytesRef> group = groups[startIndex];
            for (ScoreDoc scoreDoc : group.scoreDocs) {
                try {
                    Document doc = standardIndexSearcher.doc(scoreDoc.doc);
                    results.add(doc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }else {
        // Calculate the start and end indices for the current page
    	int numOut = LuceneConstants.PAGE_SIZE * currentPage;	
        int start = LuceneConstants.PAGE_SIZE * (currentPage - 1);
        int end = Math.min(numOut, start + LuceneConstants.PAGE_SIZE);
        
        topDocs = standardIndexSearcher.search(query, numOut);
        ScoreDoc[] hits = topDocs.scoreDocs;
	    StoredFields storedFields = standardIndexSearcher.storedFields();
	    if (hits.length > 0) {
	        for (int i = start; i < end; i++) {
	            // Get the document object for the current hit
	            Document hitDoc = storedFields.document(hits[i].doc);
	            // Add the document object to the list of results
	            results.add(hitDoc);
	        }
	    }
    }
    //printStandardIndexer();
    // Return the list of results
    return results;
	
}
private List<Document> searchKeyword(boolean isGrouped) throws IOException, ParseException {
    List<Document> results = new ArrayList<Document>();
    
    // Preprocess the query text
    currentQuery = preprocessText(currentQuery);

    // Create a query parser with the specified analyzer
    QueryParser queryParser = new MultiFieldQueryParser(getFieldNames(keywordIndexReader), new KeywordAnalyzer());
    Query query = queryParser.parse(currentQuery);
    

    addHistory(currentQuery);

    if (isGrouped) {
        topGroups = groupingKeywordResults(query);
        GroupDocs<BytesRef>[] groups = topGroups.groups;

        // Calculate the start index for the current page
        int numGroups = groups.length;
        int startIndex = (currentPage - 1);

        // Add the documents from the selected group to the list of results
        if (startIndex >= 0 && startIndex < numGroups) {
            GroupDocs<BytesRef> group = groups[startIndex];
            for (ScoreDoc scoreDoc : group.scoreDocs) {
                try {
                    Document doc = keywordIndexSearcher.doc(scoreDoc.doc);
                    results.add(doc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }else {  
        // Calculate the start and end indices for the current page
    	int numOut = LuceneConstants.PAGE_SIZE * currentPage;	
        int start = LuceneConstants.PAGE_SIZE * (currentPage - 1);
        int end = Math.min(numOut, start + LuceneConstants.PAGE_SIZE);
        
	    topDocs = keywordIndexSearcher.search(query, numOut);
	    ScoreDoc[] hits = topDocs.scoreDocs;
	    
	    StoredFields storedFields = keywordIndexSearcher.storedFields();
	    if (hits.length > 0) {
	        for (int i = start; i < end; i++) {
	            // Get the document object for the current hit
	            Document hitDoc = storedFields.document(hits[i].doc);
	            // Add the document object to the list of results
	            results.add(hitDoc);
	        }
	    }
    }

    //printKeywordIndexer();
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
			return search(currentQuery, currentField,isGrouped);
		}
		return Collections.emptyList();
  }

	// @return The previous page of search results.
	public List<Document> getPreviousPage() throws IOException, ParseException {
		if (hasPreviousPage()) {
			currentPage--;
			return search(currentQuery, currentField,isGrouped);
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
	
	// Group by the specified field
	public TopGroups<BytesRef> groupingKeywordResults(Query query) throws IOException{
		
	 	GroupingSearch groupingSearch = new GroupingSearch(LuceneConstants.GROUP);
	 	Sort groupSort = new Sort(new SortField(LuceneConstants.GROUP, SortField.Type.STRING));
	 	//Sort groupSort = Sort.RELEVANCE;
	 	groupingSearch.setGroupDocsLimit(50);
	 	groupingSearch.setGroupSort(groupSort);
	 	groupingSearch.setSortWithinGroup(groupSort);
	    TopGroups<BytesRef> topGroups = groupingSearch.search(keywordIndexSearcher, query, 0, 50);
	    
	    return topGroups;
	}
	
	// Group by the specified field
	public TopGroups<BytesRef> groupingStandardResults(Query query) throws IOException{
		
	 	GroupingSearch groupingSearch = new GroupingSearch(LuceneConstants.GROUP);
	 	//int yearNumeric = Integer.parseInt(groupingField);
	 	Sort groupSort = new Sort(new SortField(LuceneConstants.GROUP, SortField.Type.STRING));
	 	//Sort groupSort = Sort.RELEVANCE;
	 	groupingSearch.setGroupDocsLimit(50);
	 	groupingSearch.setGroupSort(groupSort);
	 	groupingSearch.setSortWithinGroup(groupSort);
	    TopGroups<BytesRef> topGroups = groupingSearch.search(standardIndexSearcher, query, 0, 50);
	    
	    return topGroups;
	}
	
	// Add the string query to a list for keeping history
	public void addHistory(String text) {
		if (history.contains(text)) {
	        return;
	    }
		if(history.size() == 10) {
			history.remove(0);
		}
		
		history.add(text);
		/*for(int i=0; i<history.size(); i++) {
			System.out.println(history.get(i));
		}**/
	}
	public List<String> searchHistory(String text) {
		List<String> historyList = new ArrayList<String>();
		for(String i:history) {
			if(i.startsWith(text)) {
				historyList.add(i);
				System.out.println("HISTORY: \n"+i);
			}
		}
		/*for(int k=0; k<historyList.size(); k++) {
			System.out.println(historyList.get(k));
		}*/
			return historyList;
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