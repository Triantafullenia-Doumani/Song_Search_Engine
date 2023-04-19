
package org.indexer;

import org.Constants.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer {


   public Indexer() throws IOException {
	    // Initialize index directory
	    Directory index = null;
	    index = FSDirectory.open(Paths.get(LuceneConstants.INDEX_FILE_PATH));

	    // Create analyzer and index writer configuration
	    Analyzer analyzer  = new StandardAnalyzer();
	    IndexWriterConfig config = new IndexWriterConfig(analyzer);

	    // Create index writer
	    IndexWriter writer = new IndexWriter(index, config);

	    // Parse CSV file
	    CSVFormat format = CSVFormat.DEFAULT;
	    CSVParser parser = CSVParser.parse(new File(LuceneConstants.CSV_PATH_AND_FILE_NAME), StandardCharsets.UTF_8, format);
	    
	 // Check if header is present in CSV file
	    Map<String, Integer> headerMap = parser.getHeaderMap();
	    if (headerMap == null) {
	       writer.close();
	       throw new IOException("CSV file does not have header");
	    }
	    
	    // Iterate over records and add to index
	    for (CSVRecord record : parser) {
	        // Create new document for each record
	        Document doc = new Document();
	        
	        // Add each field to the document
	        for (String header : parser.getHeaderMap().keySet()) {
	            // Get the text value for the header
	            String text = record.get(header);
	            
	            // Preprocess text
	            text = preprocessText(text);
	            
	            // Add field to document
	            doc.add(new TextField(header, text, Field.Store.YES));
	        }
	        // Add document to the index
	        writer.addDocument(doc);
      }
      // Close index writer and parser
      writer.close();
      parser.close();
   
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