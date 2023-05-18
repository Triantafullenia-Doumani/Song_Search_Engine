package org.Searcher;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.Constants.LuceneConstants;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
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
import org.apache.lucene.util.BytesRef;

public class KeywordSearcherImpl implements Searcher{

	private IndexReader keywordIndexReader;
	private IndexSearcher keywordIndexSearcher;
	private Directory keywordIndexDirectory;
	private TopGroups<BytesRef> topGroups;
	TopDocs topDocs;
	
	public KeywordSearcherImpl() throws IOException {
		// Open keyword index directory
		this.keywordIndexDirectory = FSDirectory.open(Paths.get(LuceneConstants.KEYWORD_INDEX_FILE_PATH));
		this.keywordIndexReader = DirectoryReader.open(this.keywordIndexDirectory);
		this.keywordIndexSearcher = new IndexSearcher(keywordIndexReader);
		if (keywordIndexReader == null) {
		    System.out.println("Could not read keyword Indexer");
		    System.exit(1);
		} 
	}
	@Override
	public List<Document> search(String currentQuery, String currentField,boolean isGrouped, int currentPage) throws IOException, ParseException {
	    List<Document> results = new ArrayList<Document>();
	    
	    // Preprocess the query text
	    currentQuery = Helper.preprocessText(currentQuery);

	    // Create a query parser with the specified analyzer
	    QueryParser queryParser = new MultiFieldQueryParser(Helper.getFieldNames(keywordIndexReader), new KeywordAnalyzer());
	    Query query = queryParser.parse(currentQuery);
	    
	    if (isGrouped) {
	        topGroups = groupingResults(query);
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
	    return results;
	}
	@Override
	// Group by the specified field
	public TopGroups<BytesRef> groupingResults(Query query) throws IOException{
	 	GroupingSearch groupingSearch = new GroupingSearch(LuceneConstants.GROUP);
	 	Sort groupSort = new Sort(new SortField(LuceneConstants.GROUP, SortField.Type.STRING));
	 	//Sort groupSort = Sort.RELEVANCE;
	 	groupingSearch.setGroupDocsLimit(50); //posa apotelesmata na exei to kathe group
	 	groupingSearch.setGroupSort(groupSort);
	 	groupingSearch.setSortWithinGroup(groupSort);
	    TopGroups<BytesRef> topGroups = groupingSearch.search(keywordIndexSearcher, query, 0, 50);
	    
	    return topGroups;
	}
	
	@Override
	// Print method, just for debugging to verify that Indexer has been created correctly
	public void printIndexer() throws IOException {
	    System.out.println("Contents of the keyword index:");
	    int keywordNumDocs = keywordIndexReader.numDocs();
	    for (int i = 0; i < keywordNumDocs; i++) {
	        Document doc = keywordIndexReader.document(i);
	        System.out.println("Document " + i + ":");
	        List<IndexableField> fields = doc.getFields();
	        for (IndexableField field : fields) {
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
		this.keywordIndexDirectory.close();	
	}
}
