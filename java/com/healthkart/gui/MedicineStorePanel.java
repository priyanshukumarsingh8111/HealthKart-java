package com.healthkart.gui;

import com.healthkart.dao.MedicineDao;
import com.healthkart.exception.ValidationException;
import com.healthkart.model.Medicine;
import com.healthkart.service.MedicineService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Medicine Pharmacy and Catalog Panel.
 * Demonstrates:
 * - GUI Components: JTable, JScrollPane, JComboBox, JTextField, JSplitPane, JDialog
 * - Collections & Generics: List<Medicine>, Set<String>, Map<Medicine, Integer> for Cart
 * - Multithreading: Asynchronous inventory query and checkout via SwingWorker
 * - Database Operations: Prepared statements, stock reduction transactions
 */
public class MedicineStorePanel extends BasePanel {

    private final MedicineService medicineService;
    private final MedicineDao medicineDao;

    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> sortCombo;
    private JTable medicineTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel medicineDetailsLabel;

    private List<Medicine> currentMedicines = new ArrayList<>();
    // Shopping cart using Collections Map
    private final Map<Medicine, Integer> shoppingCart = new LinkedHashMap<>();
    private JLabel cartSummaryLabel;

    public MedicineStorePanel(MedicineService medicineService, MedicineDao medicineDao) {
        this.medicineService = medicineService;
        this.medicineDao = medicineDao;
        initUI();
        refreshData();
    }

    private void initUI() {
        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(createHeaderLabel("HealthKart Pharmacy & Medicine Catalog",
                "Browse authentic medicines, filter by category, sort by price or rating, and order directly with live stock updates"), BorderLayout.WEST);

        JPanel cartTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cartSummaryLabel = new JLabel("Cart: 0 items (₹0.00)");
        cartSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JButton viewCartBtn = new JButton("🛒 View Cart");
        viewCartBtn.addActionListener(e -> openCartDialog());
        cartTopPanel.add(cartSummaryLabel);
        cartTopPanel.add(viewCartBtn);
        topPanel.add(cartTopPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Filter / Search Toolbar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setBorder(BorderFactory.createTitledBorder("Search & Filter Medicines"));

        filterBar.add(new JLabel("Search:"));
        searchField = new JTextField(15);
        searchField.addActionListener(e -> applyFilter());
        filterBar.add(searchField);

        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> applyFilter());
        filterBar.add(searchBtn);

        filterBar.add(new JLabel("Category:"));
        categoryCombo = new JComboBox<>(new String[]{"All Categories"});
        categoryCombo.addActionListener(e -> applyFilter());
        filterBar.add(categoryCombo);

        filterBar.add(new JLabel("Sort By:"));
        sortCombo = new JComboBox<>(new String[]{"Name (A-Z)", "Price (Low to High)", "Price (High to Low)", "Top Rated"});
        sortCombo.addActionListener(e -> applyFilter());
        filterBar.add(sortCombo);

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryCombo.setSelectedIndex(0);
            sortCombo.setSelectedIndex(0);
            applyFilter();
        });
        filterBar.add(resetBtn);

        // Center split: Table on Left, Details & Add to Cart on Right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // Table setup
        String[] columnNames = {"ID", "Medicine Name", "Category", "Price (₹)", "Rating", "Stock", "Manufacturer"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        medicineTable = new JTable(tableModel);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medicineTable.setRowHeight(24);
        medicineTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = medicineTable.getSelectedRow();
                if (row >= 0 && row < currentMedicines.size()) {
                    showMedicineDetails(currentMedicines.get(row));
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(medicineTable);
        splitPane.setLeftComponent(scrollPane);

        // Right details panel
        JPanel detailsPanel = new JPanel(new BorderLayout(10, 10));
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Medicine Information"));
        medicineDetailsLabel = new JLabel("<html><i>Select a medicine to view details and add to cart.</i></html>");
        medicineDetailsLabel.setVerticalAlignment(SwingConstants.TOP);
        medicineDetailsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        detailsPanel.add(medicineDetailsLabel, BorderLayout.CENTER);

        JPanel detailsAction = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addToCartBtn = createPrimaryButton("+ Add to Cart");
        addToCartBtn.addActionListener(e -> {
            int row = medicineTable.getSelectedRow();
            if (row >= 0 && row < currentMedicines.size()) {
                addToCart(currentMedicines.get(row));
            } else {
                JOptionPane.showMessageDialog(this, "Please select a medicine first.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        detailsAction.add(addToCartBtn);
        detailsPanel.add(detailsAction, BorderLayout.SOUTH);
        splitPane.setRightComponent(detailsPanel);

        // Combine filter bar and table in center container
        JPanel centerContainer = new JPanel(new BorderLayout(5, 5));
        centerContainer.add(filterBar, BorderLayout.NORTH);
        centerContainer.add(splitPane, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Ready. Loaded medicines from JDBC.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void applyFilter() {
        String search = searchField.getText().trim();
        String cat = (String) categoryCombo.getSelectedItem();
        String sortOption = (String) sortCombo.getSelectedItem();

        String sortBy = "name";
        if ("Price (Low to High)".equals(sortOption)) sortBy = "price-low";
        else if ("Price (High to Low)".equals(sortOption)) sortBy = "price-high";
        else if ("Top Rated".equals(sortOption)) sortBy = "rating";

        final String finalCategory = ("All Categories".equals(cat)) ? null : cat;
        final String finalSort = sortBy;

        statusLabel.setText("Querying JDBC database...");
        executeAsync(() -> medicineService.getMedicines(search, finalCategory, finalSort), list -> {
            currentMedicines = list;
            tableModel.setRowCount(0);
            for (Medicine m : list) {
                tableModel.addRow(new Object[]{
                        m.getId(),
                        m.getName(),
                        m.getCategory(),
                        String.format("%.2f", m.getPrice()),
                        m.getRating() + " ★",
                        m.getStockQuantity(),
                        m.getManufacturer()
                });
            }
            statusLabel.setText("Found " + list.size() + " medicines matching criteria.");
        });
    }

    @Override
    public void refreshData() {
        executeAsync(() -> {
            Set<String> categories = medicineService.getCategories();
            List<Medicine> all = medicineService.getMedicines(null, null, "name");
            return new Object[]{categories, all};
        }, res -> {
            Object[] data = (Object[]) res;
            @SuppressWarnings("unchecked")
            Set<String> categories = (Set<String>) data[0];
            @SuppressWarnings("unchecked")
            List<Medicine> all = (List<Medicine>) data[1];

            categoryCombo.removeAllItems();
            categoryCombo.addItem("All Categories");
            for (String c : categories) {
                categoryCombo.addItem(c);
            }

            currentMedicines = all;
            tableModel.setRowCount(0);
            for (Medicine m : all) {
                tableModel.addRow(new Object[]{
                        m.getId(),
                        m.getName(),
                        m.getCategory(),
                        String.format("%.2f", m.getPrice()),
                        m.getRating() + " ★",
                        m.getStockQuantity(),
                        m.getManufacturer()
                });
            }
            if (!all.isEmpty()) {
                medicineTable.setRowSelectionInterval(0, 0);
            }
        });
    }

    private void showMedicineDetails(Medicine m) {
        medicineDetailsLabel.setText("<html><div style='width:240px; padding:6px; font-family:Segoe UI;'>"
                + "<h3 style='margin:0 0 5px 0; color:#1e3a8a;'>" + m.getName() + "</h3>"
                + "<p><b>Category:</b> " + m.getCategory() + "<br>"
                + "<b>Price:</b> ₹" + String.format("%.2f", m.getPrice()) + "<br>"
                + "<b>Rating:</b> " + m.getRating() + " ★<br>"
                + "<b>In Stock:</b> " + m.getStockQuantity() + " units<br>"
                + "<b>Manufacturer:</b> " + m.getManufacturer() + "</p>"
                + "<hr>"
                + "<p style='color:#475569;'><b>Description:</b><br>" + m.getDescription() + "</p>"
                + "</div></html>");
    }

    private void addToCart(Medicine m) {
        if (m.getStockQuantity() <= 0) {
            JOptionPane.showMessageDialog(this, "Sorry, this medicine is currently out of stock.", "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }
        shoppingCart.put(m, shoppingCart.getOrDefault(m, 0) + 1);
        updateCartSummary();
        JOptionPane.showMessageDialog(this, "Added 1x " + m.getName() + " to cart!", "Cart Updated", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateCartSummary() {
        int count = 0;
        double total = 0.0;
        for (Map.Entry<Medicine, Integer> e : shoppingCart.entrySet()) {
            count += e.getValue();
            total += e.getKey().getPrice() * e.getValue();
        }
        cartSummaryLabel.setText(String.format("Cart: %d items (₹%.2f)", count, total));
    }

    private void openCartDialog() {
        if (shoppingCart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty. Add some medicines first!", "Cart Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Shopping Cart & Checkout", true);
        dialog.setSize(480, 400);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        String[] cols = {"Medicine", "Unit Price", "Qty", "Subtotal"};
        DefaultTableModel cartModel = new DefaultTableModel(cols, 0);
        double total = 0.0;
        for (Map.Entry<Medicine, Integer> e : shoppingCart.entrySet()) {
            Medicine med = e.getKey();
            int qty = e.getValue();
            double sub = med.getPrice() * qty;
            total += sub;
            cartModel.addRow(new Object[]{med.getName(), "₹" + med.getPrice(), qty, String.format("₹%.2f", sub)});
        }

        JTable cartTable = new JTable(cartModel);
        dialog.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel totalLabel = new JLabel(String.format(" Total Amount: ₹%.2f ", total));
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bottomPanel.add(totalLabel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearBtn = new JButton("Clear Cart");
        clearBtn.addActionListener(e -> {
            shoppingCart.clear();
            updateCartSummary();
            dialog.dispose();
        });

        final double finalTotal = total;
        JButton checkoutBtn = createPrimaryButton("Confirm Purchase & Deduct Stock (JDBC)");
        checkoutBtn.addActionListener(e -> {
            checkoutBtn.setEnabled(false);
            executeAsync(() -> {
                // Update stock in JDBC database
                for (Map.Entry<Medicine, Integer> entry : shoppingCart.entrySet()) {
                    medicineService.purchaseMedicine(entry.getKey().getId(), entry.getValue());
                }
                return true;
            }, success -> {
                shoppingCart.clear();
                updateCartSummary();
                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                        String.format("Order Placed Successfully!\nTotal Paid: ₹%.2f\nInventory stock updated via JDBC transaction.", finalTotal),
                        "Order Confirmed", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            });
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(clearBtn);
        buttons.add(checkoutBtn);
        bottomPanel.add(buttons, BorderLayout.EAST);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    @Override
    public String getPanelTitle() {
        return "Pharmacy Catalog";
    }
}
