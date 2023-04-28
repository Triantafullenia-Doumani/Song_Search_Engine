package org;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import org.Indexer.KeywordIndexer;
import org.Indexer.StandardIndexer;
import org.Searcher.Searcher;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import com.opencsv.exceptions.CsvException;

public class LuceneGUI implements ActionListener {

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
	
	private int currentPage = 1;
    private Searcher searcher;
    
    // Constructor for the GUI
    public LuceneGUI(Searcher searcher) {
        this.searcher = searcher;
        createGUI();
    }

    private void createGUI() {
		// Create and set up the window
		frame = new JFrame("Lucene GUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane
		JPanel contentPane = new JPanel();
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.setLayout(new BorderLayout());
		
		
		// Create the query panel with the query text field, field combo box, search button and grouping checkbox
		JPanel queryPanel = new JPanel(new GridLayout(1, 0, 10, 0));
		queryTextField = new JTextField();
		fieldComboBox = new JComboBox<>(new String[]{"General Search","Artist", "Title", "Album", "Date", "Lyrics", "Year"});
		searchButton = new JButton("Search");
		groupingCheckBox = new JCheckBox("Sort results by Artist", true);

		searchButton.addActionListener(this);
		queryPanel.add(queryTextField);
		queryPanel.add(fieldComboBox);
		queryPanel.add(searchButton);
		queryPanel.add(groupingCheckBox);

		// Create the results panel with the results text area and pagination controls
		JPanel resultsPanel = new JPanel(new BorderLayout());
		resultsTextArea = new JTextArea(20, 40);
		resultsTextArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(resultsTextArea);
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
		resultsPanel.add(scrollPane, BorderLayout.CENTER);
		resultsPanel.add(paginationPanel, BorderLayout.SOUTH);

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
        if (event.getActionCommand().equals("Search")) {
        	String queryString = queryTextField.getText().trim();
        	String field = (String) fieldComboBox.getSelectedItem();
        	if(groupingCheckBox.isSelected()) {
            	isGrouped = true;
        	} else {
        		isGrouped = false;
        	}
            List<Document> results;
            try {
                results = searcher.search(queryString, field,isGrouped);
                displayResults(results);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (event.getActionCommand().equals("Next")) {
            try {
                if (searcher.hasNextPage()) {
                	currentPage++;
                    List<Document> results = searcher.getNextPage();
                    displayResults(results);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (event.getActionCommand().equals("Previous")) {
            try {
                if (searcher.hasPreviousPage()) {
                	currentPage--;
                    List<Document> results = searcher.getPreviousPage();
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
    	String field = (String) fieldComboBox.getSelectedItem();
        pageNumberLabel.setText("Page " + currentPage);
        resultsTextArea.setText("");
        Highlighter highlighter = resultsTextArea.getHighlighter();
        highlighter.removeAllHighlights();
        if(groupingCheckBox.isSelected()) {
        	
        }
        for (Document result : results) {
            String title = result.get("Title");
            String artist = result.get("Artist");
            String year = result.get("Year");
            String resultString = title + " by " + artist + " (" + year + ")\n";
            resultsTextArea.append(resultString);
            int startIndex = resultString.indexOf(queryString);
            if(field.equals("General Search")) {
	            if (startIndex != -1) {
	                try {
	                    highlighter.addHighlight(
	                            resultsTextArea.getText().indexOf(resultString) + startIndex,
	                            resultsTextArea.getText().indexOf(resultString) + startIndex + queryString.length(),
	                            new DefaultHighlighter.DefaultHighlightPainter(Color.PINK)
	                    );
	                } catch (BadLocationException e) {
	                    e.printStackTrace();
	                }
	            }
            }
            
        }
        // Enable or disable the "Previous" button based on the value of searcher.hasNextPage()
        if (searcher.hasNextPage()) {
            prevButton.setEnabled(true);
        } else {
            prevButton.setEnabled(false);
        }
        
        // Enable or disable the "Next" button based on the value of searcher.hasNextPage()
        if (searcher.hasNextPage()) {
            nextButton.setEnabled(true);
        } else {
            nextButton.setEnabled(false);
        }
    }




    public static void main(String[] args) throws IOException, CsvException {
        new StandardIndexer();
        new KeywordIndexer();
        Searcher searcher = new Searcher();
        new LuceneGUI( searcher);
        searcher.close();
    }
}
