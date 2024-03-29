package org;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.Indexer.Indexer;
import org.Indexer.KeywordIndexerImpl;
import org.Indexer.StandardIndexerImpl;
import org.Searcher.SearchEngine;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import com.opencsv.exceptions.CsvException;

public class LuceneGUI implements ActionListener,DocumentListener,MouseListener {

    // Declare instance variables for the GUI components
	private JFrame frame;
	private JTextField queryTextField;
	private JComboBox<String> fieldComboBox;
	private JTextArea resultsTextArea;
	private JButton searchButton;
	private JLabel pageNumberLabel;
	private JButton prevButton;
	private JButton nextButton;
	private JCheckBox groupingCheckBox;
	private JCheckBox semanticCheckBox;
	private JList<String> similarityList;
	
	private int currentPage = 0;
    private SearchEngine searchEngine;
    
    // Constructor for the GUI
    public LuceneGUI(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
        createGUI();
    }

    private void createGUI() {
		// Create and set up the window
		frame = new JFrame("Lucene GUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane
		JPanel contentPane = new JPanel();
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		contentPane.setLayout(new BorderLayout());
		
		// Create the query panel with the query text field, field combo box, search button and grouping checkbox
		JPanel queryPanel = new JPanel(new GridLayout(1, 0, 30, 0));
		queryTextField = new JTextField();
		fieldComboBox = new JComboBox<>(new String[]{"As Keyword","Artist", "Title", "Album", "Date", "Lyrics", "Year"});
		searchButton = new JButton("Search");
		groupingCheckBox = new JCheckBox("Sort results by Year", false);
		semanticCheckBox = new JCheckBox("Semantic Search", false);
		searchButton.addActionListener(this);
		queryTextField.getDocument().addDocumentListener(this);
		queryPanel.add(queryTextField);
		queryPanel.add(fieldComboBox);
		queryPanel.add(searchButton);
		queryPanel.add(groupingCheckBox);
		queryPanel.add(semanticCheckBox);
		
		// Create the results panel with the results text area and pagination controls
		JPanel resultsPanel = new JPanel(new BorderLayout());
		resultsTextArea = new JTextArea(20, 40);
		resultsTextArea.setEditable(false);
		JScrollPane scrollPaneResults = new JScrollPane(resultsTextArea);
		similarityList = new JList<>(new String[] {});
		similarityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);		
		similarityList.setVisibleRowCount(-1);
		JScrollPane scrollPaneSimilarity = new JScrollPane(similarityList);
		similarityList.addMouseListener(this);
		
		pageNumberLabel = new JLabel("Page " + currentPage);
		prevButton = new JButton("Previous");
		prevButton.addActionListener(this);
		prevButton.setEnabled(false);
		nextButton = new JButton("Next");
		nextButton.addActionListener(this);
		nextButton.setEnabled(false);
		JPanel paginationPanel = new JPanel(new FlowLayout());
		paginationPanel.add(prevButton);
		paginationPanel.add(pageNumberLabel);
		paginationPanel.add(nextButton);
		resultsPanel.add(scrollPaneResults, BorderLayout.CENTER);
		resultsPanel.add(paginationPanel, BorderLayout.SOUTH);
		resultsPanel.add(scrollPaneSimilarity,BorderLayout.WEST);
		
		
		// Add the query panel and results panel to the content pane
		contentPane.add(queryPanel, BorderLayout.NORTH);
		contentPane.add(resultsPanel, BorderLayout.CENTER);

		// Display the window
		frame.setContentPane(contentPane);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
    }
    
    // Method to handle the search button click
    @Override
    public void actionPerformed(ActionEvent event) {
    	boolean isGrouped;
    	boolean semanticSearch;
        if (event.getActionCommand().equals("Search")) {
        	currentPage = 0;
        	String queryString = queryTextField.getText().trim();
        	String field = (String) fieldComboBox.getSelectedItem();
        	if(groupingCheckBox.isSelected()) {
            	isGrouped = true;
        	} else {
        		isGrouped = false;
        	}
        	if(semanticCheckBox.isSelected()) {
        		semanticSearch = true;
        	} else {
        		semanticSearch = false;
        	}
            List<Document> results;
            try {
                results = searchEngine.search(queryString, field,isGrouped, semanticSearch);
                displayResults(results);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            
        } else if (event.getActionCommand().equals("Next")) {
            try {
                if (searchEngine.hasNextPage()) {
                	currentPage++;
                    List<Document> results = searchEngine.getNextPage();
                    displayResults(results);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (event.getActionCommand().equals("Previous")) {
            try {
                if (searchEngine.hasPreviousPage()) {
                	currentPage--;
                    List<Document> results = searchEngine.getPreviousPage();
                    displayResults(results);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } 
        
    }
    private void displayResults(List<Document> results) {

	    String queryString = queryTextField.getText().trim();

	    Analyzer analyzer = new StandardAnalyzer();
	    String field = (String) fieldComboBox.getSelectedItem();
	    pageNumberLabel.setText("Page " + currentPage);
	    resultsTextArea.setText("");
	    Highlighter highlighter = resultsTextArea.getHighlighter();
	    highlighter.removeAllHighlights();        
	    for (Document result : results) {
	        String title = result.get("Title");
	        String artist = result.get("Artist");
	        String year = result.get("Year");
	        String lyrics = result.get("Lyrics"); 
	        String resultString;
	        if(field.equals("Lyrics")) {
		        resultString = title + " by " + artist + " (" + year + ")\n\tLyrics: "+lyrics+"\n";
	        }else {
		        resultString = title + " by " + artist + " (" + year + "\n";

	        }

	        resultsTextArea.append(resultString);
	        try (TokenStream queryTokenStream = analyzer.tokenStream("", new StringReader(queryString))) {
	            CharTermAttribute termAttribute = queryTokenStream.addAttribute(CharTermAttribute.class);
	            queryTokenStream.reset();

	            while (queryTokenStream.incrementToken()) {
	                String queryToken = termAttribute.toString();
	                String[] resultWords = resultString.toLowerCase().split("\\s+");

	                for(String word : resultWords) {
	                    if(getEditDistance(queryToken, word) <= 2) {
	                        int startIndex = resultString.toLowerCase().indexOf(word);
	                        if (startIndex != -1) {
	                            try {
	                                highlighter.addHighlight(
	                                    resultsTextArea.getText().indexOf(resultString) + startIndex,
	                                    resultsTextArea.getText().indexOf(resultString) + startIndex + word.length(),
	                                    new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN)
	                                );
	                            } catch (BadLocationException e) {
	                                e.printStackTrace();
	                            }
	                        }
	                    }
	                }
	            }

	            queryTokenStream.end();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
        
        // Enable or disable the "Previous" button based on the value of searchEngine.hasNextPage()
        if (searchEngine.hasNextPage()) {
            prevButton.setEnabled(true);
        } else {
            prevButton.setEnabled(false);
        }
        
        // Enable or disable the "Next" button based on the value of searchEngine.hasNextPage()
        if (searchEngine.hasNextPage()) {
            nextButton.setEnabled(true);
        } else {
            nextButton.setEnabled(false);
        }
    }
    private static int getEditDistance(String word1, String word2) {
        int len1 = word1.length();
        int len2 = word2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1], Math.min(dp[i][j - 1], dp[i - 1][j])) + 1;
                }
            }
        }

        return dp[len1][len2];
    }

    
    //Document Listener methods
    public void changedUpdate(DocumentEvent event) {
        updateSuggestions();
    }
    public void removeUpdate(DocumentEvent event) {
        updateSuggestions();
    }
    public void insertUpdate(DocumentEvent event) {
        updateSuggestions();
    }
    public void updateSuggestions() {
        String input = queryTextField.getText();
        List<String> historyList = searchEngine.searchHistory(input);
        similarityList.setListData(historyList.toArray(new String[] {}));
    }
    
    //Mouse Listener methods
    public void mousePressed(MouseEvent event) {}
    
    public void mouseReleased(MouseEvent event) {}

    public void mouseEntered(MouseEvent event) {}

    public void mouseExited(MouseEvent event) {}
 
    public void mouseClicked(MouseEvent event) {
    	if (event.getClickCount() == 2) {
            int index = similarityList.locationToIndex(event.getPoint());
            Object suggestedQuery = similarityList.getModel().getElementAt(index);
            String suggestedQueryText = suggestedQuery.toString();
            queryTextField.setText(suggestedQueryText);
        }
    }
    
    public static void main(String[] args) throws IOException, CsvException {

    	Indexer standardIndexer = new StandardIndexerImpl();
    	standardIndexer.createIndexer();
    	Indexer keywordIndexer = new KeywordIndexerImpl();
    	keywordIndexer.createIndexer();
        SearchEngine searchEngine = new SearchEngine();
        new LuceneGUI( searchEngine);
        //searchEngine.close();
    }
}