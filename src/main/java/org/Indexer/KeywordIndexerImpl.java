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
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class KeywordIndexerImpl implements Indexer{

	IndexWriter indexWriter;
	CSVParser parser;
	
	@Override
	public void createIndexer() throws IOException {
		
		System.out.println("Keyword Analyzer: ");

	    // Initialize index directory
		Directory index = FSDirectory.open(Paths.get(LuceneConstants.KEYWORD_INDEX_FILE_PATH));
	    if (!DirectoryReader.indexExists(index)) {
	    	System.out.println("	Index does not exist");
	    } else {
            IndexReader reader = DirectoryReader.open(index);
            System.out.println("	Number of documents in existing Index: " + reader.numDocs());
            reader.close();
        }
		// Create index writer configuration
		IndexWriterConfig config = new IndexWriterConfig(new KeywordAnalyzer());
		
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
		        if(header.equals("Year")) {
		        	text = text.replace(".0", "");
		        	System.out.println(text);
		        }
		        // @TOFIX synexisei na bazei ta anepithimita string mesa
		        if((text.equals("lyrics for this song have yet to be released please check back once the song has been released")) || (text.equals("unreleased songs")) || (text.equals("unreleased"))) {
		        	text = "";	
		        }else {
		        	// Preprocess text
		            text = Helper.preprocessText(text);
		        }
     
				if (header.equals(LuceneConstants.GROUP)) {
					doc.add(new SortedDocValuesField (header, new BytesRef(text) ));
					doc.add(new StringField(header, text, Field.Store.YES));
				}else {
					doc.add(new TextField(header, text, Field.Store.YES));
				}
		    }
		    // Add document to the index
		    this.indexWriter.addDocument(doc);
		    // Print vector for debugging
		}
		System.out.println("	New Index created successfully: Number of documents in the new index: " + this.indexWriter.numRamDocs()+"\n");
		close();
		//Helper.printFieldNames(LuceneConstants.KEYWORD_INDEX_FILE_PATH);
	}
   @Override
   public void close() throws IOException {
	   this.indexWriter.close();
	   this.parser.close();
	}
}