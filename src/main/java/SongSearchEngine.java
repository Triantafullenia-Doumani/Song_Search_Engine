import java.io.IOException;
import java.util.List;

import org.Searcher.Searcher;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.indexer.Indexer;

public class SongSearchEngine {

	public static void main(String[] args) throws IOException, ParseException {
        // Create a new index
        Indexer indexer = new Indexer();
        
        // Search the index
        Searcher searcher = new Searcher("Lady Gaga", "artist");
        List<Document> results = searcher.searchFiles();
        
        // Print the search results
        for (Document doc : results) {
            System.out.println(doc);
        }

	}

}
