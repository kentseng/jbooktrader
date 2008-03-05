package com.jbooktrader.platform.backtest;

import com.jbooktrader.platform.model.JBookTraderException;
import com.jbooktrader.platform.startup.JBookTrader;
import com.jbooktrader.platform.strategy.Strategy;
import com.jbooktrader.platform.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * Dialog to specify options for back testing using a historical data file.
 */
public class BackTestDialog extends JDialog {
    private static final Dimension MIN_SIZE = new Dimension(500, 120);// minimum frame size
    private final String fileName = "backTester.dataFileName";

    private JPanel progressPanel;
    private JButton cancelButton, backTestButton, selectFileButton;
    private JTextField fileNameText;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    private final Strategy strategy;
    private final PreferencesHolder preferences;
    private BackTestStrategyRunner btsr;

    public BackTestDialog(JFrame parent, Strategy strategy) throws JBookTraderException {
        super(parent);
        this.strategy = strategy;
        preferences = PreferencesHolder.getInstance();
        init();
        pack();
        assignListeners();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void setProgress(long count, long iterations, String label) {
        progressLabel.setText(label);
        int percent = (int) (100 * (count / (double) iterations));
        String text = percent + "%";
        progressBar.setValue(percent);
        progressBar.setString(text);
    }

    public void enableProgress() {
        progressLabel.setText("");
        progressBar.setValue(0);
        progressBar.setString("Starting back test...");
        progressPanel.setVisible(true);
        backTestButton.setEnabled(false);
        cancelButton.setEnabled(true);
        getRootPane().setDefaultButton(cancelButton);
    }

    public void showProgress(String progressText) {
        progressBar.setValue(0);
        progressBar.setString(progressText);
    }


    public void signalCompleted() {
        dispose();
    }


    private void setOptions() throws JBookTraderException {
        String historicalFileName = fileNameText.getText();
        File file = new File(historicalFileName);
        if (!file.exists()) {
            fileNameText.requestFocus();
            String msg = "Historical file " + "\"" + historicalFileName + "\"" + " does not exist.";
            throw new JBookTraderException(msg);
        }
    }

    private void assignListeners() {

        backTestButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    preferences.setProperty(fileName, fileNameText.getText());
                    setOptions();
                    btsr = new BackTestStrategyRunner(BackTestDialog.this, strategy);
                    new Thread(btsr).start();
                } catch (Exception ex) {
                    MessageDialog.showError(BackTestDialog.this, ex.getMessage());
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (btsr != null) {
                    btsr.cancel();
                }
                dispose();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (btsr != null) {
                    btsr.cancel();
                }
                dispose();
            }
        });

        selectFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(JBookTrader.getAppPath());
                fileChooser.setDialogTitle("Select Historical Data File");

                String filename = getFileName();
                if (filename.length() != 0) {
                    fileChooser.setSelectedFile(new File(filename));
                }

                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    fileNameText.setText(file.getAbsolutePath());
                }
            }
        });
    }


    private void init() {

        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Back Test");

        getContentPane().setLayout(new BorderLayout());

        JPanel northPanel = new JPanel(new SpringLayout());
        JPanel centerPanel = new JPanel(new SpringLayout());
        JPanel southPanel = new JPanel(new BorderLayout());

        // strategy panel and its components
        JPanel strategyPanel = new JPanel(new SpringLayout());

        JLabel fileNameLabel = new JLabel("Historical data file:", JLabel.TRAILING);
        fileNameText = new JTextField();
        fileNameText.setText(preferences.getProperty(fileName));
        selectFileButton = new JButton("...");
        fileNameLabel.setLabelFor(fileNameText);

        strategyPanel.add(fileNameLabel);
        strategyPanel.add(fileNameText);
        strategyPanel.add(selectFileButton);

        SpringUtilities.makeOneLineGrid(strategyPanel, 3);

        northPanel.add(strategyPanel);
        SpringUtilities.makeCompactGrid(northPanel, 1, 1, 5, 5, 5, 5);//rows, cols, initX, initY, xPad, yPad

        progressLabel = new JLabel();
        progressLabel.setForeground(Color.BLACK);
        progressBar = new JProgressBar();
        progressBar.setEnabled(false);
        progressBar.setStringPainted(true);

        backTestButton = new JButton("Back Test");
        backTestButton.setMnemonic('B');
        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('C');

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(backTestButton);
        buttonsPanel.add(cancelButton);

        progressPanel = new JPanel(new SpringLayout());
        progressPanel.add(progressLabel);
        progressPanel.add(progressBar);
        progressPanel.setVisible(false);
        SpringUtilities.makeCompactGrid(progressPanel, 1, 2, 12, 5, 12, 5);//rows, cols, initX, initY, xPad, yPad

        southPanel.add(progressPanel, BorderLayout.NORTH);
        southPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(backTestButton);
        setPreferredSize(MIN_SIZE);
        setMinimumSize(getPreferredSize());
    }

    public String getFileName() {
        return fileNameText.getText();
    }

}