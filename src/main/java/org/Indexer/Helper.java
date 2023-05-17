package org.Indexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Helper {
	static void printFieldNames(String path) throws IOException {
		Directory index = FSDirectory.open(Paths.get(path));

	    IndexReader reader = DirectoryReader.open(index);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    Set<String> fieldNames = new HashSet<>();
	    for (int i = 0; i < reader.maxDoc(); i++) {
	        Document doc = reader.document(i);
	        List<IndexableField> fields = doc.getFields();
	        for (IndexableField field : fields) {
	            fieldNames.add(field.name());
	        }
	    }
	    System.out.println("Field Names:");
	    for (String fieldName : fieldNames) {
	        System.out.println(fieldName);
	    }
	    reader.close();
	}
	   static String preprocessText(String text) {
		    // Remove all punctuation marks except hyphens, periods, and digits
		    text = text.replaceAll("[^a-zA-Z0-9\\s.-]", "");
		    // Lowercase the text
		    text = text.toLowerCase();
		    // Trim leading and trailing whitespace
		    text = text.trim();
		    return text;
	   }
}
