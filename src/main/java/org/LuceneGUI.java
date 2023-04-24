package org;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.Searcher.Searcher;
import org.apache.lucene.document.Document;
import org.indexer.Indexer;

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
	private int currentPage = 1;
	private int pageSize = 10;
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

		// Create the query panel with the query text field, field combo box and search button
		JPanel queryPanel = new JPanel(new GridLayout(1, 0, 10, 0));
		queryTextField = new JTextField();
		fieldComboBox = new JComboBox<>(new String[]{"Artist", "Title", "Album", "Date", "Lyrics", "Year"});
		searchButton = new JButton("Search");
		searchButton.addActionListener(this);
		queryPanel.add(queryTextField);
		queryPanel.add(fieldComboBox);
		queryPanel.add(searchButton);

		// Create the results panel with the results text area and pagination controls
		JPanel resultsPanel = new JPanel(new BorderLayout());
		resultsTextArea = new JTextArea(20, 40);
		resultsTextArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(resultsTextArea);
		pageNumberLabel = new JLabel("Page " + currentPage);
		prevButton = new JButton("Prev");
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
        if (event.getActionCommand().equals("Search")) {
            String queryString = queryTextField.getText().trim();
            String field = (String) fieldComboBox.getSelectedItem();
            List<Document> results;
            try {
                results = searcher.search(queryString, field, pageSize);
                displayResults(results);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (event.getActionCommand().equals("Next Page")) {
            try {
                if (searcher.hasNextPage()) {
                    List<Document> results = searcher.getNextPage();
                    displayResults(results);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (event.getActionCommand().equals("Previous Page")) {
            try {
                if (searcher.hasPreviousPage()) {
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
        resultsTextArea.setText("");
        for (Document result : results) {
            resultsTextArea.append(result.get("Title") + " by " + result.get("Artist") + " (" + result.get("Year") + ")\n");
        }
    }


    public static void main(String[] args) throws IOException {
        new Indexer();
        Searcher searcher = new Searcher();
        new LuceneGUI( searcher);
    }
}
