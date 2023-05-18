package org.Searcher;

import java.io.IOException;

public class SearcherFactory {
    public static Searcher selectSearcher(String currentField, boolean semanticSearch) throws IOException {
    	if(currentField.equals("As Keyword")) {
    		System.out.println("Searching in keyword Indexer...");
    		return new KeywordSearcherImpl();
		  }else {
			  if(semanticSearch) {
				  System.out.println("Semantic searching in Standard Indexer...");
				  return new SemanticSearcherImpl();
			  }else {
				  System.out.println("Searching in Standard Indexer...");
				  return new StandardSearcherImpl();
			  }
		  }

    }
}