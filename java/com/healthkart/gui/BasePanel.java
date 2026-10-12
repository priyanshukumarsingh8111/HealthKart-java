package com.healthkart.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Base abstract class for all GUI tabs/panels.
 * Demonstrates:
 * - OOP: Inheritance (extends JPanel) and Polymorphism (abstract refreshData template method)
 * - Multithreading: Standardized SwingWorker background execution to prevent EDT freezing
 */
public abstract class BasePanel extends JPanel {

    public BasePanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 247, 250));
    }

    /**
     * Polymorphic method implemented by each panel to reload its data.
     */
    public abstract void refreshData();

    /**
     * Polymorphic method returning tab display title.
     */
    public abstract String getPanelTitle();

    /**
     * Helper to create styled primary buttons.
     */
    protected JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(new Color(37, 99, 235));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Helper to create styled header labels.
     */
    protected JLabel createHeaderLabel(String title, String subtitle) {
        JLabel label = new JLabel("<html><div style='padding-bottom:6px;'><span style='font-size:16px; font-weight:bold; color:#1e293b;'>" 
                + title + "</span><br><span style='font-size:11px; color:#64748b;'>" + subtitle + "</span></div></html>");
        return label;
    }

    /**
     * Safely executes a database task on a background worker thread.
     * Prevents GUI freezing (EDT responsiveness) and handles exceptions cleanly.
     *
     * @param backgroundTask Task returning result of type T
     * @param onDone Callback invoked on Event Dispatch Thread with result of type T
     * @param <T> Result type (Generics)
     */
    protected <T> void executeAsync(BackgroundTask<T> backgroundTask, ResultCallback<T> onDone) {
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return backgroundTask.run();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    onDone.onSuccess(result);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(BasePanel.this,
                            "Operation Error: " + cause.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    @FunctionalInterface
    public interface BackgroundTask<T> {
        T run() throws Exception;
    }

    @FunctionalInterface
    public interface ResultCallback<T> {
        void onSuccess(T result);
    }
}
