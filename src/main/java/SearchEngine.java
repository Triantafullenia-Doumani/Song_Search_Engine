import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class SearchEngine {
    
    private Analyzer analyzer;
    private KeywordAnalyzer keywordAnalyzer;
    private EnglishAnalyzer englishAnalyzer;
    
    
    public SearchEngine() throws IOException {
        // Create the keyword analyzer for fields that should not be analyzed
    	this.keywordAnalyzer = new KeywordAnalyzer();
    	
    	// The StandardAnalyzer in Lucene applies all the tokenization rules:
    	// Such as lowercase, tokenization, stopword removal, and stemming, in sequence, to the input text. 
        this.analyzer = new StandardAnalyzer();
        
        this.englishAnalyzer = new EnglishAnalyzer(null);
      
        // Create the analyzer with per-field configuration
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("Artist", keywordAnalyzer);
        perFieldAnalyzers.put("Title",  analyzer);
        perFieldAnalyzers.put("Album", analyzer);
        perFieldAnalyzers.put("Date", keywordAnalyzer);
        perFieldAnalyzers.put("Year", keywordAnalyzer);
        perFieldAnalyzers.put("Lyrics", englishAnalyzer);
        
    }


    
}