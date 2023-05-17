package org.Searcher;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;

public class Helper {
	protected static String[] getFieldNames(IndexReader reader) throws IOException {
	    Set<String> fieldSet = new HashSet<String>();
	    for (int i = 0; i < reader.maxDoc(); i++) {
	        Document doc = reader.document(i);
	        for (IndexableField field : doc.getFields()) {
	            fieldSet.add(field.name());
	        }
	    }
	    return fieldSet.toArray(new String[fieldSet.size()]);
	}
	
	protected static String preprocessText(String text) {
		// Remove punctuation
	   text = text.replaceAll("[^a-zA-Z0-9 ]", "");
	   // Lower case the text
	   text = text.toLowerCase();
	   // Trim leading and trailing whitespace
       text = text.trim();

       return text;
	}
}
