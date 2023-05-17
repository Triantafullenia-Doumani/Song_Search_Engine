package org.Searcher;

import java.io.IOException;

public class SearcherFactory {
    public static Searcher createSearcher(String currentField, boolean senematicSearch) throws IOException {
    	if(currentField.equals("As Keyword")) {
    		System.out.println("Searching in keyword Indexer...");
    		return new KeywordSearcherImpl();
		  }else {
			  if(senematicSearch) {
				  System.out.println("Senematic searching in Standard Indexer...");
				  return new SenematicSearcherImpl();
			  }else {
				  System.out.println("Searching in Standard Indexer...");
				  return new StandardSearcherImpl();
			  }
		  }

    }
}