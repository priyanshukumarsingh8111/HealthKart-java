package com.healthkart.gui;

import com.formdev.flatlaf.FlatLightLaf;
import com.healthkart.dao.*;
import com.healthkart.service.AppointmentService;
import com.healthkart.service.MedicineService;

import javax.swing.*;
import java.awt.*;

/**
 * Main Window for the HealthKart Java GUI Desktop Application.
 * Integrates comprehensive clinical management, pharmacy catalog,
 * appointment scheduling, and database inspection.
 */
public class HealthKartGUI extends JFrame {

    private final DoctorBookingPanel doctorPanel;
    private final MedicineStorePanel medicinePanel;
    private final AppointmentManagementPanel appointmentPanel;
    private final PrescriptionReminderPanel prescriptionPanel;
    private final DatabaseInspectorPanel dbInspectorPanel;

    public HealthKartGUI() {
        super("HealthKart - Healthcare & Pharmacy Desktop System (JDBC & Swing)");
        setupLookAndFeel();

        // Instantiate DAOs and Services
        DoctorDao doctorDao = new DoctorDaoImpl();
        MedicineDao medicineDao = new MedicineDaoImpl();
        AppointmentDao appointmentDao = new AppointmentDaoImpl();
        PrescriptionDao prescriptionDao = new PrescriptionDaoImpl();
        ReminderDao reminderDao = new ReminderDaoImpl();
        NotificationDao notificationDao = new NotificationDaoImpl();

        AppointmentService appointmentService = new AppointmentService(appointmentDao, doctorDao, notificationDao);
        MedicineService medicineService = new MedicineService(medicineDao);

        // Build Panels
        this.doctorPanel = new DoctorBookingPanel(doctorDao, appointmentService);
        this.medicinePanel = new MedicineStorePanel(medicineService, medicineDao);
        this.appointmentPanel = new AppointmentManagementPanel(appointmentService, appointmentDao);
        this.prescriptionPanel = new PrescriptionReminderPanel(prescriptionDao, reminderDao);
        this.dbInspectorPanel = new DatabaseInspectorPanel(appointmentDao, doctorDao, medicineDao);

        initGUI();
    }

    private void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
        } catch (Throwable t) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void initGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 58, 138)); // Deep Indigo/Blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titleLabel = new JLabel("<html><span style='font-size:18px; font-weight:bold; color:white;'>HealthKart Desktop System</span><br>" +
                "<span style='font-size:11px; color:#bfdbfe;'>Complete Java GUI with Pure JDBC Database &amp; Multithreaded Concurrency</span></html>");
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel systemBadge = new JLabel("<html><div style='background-color:#1e40af; border:1px solid #60a5fa; padding:4px 10px; border-radius:4px; color:white; font-size:10px; text-align:right;'>" +
                "<b>System Architecture:</b><br>" +
                "OOP Architecture • Collections &amp; Generics • Multithreaded Sync • JDBC DAOs" +
                "</div></html>");
        headerPanel.add(systemBadge, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane for Navigation
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("🩺 " + doctorPanel.getPanelTitle(), doctorPanel);
        tabbedPane.addTab("💊 " + medicinePanel.getPanelTitle(), medicinePanel);
        tabbedPane.addTab("📅 " + appointmentPanel.getPanelTitle(), appointmentPanel);
        tabbedPane.addTab("📋 " + prescriptionPanel.getPanelTitle(), prescriptionPanel);
        tabbedPane.addTab("⚙ " + dbInspectorPanel.getPanelTitle(), dbInspectorPanel);

        tabbedPane.addChangeListener(e -> {
            Component c = tabbedPane.getSelectedComponent();
            if (c instanceof BasePanel bp) {
                bp.refreshData();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        // Status Footer
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(241, 245, 249));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));
        JLabel footerLabel = new JLabel("Database Engine: SQLite JDBC (data/healthkart.db) | Multithreading Worker: Active");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(100, 116, 139));
        footerPanel.add(footerLabel, BorderLayout.WEST);

        add(footerPanel, BorderLayout.SOUTH);
    }

    /**
     * Standalone Launcher for Java GUI Desktop Application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HealthKartGUI gui = new HealthKartGUI();
            gui.setVisible(true);
        });
    }
}
