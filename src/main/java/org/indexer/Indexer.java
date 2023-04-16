package org.indexer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

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
	   
	   Directory index = null;
	   
	   try {
	      //this directory will contain the indexes
	      index = FSDirectory.open(Paths.get(LuceneConstants.FILE_PATH));
	   } catch (FileNotFoundException e) {
	       // Handle the FileNotFoundException
	       e.printStackTrace();
	   } catch (IOException e) {
	       // Handle the IOException
	       e.printStackTrace();
	   }

      //create the indexer
      Analyzer analyzer  = new StandardAnalyzer();
      IndexWriterConfig config = new IndexWriterConfig(analyzer);
      IndexWriter writer = new IndexWriter(index, config);
   
      // Parse CSV file
      CSVFormat format = CSVFormat.DEFAULT;
      CSVParser parser = CSVParser.parse(new File(LuceneConstants.FILE_PATH), StandardCharsets.UTF_8, format);

      // Iterate over records and add to index
      for (CSVRecord record : parser) {
          Document doc = new Document();
          for (String header : parser.getHeaderMap().keySet()) {
              doc.add(new TextField(header, record.get(header), Field.Store.YES));
          }
          writer.addDocument(doc);
      }
      /* Dhladh pio analytika: 
       * 
      for (CSVRecord record : parser) {
          // Get the fields from the current record
          String artist = record.get("Artist");
          String title = record.get("Title");
          String album = record.get("Album");
          String date = record.get("Date");
          String lyrics = record.get("Lyrics");
          String year = record.get("Year");
          
          // Tha mporousame na xrhsimopoihsoume to Preprocess function alla logika tis leitourgiew tou tis ulopoiei hdh h Lucene me tous Analytes
          artist = preprocessText(artist);
          title = preprocessText(title);
          album = preprocessText(album);
          lyrics = preprocessText(lyrics);
          
      }*/

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