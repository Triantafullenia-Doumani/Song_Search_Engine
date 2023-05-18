package org.Searcher;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.Constants.LuceneConstants;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.GroupingSearch;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.BooleanClause;

public class StandardSearcherImpl implements Searcher{
	private IndexReader standardIndexReader;
	
	private IndexSearcher standardIndexSearcher;
	
	private Directory standardIndexDirectory;
	
	private TopDocs topDocs;
	private TopGroups<BytesRef> topGroups;
	
	public StandardSearcherImpl() throws IOException {
	    // Open standard index directory
		this.standardIndexDirectory = FSDirectory.open(Paths.get(LuceneConstants.STANDARD_INDEX_FILE_PATH));
		this.standardIndexReader = DirectoryReader.open(this.standardIndexDirectory);
		this.standardIndexSearcher = new IndexSearcher(standardIndexReader);
		if (standardIndexReader == null) {
		    System.out.println("Could not read standard Indexer");
		    System.exit(1);
		}
	}
	@Override
	public List<Document> search(String currentQuery, String currentField,boolean isGrouped, int currentPage) throws IOException, ParseException {
	    List<Document> results = new ArrayList<Document>();

	    // Preprocess the query text
	    currentQuery = Helper.preprocessText(currentQuery);

	    // Create a query parser with the specified field and analyzer
	    QueryParser queryParser = new QueryParser(currentField, new StandardAnalyzer());

	    // Parse the current query
	    Query query = queryParser.parse(currentQuery);

	    // Apply typo correction
	    String correctedQueryString = addTypoCorrection(query, currentField);
	    Query correctedQuery = queryParser.parse(correctedQueryString);

	    
		if (isGrouped) {
	        topGroups = groupingResults(correctedQuery);
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
	        topDocs = standardIndexSearcher.search(correctedQuery, numOut);
	        ScoreDoc[] hits = topDocs.scoreDocs;
		    StoredFields storedFields = standardIndexSearcher.storedFields();
		    if (hits.length > 0) {
		        for (int i = start; i < end && i < hits.length;  i++ ) {
		            // Get the document object for the current hit
		            Document hitDoc = storedFields.document(hits[i].doc);
		            // Add the document object to the list of results
		            results.add(hitDoc);
		        }
		    }
	    }
	    //printIndexer();
	    // Return the list of results
	    return results;
	}

	public String addTypoCorrection(Query query, String currentField) throws ParseException {
	    String queryString = query.toString(currentField);
	    String[] words = queryString.split(" ");
	    String[] correctedWords = new String[words.length];

	    for (int i = 0; i < words.length; i++) {
	        correctedWords[i] = words[i] + "~2";
	    }

	    String correctedQueryString = String.join(" ", correctedWords);
	    return correctedQueryString;
	}


	@Override
	// Group by the specified field
	public TopGroups<BytesRef> groupingResults(Query query) throws IOException{
		
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
	@Override
	// Print method, just for debugging to verify that Indexer has been created correctly
	public void printIndexer() throws IOException {
		System.out.println("Contents of the standard index:");
		int standardNumDocs = standardIndexReader.numDocs();
		for (int i = 0; i < standardNumDocs; i++) {
		    Document doc = standardIndexReader.document(i);
		    System.out.println("Document " + i + ":");
		    List<IndexableField> fields = doc.getFields();
		    for (IndexableField field : fields) {
		    	String value = doc.get(field.name());
		        System.out.println("  " + field.name() + ": " + doc.get(field.name()));
		    }
	    }
	}
	@Override
	public TopDocs getTopDocs() {
		return this.topDocs;
	}
	@Override
	public TopGroups<BytesRef> getTopGroups() {
		return this.topGroups;
	}
	@Override
	public void close() throws IOException {
		this.standardIndexDirectory.close();	
	}
}
