package org.Searcher;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.util.BytesRef;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
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
import java.util.*;

import org.Constants.LuceneConstants;

public class SemanticSearcherImpl implements Searcher {
	private Word2Vec word2Vec;
	private IndexReader standardIndexReader;
	
	private IndexSearcher standardIndexSearcher;
	
	private Directory standardIndexDirectory;
	
	private TopDocs topDocs;
	private TopGroups<BytesRef> topGroups;
	

	public SemanticSearcherImpl() throws IOException {
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
	    word2Vec = WordVectorSerializer.readWord2VecModel(new File(LuceneConstants.MODEL_PATH_AND_FILE_NAME));
	    List<Document> results = new ArrayList<>();
	
	    // Preprocess the query text
	    currentQuery = Helper.preprocessText(currentQuery);
	
	    // Compute the query vector
	    double[] queryVector = computeQueryVector(currentQuery);
	
	    // Create a query parser with the specified field and analyzer
	    QueryParser queryParser = new QueryParser(currentField, new StandardAnalyzer());
	    Query query = queryParser.parse(currentQuery);
	    int numOut = LuceneConstants.PAGE_SIZE * currentPage;
	
	
	    if (isGrouped) {
	        // Grouping results
	        topGroups = groupingResults(query);
	        GroupDocs<BytesRef>[] groups = topGroups.groups;
	
	        // Calculate the start index for the current page
	        int numGroups = groups.length;
	        int startIndex = (currentPage - 1);
	
	        // Add the documents from the selected group to the list of results
	        if (startIndex >= 0 && startIndex < numGroups) {
	            GroupDocs<BytesRef> group = groups[startIndex];
	
	            // Re-rank the candidate documents based on the cosine similarity
	            Map<Document, Double> scoredDocs = new HashMap<>();
	            for (ScoreDoc scoreDoc : group.scoreDocs) {
	                try {
	                    Document doc = standardIndexSearcher.doc(scoreDoc.doc);
	                    byte[] docVectorBytes = doc.getBinaryValue(currentField + "_vector").bytes;
	                    double[] docVector = parseVector(docVectorBytes);
	                    double cosineSim = cosineSimilarity(queryVector, docVector);
	                    scoredDocs.put(doc, cosineSim);
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	
	            // Sort the documents by the cosine similarity
	            results = scoredDocs.entrySet().stream()
	                    .sorted(Map.Entry.<Document, Double>comparingByValue().reversed())
	                    .map(Map.Entry::getKey)
	                    .collect(Collectors.toList());
	        }
	    } else {
	        // Perform the search using the original query
	        topDocs = standardIndexSearcher.search(query, numOut);
	        ScoreDoc[] hits = topDocs.scoreDocs;
	
	        // Re-rank the candidate documents based on the cosine similarity
	        Map<Document, Double> scoredDocs = new HashMap<>();
	        for (ScoreDoc hit : hits) {
	            Document doc = standardIndexSearcher.doc(hit.doc);
	            byte[] docVectorBytes = doc.getBinaryValue(currentField + "_vector").bytes;
	            double[] docVector = parseVector(docVectorBytes);
	            double cosineSim = cosineSimilarity(queryVector, docVector);
	            scoredDocs.put(doc, cosineSim);
	        }
	
	        // Sort the documents by the cosine similarity
	        results = scoredDocs.entrySet().stream()
	                .sorted(Map.Entry.<Document, Double>comparingByValue().reversed())
	                .map(Map.Entry::getKey)
	                .collect(Collectors.toList());
	
	        // Calculate the start and end indices for the current page
	        int start = LuceneConstants.PAGE_SIZE * (currentPage - 1);
	        int end = Math.min(results.size(), start + LuceneConstants.PAGE_SIZE);
	
	        // Get the sublist for the current page
	        results = results.subList(start, end);
	    }
	
	    return results;
	}
	
		private double[] computeQueryVector(String query) {
		    String[] words = query.split("\\s+");
		    double[] vector = new double[word2Vec.getLayerSize()];
		
		    for (String word : words) {
		        double[] wordVector = word2Vec.getWordVector(word);
		        if (wordVector != null) {
		            for (int i = 0; i < vector.length; i++) {
		                vector[i] += wordVector[i];
		            }
		        }
		    }
		
		    for (int i = 0; i < vector.length; i++) {
		        vector[i] /= words.length;
		    }
		
		    return vector;
		}
		
		private double[] parseVector(byte[] bytes) {
		    double[] vector = new double[bytes.length / Double.BYTES];
		    ByteBuffer buffer = ByteBuffer.wrap(bytes);
		    for (int i = 0; i < vector.length; i++) {
		        vector[i] = buffer.getDouble();
		    }
		    return vector;
		}
		
		private double cosineSimilarity(double[] vector1, double[] vector2) {
		    double dotProduct = 0.0;
		    double normA = 0.0;
		    double normB = 0.0;
		    for (int i = 0; i < vector1.length; i++) {
		        dotProduct += vector1[i] * vector2[i];
		        normA += Math.pow(vector1[i], 2);
		        normB += Math.pow(vector2[i], 2);
		    }
		    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
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
		public TopDocs getTopDocs() {
			return this.topDocs;
		}
		@Override
		public TopGroups<BytesRef> getTopGroups() {
			return this.topGroups;
		}
		@Override
		public void printIndexer() throws IOException {
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
		@Override
		public void close() throws IOException {
			this.standardIndexDirectory.close();	
		}
	
}
