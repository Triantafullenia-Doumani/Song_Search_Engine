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
    private JPanel panel;
    private JTextField queryTextField;
    private JComboBox<String> fieldComboBox;
    private JTextArea resultsTextArea;
    private Searcher searcher;
    
    // Constructor for the GUI
    public LuceneGUI(Searcher searcher) {
        this.searcher = searcher;
        createGUI();
    }

    private void createGUI() {
        // Create and set up the frame
        frame = new JFrame("Lucene Search");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);

        // Create and set up the panel
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);

        // Add the query label and text field
        JLabel queryLabel = new JLabel("Query:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(queryLabel, constraints);
        queryTextField = new JTextField(20);
        constraints.gridx = 1;
        panel.add(queryTextField, constraints);

        // Add the field label and combo box
        JLabel fieldLabel = new JLabel("Field:");
        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(fieldLabel, constraints);
        fieldComboBox = new JComboBox<>(new String[] {"Artist", "Title", "Album", "Date", "Lyrics", "Year"});
        constraints.gridx = 1;
        panel.add(fieldComboBox, constraints);

        // Add the search button
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(this);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        panel.add(searchButton, constraints);

        // Add the results text area
        resultsTextArea = new JTextArea(10, 40);
        resultsTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultsTextArea);
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        panel.add(scrollPane, constraints);

        // Add the panel to the frame and show it
        frame.getContentPane().add(panel);
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
                results = searcher.search(queryString, field);
                resultsTextArea.setText("");
                for (Document result : results) {
                    resultsTextArea.append(result.get("Title") + " by " + result.get("Artist") + " (" + result.get("Year") + ")\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    public static void main(String[] args) throws IOException {
        Indexer indexer = new Indexer();
        Searcher searcher = new Searcher();
        LuceneGUI gui = new LuceneGUI( searcher);
    }
}
