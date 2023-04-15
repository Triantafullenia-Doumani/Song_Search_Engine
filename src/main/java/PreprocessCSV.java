import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class PreprocessCSV {
    public static void main(String[] args) throws IOException {
        String fileName = "../data/final_dataset.csv";
        
        CSVParser parser = null;

        try {
            // Create the CSV parser with the appropriate format
            parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT);
        } catch (FileNotFoundException e) {
            // Handle the FileNotFoundException
            e.printStackTrace();
        } catch (IOException e) {
            // Handle the IOException
            e.printStackTrace();
        }
        // Loop through each record in the CSV file
        for (CSVRecord record : parser) {
            // Get the fields from the current record
            String artist = record.get("Artist");
            String title = record.get("Title");
            String album = record.get("Album");
            String date = record.get("Date");
            String lyrics = record.get("Lyrics");
            String year = record.get("Year");
            
            // Paraphrases the text fields
            artist = preprocessText(artist);
            title = preprocessText(title);
            album = preprocessText(album);
            lyrics = preprocessText(lyrics);
            
        }
        
        // Close the parser
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
