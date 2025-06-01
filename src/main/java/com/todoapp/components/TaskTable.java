package com.todoapp.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.LinkedHashSet;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Custom JTable for task management with enhanced features
 * Supports pinning, color coding, hover effects and keyboard shortcuts
 */
public class TaskTable extends JTable {

    // ==================== COMPONENTS ====================
    private DefaultTableModel tableModel;
    private int hoveredRow = -1;
    private LinkedHashSet<Integer> pinnedTaskRows;

    // ==================== INITIALIZATION ====================

    public TaskTable() {
        initializeTableModel();
        setupTableProperties();
        setupCellRenderers();
        initializePinnedTasks();
    }

    /**
     * Initialize read-only table model với custom behavior
     */
    private void initializeTableModel() {
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class; // All columns are String type
            }
        };

        // Add table columns
        tableModel.addColumn("Task");
        tableModel.addColumn("Due Date");
        tableModel.addColumn("Priority");
        tableModel.addColumn("Status");

        setModel(tableModel);
    }

    private void initializePinnedTasks() {
        pinnedTaskRows = new LinkedHashSet<>();
    }

    // ==================== TABLE CONFIGURATION ====================

    /**
     * Configure visual properties and layout
     */
    private void setupTableProperties() {
        // Table appearance
        setRowHeight(30);
        setShowGrid(true);
        setGridColor(new Color(230, 230, 230));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(true);
        setIntercellSpacing(new Dimension(5, 5));

        configureTableHeader();
        configureColumnWidths();
    }

    /**
     * Configure table header appearance
     */
    private void configureTableHeader() {
        JTableHeader header = getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setBackground(new Color(240, 240, 240));
        header.setReorderingAllowed(false); // Prevent column reordering
    }

    /**
     * Set preferred column widths
     */
    private void configureColumnWidths() {
        TableColumnModel columnModel = getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(300); // Task
        columnModel.getColumn(1).setPreferredWidth(100); // Due Date
        columnModel.getColumn(2).setPreferredWidth(80); // Priority
        columnModel.getColumn(3).setPreferredWidth(100); // Status
    }

    // ==================== CELL RENDERERS ====================

    /**
     * Setup custom cell renderers với color coding
     */
    private void setupCellRenderers() {
        DefaultTableCellRenderer defaultRenderer = createDefaultRenderer();

        // Apply default renderer to all columns
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
        }

        // Special renderer for Task column (with pin icons)
        setupTaskColumnRenderer(defaultRenderer);
    }

    /**
     * Create default cell renderer với hover và selection effects
     */
    private DefaultTableCellRenderer createDefaultRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                Component component = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);

                applyRowStateColors(component, isSelected, row);
                applyColumnColors(component, value, column, isSelected, row);
                configureAlignment(component, column);
                configureFont(component);

                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

                return component;
            }
        };
    }

    /**
     * Apply colors based on row state (selected/hover/normal)
     */
    private void applyRowStateColors(Component component, boolean isSelected, int row) {
        if (isSelected) {
            component.setBackground(new Color(173, 216, 230)); // Light blue for selection
            component.setForeground(Color.BLACK);
        } else if (row == hoveredRow) {
            component.setBackground(new Color(240, 248, 255)); // Lightest blue for hover
            component.setForeground(Color.BLACK);
        } else {
            // Alternating row colors
            component.setBackground(row % 2 == 0 ? new Color(250, 250, 250) : Color.WHITE);
            component.setForeground(Color.BLACK);
        }
    }

    /**
     * Apply special colors for Priority and Status columns
     */
    private void applyColumnColors(Component component, Object value, int column,
            boolean isSelected, int row) {
        // Skip color coding if row is selected or hovered
        if (isSelected || row == hoveredRow)
            return;

        if (column == 2 && value != null) { // Priority column
            switch (value.toString()) {
                case "High":
                    component.setBackground(new Color(255, 200, 200)); // Light red
                    break;
                case "Medium":
                    component.setBackground(new Color(255, 235, 200)); // Light orange
                    break;
                case "Low":
                    component.setBackground(new Color(220, 255, 220)); // Light green
                    break;
            }
        } else if (column == 3 && value != null) { // Status column
            switch (value.toString()) {
                case "Completed":
                    component.setBackground(new Color(200, 230, 255)); // Sky blue
                    break;
                case "In Progress":
                    component.setBackground(new Color(230, 220, 255)); // Light purple
                    break;
                case "Pending":
                    component.setBackground(new Color(240, 240, 240)); // Light gray
                    break;
            }
        }
    }

    /**
     * Configure text alignment for different columns
     */
    private void configureAlignment(Component component, int column) {
        if (column == 2 || column == 3) { // Priority and Status columns
            ((JLabel) component).setHorizontalAlignment(SwingConstants.CENTER);
        } else {
            ((JLabel) component).setHorizontalAlignment(SwingConstants.LEFT);
        }
    }

    private void configureFont(Component component) {
        component.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    /**
     * Setup special renderer for Task column với pin icons
     */
    private void setupTaskColumnRenderer(DefaultTableCellRenderer defaultRenderer) {
        getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                // Show pin icon for pinned tasks
                int modelRow = table.convertRowIndexToModel(row);
                if (pinnedTaskRows.contains(modelRow)) {
                    label.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
                    label.setText("📌 " + value);
                    label.setIconTextGap(5);
                } else {
                    label.setIcon(null);
                }

                return label;
            }
        });
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Add mouse và keyboard listeners
     */
    public void addTableMouseListeners() {
        addMouseListener(createMouseClickListener());
        addMouseMotionListener(createMouseMotionListener());
        addKeyListener(createKeyboardListener());
    }

    /**
     * Create mouse click listener cho selection và context menu
     */
    private MouseAdapter createMouseClickListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());

                if (SwingUtilities.isRightMouseButton(e) && row >= 0) {
                    setRowSelectionInterval(row, row);
                    // Context menu handled in MainWindow
                } else if (row >= 0) {
                    if (isRowSelected(row)) {
                        clearSelection();
                    } else {
                        setRowSelectionInterval(row, row);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                repaint();
            }
        };
    }

    /**
     * Create mouse motion listener cho hover effects
     */
    private MouseMotionAdapter createMouseMotionListener() {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());

                if (row != hoveredRow) {
                    hoveredRow = row;
                    repaint();
                }
            }
        };
    }

    /**
     * Create keyboard listener cho shortcuts
     */
    private KeyAdapter createKeyboardListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int selectedRow = getSelectedRow();

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DELETE:
                        if (selectedRow >= 0) {
                            firePropertyChange("deleteTask", -1, selectedRow);
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                        if (selectedRow >= 0) {
                            firePropertyChange("editTask", -1, selectedRow);
                        }
                        break;
                    case KeyEvent.VK_P:
                        if (selectedRow >= 0 && (e.isControlDown() || e.isMetaDown())) {
                            int modelRow = convertRowIndexToModel(selectedRow);
                            firePropertyChange("togglePin", -1, modelRow);
                        }
                        break;
                }
            }
        };
    }

    // ==================== PIN MANAGEMENT ====================

    /**
     * Update pinned tasks display
     */
    public void updatePinnedTasks(LinkedHashSet<Integer> pinnedRows) {
        this.pinnedTaskRows = pinnedRows;
        repaint();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get currently selected task name
     */
    public String getSelectedTask() {
        int selectedRow = getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = convertRowIndexToModel(selectedRow);
            return (String) tableModel.getValueAt(modelRow, 0);
        }
        return null;
    }

    /**
     * Filter tasks based on search text
     */
    public void filterTasks(String text) {
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) getRowSorter();
        if (text == null || text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    // ==================== GETTERS ====================

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public int getHoveredRow() {
        return hoveredRow;
    }

    public void setHoveredRow(int row) {
        this.hoveredRow = row;
    }
}