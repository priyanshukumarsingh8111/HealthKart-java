package com.healthkart.gui;

import com.healthkart.dao.DoctorDao;
import com.healthkart.dao.DoctorDaoImpl;
import com.healthkart.exception.SlotConflictException;
import com.healthkart.exception.ValidationException;
import com.healthkart.model.Appointment;
import com.healthkart.model.Doctor;
import com.healthkart.service.AppointmentService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Doctor Consultation and Interactive Slot Booking Panel.
 * Demonstrates:
 * - GUI Components: JComboBox, JList, JButton, JDialog, CardLayout / FlowLayout
 * - Multithreading: Asynchronous slot queries and booking via SwingWorker
 * - OOP: Inheritance (BasePanel), Polymorphism, Exception Handling (SlotConflictException)
 * - Collections & Generics: List<Doctor>, Set<String>, Map<String, Object>
 */
public class DoctorBookingPanel extends BasePanel {

    private final DoctorDao doctorDao;
    private final AppointmentService appointmentService;

    private JComboBox<String> specialtyCombo;
    private DefaultListModel<Doctor> doctorListModel;
    private JList<Doctor> doctorJList;
    private JComboBox<String> dateCombo;
    private JPanel slotsGridPanel;
    private JLabel doctorDetailsLabel;
    private JButton bookButton;

    private Doctor selectedDoctor;
    private String selectedTimeSlot;

    public DoctorBookingPanel(DoctorDao doctorDao, AppointmentService appointmentService) {
        this.doctorDao = doctorDao;
        this.appointmentService = appointmentService;
        initUI();
        refreshData();
    }

    private void initUI() {
        // Top Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(createHeaderLabel("Doctor Consultation & Live Slot Booking",
                "Select a specialized doctor, choose an available date, and book an appointment with real-time concurrency control"), BorderLayout.WEST);

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshBtn.addActionListener(e -> refreshData());
        topPanel.add(refreshBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center Split: Left = Doctor Selection, Right = Slot Booking
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.4);
        splitPane.setOpaque(false);

        // --- Left Panel: Doctor Directory ---
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "1. Select Doctor", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Specialty:"));
        specialtyCombo = new JComboBox<>(new String[]{"All Specialties"});
        specialtyCombo.addActionListener(e -> filterDoctors());
        filterPanel.add(specialtyCombo);
        leftPanel.add(filterPanel, BorderLayout.NORTH);

        doctorListModel = new DefaultListModel<>();
        doctorJList = new JList<>(doctorListModel);
        doctorJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        doctorJList.setCellRenderer(new DoctorListCellRenderer());
        doctorJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedDoctor = doctorJList.getSelectedValue();
                updateDoctorDetails();
                loadSlotsForSelectedDate();
            }
        });
        leftPanel.add(new JScrollPane(doctorJList), BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // --- Right Panel: Slot Calendar & Details ---
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "2. Select Date & Slot", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        datePanel.add(new JLabel("Consultation Date:"));
        dateCombo = new JComboBox<>();
        populateDateOptions();
        dateCombo.addActionListener(e -> loadSlotsForSelectedDate());
        datePanel.add(dateCombo);

        doctorDetailsLabel = new JLabel("<html><i>Please select a doctor from the list to view profile and available slots.</i></html>");
        doctorDetailsLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel rightHeader = new JPanel(new BorderLayout());
        rightHeader.add(datePanel, BorderLayout.NORTH);
        rightHeader.add(doctorDetailsLabel, BorderLayout.SOUTH);
        rightPanel.add(rightHeader, BorderLayout.NORTH);

        // Slots grid in center
        slotsGridPanel = new JPanel(new GridLayout(0, 4, 10, 10));
        slotsGridPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JScrollPane slotsScroll = new JScrollPane(slotsGridPanel);
        slotsScroll.setBorder(BorderFactory.createTitledBorder("Available Time Slots (Green = Available, Red = Booked)"));
        rightPanel.add(slotsScroll, BorderLayout.CENTER);

        // Action Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bookButton = createPrimaryButton("Book Selected Appointment");
        bookButton.setEnabled(false);
        bookButton.addActionListener(e -> openBookingDialog());
        actionPanel.add(bookButton);
        rightPanel.add(actionPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);
    }

    private void populateDateOptions() {
        dateCombo.removeAllItems();
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 14; i++) {
            LocalDate d = now.plusDays(i);
            String dayName = d.getDayOfWeek().name();
            String item = d.format(fmt) + (d.getDayOfWeek().getValue() == 7 ? " (Sunday - Closed)" : " (" + dayName.substring(0, 3) + ")");
            dateCombo.addItem(item);
        }
    }

    private String getSelectedDateString() {
        String item = (String) dateCombo.getSelectedItem();
        if (item == null) return LocalDate.now().toString();
        return item.split(" ")[0];
    }

    @Override
    public void refreshData() {
        executeAsync(() -> {
            List<Doctor> docs = doctorDao.findAll();
            Set<String> specs = doctorDao.getAllSpecializations();
            return new Object[]{docs, specs};
        }, result -> {
            Object[] data = (Object[]) result;
            @SuppressWarnings("unchecked")
            List<Doctor> docs = (List<Doctor>) data[0];
            @SuppressWarnings("unchecked")
            Set<String> specs = (Set<String>) data[1];

            specialtyCombo.removeAllItems();
            specialtyCombo.addItem("All Specialties");
            for (String s : specs) {
                specialtyCombo.addItem(s);
            }

            doctorListModel.clear();
            for (Doctor d : docs) {
                doctorListModel.addElement(d);
            }
            if (!docs.isEmpty()) {
                doctorJList.setSelectedIndex(0);
            }
        });
    }

    private void filterDoctors() {
        String spec = (String) specialtyCombo.getSelectedItem();
        executeAsync(() -> {
            if (spec == null || "All Specialties".equals(spec)) {
                return doctorDao.findAll();
            }
            return doctorDao.findBySpecialization(spec);
        }, docs -> {
            doctorListModel.clear();
            for (Doctor d : docs) {
                doctorListModel.addElement(d);
            }
            if (!docs.isEmpty()) {
                doctorJList.setSelectedIndex(0);
            }
        });
    }

    private void updateDoctorDetails() {
        if (selectedDoctor == null) {
            doctorDetailsLabel.setText("<html><i>Please select a doctor.</i></html>");
            return;
        }
        doctorDetailsLabel.setText("<html><div style='background-color:#e0f2fe; padding:8px; border-radius:4px;'>"
                + "<b>" + selectedDoctor.getName() + "</b> — " + selectedDoctor.getSpecialization() + "<br>"
                + "Hospital: " + selectedDoctor.getClinicHospital() + " | Experience: " + selectedDoctor.getExperience() + " yrs<br>"
                + "Consultation Fee: <b>₹" + selectedDoctor.getConsultationFee() + "</b> | Rating: " + selectedDoctor.getRating() + " ★"
                + "</div></html>");
    }

    private void loadSlotsForSelectedDate() {
        if (selectedDoctor == null) return;
        String date = getSelectedDateString();
        selectedTimeSlot = null;
        bookButton.setEnabled(false);

        if (AppointmentService.isSunday(date)) {
            slotsGridPanel.removeAll();
            JLabel closedLbl = new JLabel("<html><font color='#dc2626'><b>Sundays are closed for clinical consultations. Please select another day.</b></font></html>", SwingConstants.CENTER);
            slotsGridPanel.setLayout(new BorderLayout());
            slotsGridPanel.add(closedLbl, BorderLayout.CENTER);
            slotsGridPanel.revalidate();
            slotsGridPanel.repaint();
            return;
        }

        slotsGridPanel.removeAll();
        slotsGridPanel.setLayout(new GridLayout(0, 4, 10, 10));

        executeAsync(() -> appointmentService.getSlots(selectedDoctor.getId(), date), slotList -> {
            slotsGridPanel.removeAll();
            ButtonGroup group = new ButtonGroup();

            for (Map<String, Object> s : slotList) {
                String time = String.valueOf(s.get("time"));
                boolean isBooked = Boolean.TRUE.equals(s.get("isBooked"));

                JToggleButton slotBtn = new JToggleButton(time);
                slotBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                slotBtn.setFocusPainted(false);

                if (isBooked) {
                    slotBtn.setEnabled(false);
                    slotBtn.setBackground(new Color(254, 226, 226));
                    slotBtn.setForeground(new Color(220, 38, 38));
                    slotBtn.setText(time + " (Booked)");
                } else {
                    slotBtn.setBackground(new Color(220, 252, 231));
                    slotBtn.setForeground(new Color(22, 101, 52));
                    slotBtn.addActionListener(e -> {
                        selectedTimeSlot = time;
                        bookButton.setEnabled(true);
                        bookButton.setText("Book Consultation at " + time);
                    });
                    group.add(slotBtn);
                }
                slotsGridPanel.add(slotBtn);
            }
            slotsGridPanel.revalidate();
            slotsGridPanel.repaint();
        });
    }

    private void openBookingDialog() {
        if (selectedDoctor == null || selectedTimeSlot == null) {
            JOptionPane.showMessageDialog(this, "Please select both a doctor and an available time slot.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Confirm Appointment Booking", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(440, 380);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField nameField = new JTextField("Rahul Sharma");
        JTextField emailField = new JTextField("patient@example.com");
        JTextField phoneField = new JTextField("9876543210");
        JTextField symptomsField = new JTextField("General routine checkup");

        formPanel.add(new JLabel("Doctor:"));
        formPanel.add(new JLabel("<b>" + selectedDoctor.getName() + "</b>"));
        formPanel.add(new JLabel("Date & Slot:"));
        formPanel.add(new JLabel(getSelectedDateString() + " @ " + selectedTimeSlot));
        formPanel.add(new JLabel("Consultation Fee:"));
        formPanel.add(new JLabel("₹" + selectedDoctor.getConsultationFee()));

        formPanel.add(new JLabel("Patient Full Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Patient Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Mobile Phone:"));
        formPanel.add(phoneField);
        formPanel.add(new JLabel("Symptoms / Concern:"));
        formPanel.add(symptomsField);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton confirmBtn = createPrimaryButton("Confirm & Save to JDBC");
        confirmBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String symptoms = symptomsField.getText().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all required patient fields.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Appointment apt = new Appointment();
            apt.setAppointmentId("HK-" + ((int) (Math.random() * 900000 + 100000)));
            apt.setDoctorId(selectedDoctor.getId());
            apt.setDoctorName(selectedDoctor.getName());
            apt.setPatientName(name);
            apt.setPatientEmail(email);
            apt.setPatientPhone(phone);
            apt.setDate(getSelectedDateString());
            apt.setTime(selectedTimeSlot);
            apt.setSymptoms(symptoms);
            apt.setFee(selectedDoctor.getConsultationFee());

            confirmBtn.setEnabled(false);
            confirmBtn.setText("Booking (JDBC)...");

            // Execute on multithreaded worker
            executeAsync(() -> appointmentService.bookAppointment(apt), savedApt -> {
                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                        "Appointment Confirmed!\n" +
                        "Appointment ID: " + savedApt.getAppointmentId() + "\n" +
                        "Doctor: " + savedApt.getDoctorName() + "\n" +
                        "Slot: " + savedApt.getDate() + " at " + savedApt.getTime() + "\n\n" +
                        "Stored successfully in JDBC SQLite database.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                loadSlotsForSelectedDate();
            });
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(confirmBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    @Override
    public String getPanelTitle() {
        return "Doctor Consultation";
    }

    private static class DoctorListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Doctor doc) {
                label.setText("<html><div style='padding:4px;'>"
                        + "<b>" + doc.getName() + "</b> (" + doc.getSpecialization() + ")<br>"
                        + "<span style='color:#6b7280; font-size:10px;'>" + doc.getClinicHospital() + " • ₹" + doc.getConsultationFee() + " • " + doc.getRating() + "★</span>"
                        + "</div></html>");
                label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            }
            return label;
        }
    }
}
