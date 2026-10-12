package com.healthkart.gui;

import com.healthkart.dao.PrescriptionDao;
import com.healthkart.dao.ReminderDao;
import com.healthkart.model.Prescription;
import com.healthkart.model.Reminder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Prescriptions and Dosage Reminders Management Panel.
 * Demonstrates:
 * - GUI Components: JTabbedPane, JTable, JDialog, JCheckBox
 * - Database Operations: Dual entity management (prescriptions and reminders)
 * - Collections: Lists, iteration, asynchronous updates
 */
public class PrescriptionReminderPanel extends BasePanel {

    private final PrescriptionDao prescriptionDao;
    private final ReminderDao reminderDao;

    private JTable prescriptionTable;
    private DefaultTableModel prescriptionModel;

    private JTable reminderTable;
    private DefaultTableModel reminderModel;

    private List<Prescription> currentPrescriptions;
    private List<Reminder> currentReminders;

    public PrescriptionReminderPanel(PrescriptionDao prescriptionDao, ReminderDao reminderDao) {
        this.prescriptionDao = prescriptionDao;
        this.reminderDao = reminderDao;
        initUI();
        refreshData();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(createHeaderLabel("Prescriptions & Active Medicine Reminders",
                "View digital prescriptions issued by doctors and manage scheduled dosage reminders"), BorderLayout.WEST);

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        topPanel.add(refreshBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JTabbedPane subTabs = new JTabbedPane();

        // --- Tab 1: Prescriptions ---
        JPanel presPanel = new JPanel(new BorderLayout(5, 5));
        String[] presCols = {"ID", "Patient Name", "Doctor", "Diagnosis", "Notes", "Medicines"};
        prescriptionModel = new DefaultTableModel(presCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        prescriptionTable = new JTable(prescriptionModel);
        prescriptionTable.setRowHeight(24);
        presPanel.add(new JScrollPane(prescriptionTable), BorderLayout.CENTER);
        subTabs.addTab("Digital Prescriptions", presPanel);

        // --- Tab 2: Reminders ---
        JPanel remPanel = new JPanel(new BorderLayout(5, 5));
        String[] remCols = {"ID", "Medicine", "Timing", "Scheduled Time", "Instructions", "Patient Email", "Active Status"};
        reminderModel = new DefaultTableModel(remCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        reminderTable = new JTable(reminderModel);
        reminderTable.setRowHeight(24);
        remPanel.add(new JScrollPane(reminderTable), BorderLayout.CENTER);

        JPanel remActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton toggleActiveBtn = new JButton("Toggle Selected Reminder Active/Inactive");
        toggleActiveBtn.addActionListener(e -> toggleReminderActive());

        JButton addReminderBtn = createPrimaryButton("+ Add New Reminder");
        addReminderBtn.addActionListener(e -> openAddReminderDialog());

        remActions.add(toggleActiveBtn);
        remActions.add(addReminderBtn);
        remPanel.add(remActions, BorderLayout.SOUTH);

        subTabs.addTab("Dosage Reminders", remPanel);
        add(subTabs, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        executeAsync(() -> {
            List<Prescription> presList = prescriptionDao.findAll();
            List<Reminder> remList = reminderDao.findAll();
            return new Object[]{presList, remList};
        }, result -> {
            Object[] data = (Object[]) result;
            @SuppressWarnings("unchecked")
            List<Prescription> presList = (List<Prescription>) data[0];
            @SuppressWarnings("unchecked")
            List<Reminder> remList = (List<Reminder>) data[1];

            currentPrescriptions = presList;
            prescriptionModel.setRowCount(0);
            for (Prescription p : presList) {
                prescriptionModel.addRow(new Object[]{
                        p.getId(),
                        p.getPatientName(),
                        p.getDoctorName(),
                        p.getDiagnosis(),
                        p.getNotes(),
                        p.getMedicinesJson()
                });
            }

            currentReminders = remList;
            reminderModel.setRowCount(0);
            for (Reminder r : remList) {
                reminderModel.addRow(new Object[]{
                        r.getId(),
                        r.getMedicine(),
                        r.getTiming(),
                        r.getTime(),
                        r.getInstruction(),
                        r.getPatientEmail(),
                        r.isActive() ? "ACTIVE ✓" : "INACTIVE ✗"
                });
            }
        });
    }

    private void toggleReminderActive() {
        int row = reminderTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a reminder to toggle.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = (String) reminderModel.getValueAt(row, 0);
        String currentStatus = (String) reminderModel.getValueAt(row, 6);
        boolean newActive = !currentStatus.contains("ACTIVE");

        executeAsync(() -> reminderDao.toggleActive(id, newActive), success -> {
            JOptionPane.showMessageDialog(this, "Reminder status toggled in JDBC database.", "Updated", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        });
    }

    private void openAddReminderDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Schedule New Medicine Reminder", true);
        dialog.setSize(380, 320);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField medField = new JTextField("Amoxicillin 500mg");
        JTextField timingField = new JTextField("After Dinner");
        JTextField timeField = new JTextField("21:00");
        JTextField instrField = new JTextField("Take 1 capsule with warm water");
        JTextField emailField = new JTextField("patient@example.com");

        form.add(new JLabel("Medicine Name:"));
        form.add(medField);
        form.add(new JLabel("Timing:"));
        form.add(timingField);
        form.add(new JLabel("Time (HH:mm):"));
        form.add(timeField);
        form.add(new JLabel("Instruction:"));
        form.add(instrField);
        form.add(new JLabel("Patient Email:"));
        form.add(emailField);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = createPrimaryButton("Save to JDBC");
        saveBtn.addActionListener(e -> {
            Reminder r = new Reminder(
                    "rem-" + System.currentTimeMillis(),
                    medField.getText().trim(),
                    timingField.getText().trim(),
                    timeField.getText().trim(),
                    instrField.getText().trim(),
                    emailField.getText().trim(),
                    true
            );
            executeAsync(() -> reminderDao.save(r), saved -> {
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Reminder saved successfully into JDBC database!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            });
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    @Override
    public String getPanelTitle() {
        return "Prescriptions & Reminders";
    }
}
