package org.Indexer;

import org.Constants.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class KeywordIndexer {

	IndexWriter indexWriter;
	CSVParser parser;

	public KeywordIndexer() throws IOException {
	    // Initialize index directory
		Directory index = FSDirectory.open(Paths.get(LuceneConstants.KEYWORD_INDEX_FILE_PATH));
		
		// Create index writer configuration
		IndexWriterConfig config = new IndexWriterConfig(new KeywordAnalyzer());
		
		// Create index writer
		this.indexWriter = new IndexWriter(index, config);
		
		// Parse CSV file
		CSVFormat format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
		
		this.parser = CSVParser.parse(new File(LuceneConstants.CSV_PATH_AND_FILE_NAME), StandardCharsets.UTF_8, format);
		
		// Check if header is present in CSV file
		Map<String, Integer> headerMap = parser.getHeaderMap();
		if (headerMap == null) {
			indexWriter.close();
		    throw new IOException("CSV file does not have header");
		}
		// Iterate over records and add to index
		for (CSVRecord record : parser) {
		    // Create new document for each record
		    Document doc = new Document();
		    
		    // Add each field to the document
		    for (String header : headerMap.keySet()) {
		        // Get the text value for the header
		        String text = record.get(header);
		        	        
		        header = preprocessText(header);
		        // Add field to document
		        doc.add(new StringField(header, text, Field.Store.YES));
		    }
		    // Add document to the index
		    this.indexWriter.addDocument(doc);
		  }
		 System.out.println("Index created successfully: Number of documents in the index: " + this.indexWriter.numRamDocs());
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
   
   public void close() throws IOException {
	   this.indexWriter.close();
	   this.parser.close();
   }
}
