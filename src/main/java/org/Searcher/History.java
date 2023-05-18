package org.Searcher;

import java.util.ArrayList;
import java.util.List;

public class History {
	List<String> history = new ArrayList<String>();

	public List<String> getHistory(String text) {
		List<String> historyList = new ArrayList<String>();
		for(String i:history) {
			if(i.startsWith(text)) {
				historyList.add(i);
				System.out.println("HISTORY: \n"+i);
			}
		}
		return historyList;
	}	
	// Add the string query to a list for keeping history
	public void addHistory(String text) {
		if (history.contains(text)) {
	        return;
	    }
		if(history.size() == 10) {
			history.remove(0);
		}
		
		history.add(text);
	}	
}
