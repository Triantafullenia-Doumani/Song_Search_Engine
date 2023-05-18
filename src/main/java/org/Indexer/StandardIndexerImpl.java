package org.Indexer;

import org.Constants.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

public class StandardIndexerImpl implements Indexer{
	IndexWriter indexWriter;
	CSVParser parser;

	@Override
	public void createIndexer() throws IOException {
		
		System.out.println("Standard Analyzer: ");
		// Load the pretrained model
        Word2Vec word2Vec = WordVectorSerializer.readWord2VecModel(new File(LuceneConstants.MODEL_PATH_AND_FILE_NAME));
        // Initialize index directory
        Directory index = FSDirectory.open(Paths.get(LuceneConstants.STANDARD_INDEX_FILE_PATH));
	    if (!DirectoryReader.indexExists(index)) {
	    	System.out.println("	Index does not exist");
	    } else {
            IndexReader reader = DirectoryReader.open(index);
            System.out.println("	Number of documents in existing Index: " + reader.numDocs());
            reader.close();
        }
		// Create index writer configuration
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		
		// Create index writer
		this.indexWriter = new IndexWriter(index, config);
	    // Delete existing index, if it exists
	    if (DirectoryReader.indexExists(index)) {
	    	System.out.println("	Deleting existing index...");
	    	this.indexWriter.deleteAll();
	    }
		// Parse CSV file
		CSVFormat format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
		
		this.parser = CSVParser.parse(new File(LuceneConstants.CSV_PATH_AND_FILE_NAME), StandardCharsets.UTF_8, format);
		
		// Check if header is present in CSV file
		Map<String, Integer> headerMap = parser.getHeaderMap();
		if (headerMap == null) {
			indexWriter.close();
		    throw new IOException("	CSV file does not have header");
		}
		// Iterate over records and add to index
		for (CSVRecord record : parser) {
		    // Create new document for each record
		    Document doc = new Document();
	        
		    // Create an array to store the vector representation
		    double[] vector = new double[word2Vec.getLayerSize()];
		    
		    // Add each field to the document
		    for (String header : headerMap.keySet()) {
		        // Get the text value for the header
		        String text = record.get(header);
		        if(header.equals("Year")) {
		        	text = text.replace(".0", "");
		        }
		        
		        // @TOFIX synexisei na bazei ta anepithimita string mesa
		        if((text.equals("lyrics for this song have yet to be released please check back once the song has been released")) || (text.equals("unreleased songs")) || (text.equals("unreleased"))) {
		        	text = "";	
		        }

	            // Split the text field into individual words
	            String[] words = text.split("\\s+");
	            
	            // Group the word vectors to obtain the vector representation for the text field
	            for (String word : words) {
	                double[] wordVector = word2Vec.getWordVector(word);
	                if (wordVector != null) {
	                    for (int i = 0; i < vector.length; i++) {
	                        vector[i] += wordVector[i];
	                    }
	                }
	            }
	            //By dividing each element of the vector by the number of words, the resulting vector representation
	            //will have an average value across all dimensions. This normalization  helps in comparing and measuring 
	            //the similarity between different vector representations.
	            for (int i = 0; i < vector.length; i++) {
	                vector[i] /= words.length;
	            }
				if (header.equals(LuceneConstants.GROUP)) {
					doc.add(new SortedDocValuesField (header, new BytesRef(text) ));
					doc.add(new StoredField(header, text));
				}else {
					doc.add(new TextField(header, text, Field.Store.YES));
				}
				// Create a stored field for the vector and add it to the document
				ByteBuffer byteBuffer = ByteBuffer.allocate(Double.BYTES * vector.length);
				for (double v : vector) {
				    byteBuffer.putDouble(v);
				}
				Field vectorField = new StoredField(header + "_vector", byteBuffer.array());
				doc.add(vectorField);

		    }
		    // Add document to the index
		    this.indexWriter.addDocument(doc);
		    // Print vector for debugging
		   // System.out.println("Vector: " + Arrays.toString(vector));

		}
		System.out.println("	New Index created successfully: Number of documents in the new index: " + this.indexWriter.numRamDocs()+"\n");
		close();
		//Helper.printFieldNames(LuceneConstants.STANDARD_INDEX_FILE_PATH);
	}
	

	@Override
	public void close() throws IOException {
		this.indexWriter.close();
		this.parser.close();
	}
}