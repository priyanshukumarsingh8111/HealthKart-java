package com.healthkart.gui;

import com.healthkart.dao.AppointmentDao;
import com.healthkart.model.Appointment;
import com.healthkart.service.AppointmentService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Appointment Management and Triage Priority Queue Panel.
 * Demonstrates:
 * - GUI Components: JTable, JToolBar, JComboBox, JOptionPane
 * - Collections & Generics: PriorityQueue<Appointment> for triage scheduling, List<Appointment>, Comparators
 * - Multithreading & Synchronization: Thread-safe cancellation and slot liberation via SwingWorker
 * - Database Operations (JDBC): Queries, status updates, transaction rollback on error
 */
public class AppointmentManagementPanel extends BasePanel {

    private final AppointmentService appointmentService;
    private final AppointmentDao appointmentDao;

    private JTable appointmentTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilterCombo;
    private JTextField searchPatientField;
    private JLabel countLabel;

    private List<Appointment> currentList = new ArrayList<>();

    public AppointmentManagementPanel(AppointmentService appointmentService, AppointmentDao appointmentDao) {
        this.appointmentService = appointmentService;
        this.appointmentDao = appointmentDao;
        initUI();
        refreshData();
    }

    private void initUI() {
        // Top Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(createHeaderLabel("Appointment Management & Live Triage",
                "Monitor scheduled consultations, manage appointment statuses, and inspect chronological priority triage"), BorderLayout.WEST);

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        topPanel.add(refreshBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Filter toolbar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterBar.setBorder(BorderFactory.createTitledBorder("Filters"));

        filterBar.add(new JLabel("Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"ALL", "CONFIRMED", "COMPLETED", "CANCELLED"});
        statusFilterCombo.addActionListener(e -> applyFilter());
        filterBar.add(statusFilterCombo);

        filterBar.add(new JLabel("Patient Search:"));
        searchPatientField = new JTextField(15);
        searchPatientField.addActionListener(e -> applyFilter());
        filterBar.add(searchPatientField);

        JButton searchBtn = new JButton("Filter");
        searchBtn.addActionListener(e -> applyFilter());
        filterBar.add(searchBtn);

        JButton triageQueueBtn = new JButton("⚡ View Upcoming Triage Queue (PriorityQueue)");
        triageQueueBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        triageQueueBtn.setBackground(new Color(243, 232, 255));
        triageQueueBtn.setForeground(new Color(107, 33, 168));
        triageQueueBtn.addActionListener(e -> showTriageQueueDialog());
        filterBar.add(triageQueueBtn);

        // Center Table
        String[] columnNames = {"Appointment ID", "Doctor", "Patient Name", "Email", "Date", "Time", "Fee (₹)", "Status", "Symptoms"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        appointmentTable = new JTable(tableModel);
        appointmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appointmentTable.setRowHeight(26);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(filterBar, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(appointmentTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Actions Toolbar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        countLabel = new JLabel("Total Appointments: 0");
        bottomPanel.add(countLabel, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel Selected Appointment");
        cancelBtn.setBackground(new Color(254, 226, 226));
        cancelBtn.setForeground(new Color(185, 28, 28));
        cancelBtn.addActionListener(e -> cancelSelectedAppointment());

        JButton completeBtn = new JButton("Mark as COMPLETED");
        completeBtn.setBackground(new Color(220, 252, 231));
        completeBtn.setForeground(new Color(21, 128, 61));
        completeBtn.addActionListener(e -> completeSelectedAppointment());

        actionBtns.add(cancelBtn);
        actionBtns.add(completeBtn);
        bottomPanel.add(actionBtns, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void refreshData() {
        executeAsync(appointmentService::getAllAppointments, list -> {
            currentList = list;
            applyFilter();
        });
    }

    private void applyFilter() {
        String status = (String) statusFilterCombo.getSelectedItem();
        String query = searchPatientField.getText().trim().toLowerCase();

        tableModel.setRowCount(0);
        int count = 0;
        for (Appointment a : currentList) {
            boolean matchStatus = "ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(a.getStatus());
            boolean matchQuery = query.isEmpty() ||
                    (a.getPatientName() != null && a.getPatientName().toLowerCase().contains(query)) ||
                    (a.getPatientEmail() != null && a.getPatientEmail().toLowerCase().contains(query)) ||
                    (a.getDoctorName() != null && a.getDoctorName().toLowerCase().contains(query));

            if (matchStatus && matchQuery) {
                tableModel.addRow(new Object[]{
                        a.getAppointmentId(),
                        a.getDoctorName(),
                        a.getPatientName(),
                        a.getPatientEmail(),
                        a.getDate(),
                        a.getTime(),
                        String.format("%.2f", a.getFee()),
                        a.getStatus(),
                        a.getSymptoms()
                });
                count++;
            }
        }
        countLabel.setText("Showing " + count + " of " + currentList.size() + " appointments from JDBC.");
    }

    private void cancelSelectedAppointment() {
        int row = appointmentTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an appointment from the table to cancel.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String aptId = (String) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel appointment '" + aptId + "'?\nThis will liberate the doctor's time slot in the JDBC database.",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            executeAsync(() -> appointmentService.cancelAppointment(aptId), success -> {
                if (success) {
                    JOptionPane.showMessageDialog(this, "Appointment cancelled and slot liberated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not cancel appointment.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private void completeSelectedAppointment() {
        int row = appointmentTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an appointment to update.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String aptId = (String) tableModel.getValueAt(row, 0);
        executeAsync(() -> {
            Appointment apt = appointmentService.getAppointmentById(aptId).orElse(null);
            if (apt != null) {
                apt.setStatus("COMPLETED");
                appointmentDao.update(apt);
                return true;
            }
            return false;
        }, success -> {
            JOptionPane.showMessageDialog(this, "Appointment marked as COMPLETED in JDBC database.", "Status Updated", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        });
    }

    /**
     * Demonstrates Collections PriorityQueue & Generics.
     * Displays chronological triage ordering of upcoming appointments.
     */
    private void showTriageQueueDialog() {
        executeAsync(appointmentService::getUpcomingTriageQueue, queue -> {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Upcoming Triage (Java PriorityQueue)", true);
            dialog.setSize(520, 380);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setLocationRelativeTo(this);

            String[] cols = {"Priority Order", "Date & Time", "Doctor", "Patient", "Symptoms"};
            DefaultTableModel triageModel = new DefaultTableModel(cols, 0);

            // Drain priority queue in chronological priority order
            PriorityQueue<Appointment> copy = new PriorityQueue<>(queue);
            int rank = 1;
            while (!copy.isEmpty()) {
                Appointment a = copy.poll();
                triageModel.addRow(new Object[]{
                        "#" + rank++,
                        a.getDate() + " " + a.getTime(),
                        a.getDoctorName(),
                        a.getPatientName(),
                        a.getSymptoms()
                });
            }

            JTable table = new JTable(triageModel);
            dialog.add(new JScrollPane(table), BorderLayout.CENTER);

            JLabel note = new JLabel("<html><div style='padding:8px; color:#4338ca;'><b>PriorityQueue&lt;Appointment&gt;</b> auto-orders active consultations chronologically by date and time.</div></html>");
            dialog.add(note, BorderLayout.NORTH);

            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dialog.dispose());
            JPanel btnP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnP.add(closeBtn);
            dialog.add(btnP, BorderLayout.SOUTH);

            dialog.setVisible(true);
        });
    }

    @Override
    public String getPanelTitle() {
        return "Appointments";
    }
}
