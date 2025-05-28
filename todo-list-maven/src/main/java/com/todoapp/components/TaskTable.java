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

public class TaskTable extends JTable {
    private DefaultTableModel tableModel;
    private int hoveredRow = -1;
    private LinkedHashSet<Integer> pinnedTaskRows;
    
    public TaskTable() {
        // Khởi tạo model
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Đảm bảo bảng chỉ đọc
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class; // Tất cả các cột là String
            }
        };
        
        // Thêm các cột cho bảng
        tableModel.addColumn("Task");
        tableModel.addColumn("Due Date");
        tableModel.addColumn("Priority");
        tableModel.addColumn("Status");
        
        setModel(tableModel);
        
        // Thiết lập thuộc tính hiển thị
        setupTableProperties();
        
        // Thêm các cell renderer
        setupCellRenderers();
        
        // Tạo LinkedHashSet để lưu trạng thái pin
        pinnedTaskRows = new LinkedHashSet<>();
    }
    
    private void setupTableProperties() {
        // Thiết lập thuộc tính UI cho bảng
        setRowHeight(30);
        setShowGrid(true);
        setGridColor(new Color(230, 230, 230));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(true);
        setIntercellSpacing(new Dimension(5, 5));
        
        // Thiết lập thuộc tính UI cho header
        JTableHeader header = getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setBackground(new Color(240, 240, 240));
        header.setReorderingAllowed(false); // Không cho phép kéo thả cột
        
        // Thiết lập độ rộng cột
        TableColumnModel columnModel = getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(300); // Task
        columnModel.getColumn(1).setPreferredWidth(100); // Due Date
        columnModel.getColumn(2).setPreferredWidth(80);  // Priority
        columnModel.getColumn(3).setPreferredWidth(100); // Status
    }
    
    private void setupCellRenderers() {
        // Renderer mặc định cho tất cả các cột
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                Component component = super.getTableCellRendererComponent(table, value, 
                        isSelected, hasFocus, row, column);
                
                // Thiết lập màu nền dựa trên trạng thái select hoặc hover
                if (isSelected) {
                    component.setBackground(new Color(173, 216, 230)); // Light blue for selection
                    component.setForeground(Color.BLACK);
                } else if (row == hoveredRow) {
                    component.setBackground(new Color(240, 248, 255)); // Lightest blue for hover
                    component.setForeground(Color.BLACK);
                } else {
                    // Màu mặc định cho background
                    component.setBackground(row % 2 == 0 ? 
                                 new Color(250, 250, 250) : Color.WHITE);
                    component.setForeground(Color.BLACK);
                    
                    // Tô màu đặc biệt cho các cột Priority và Status
                    if (column == 2) { // Priority column
                        String priority = value != null ? value.toString() : "";
                        switch (priority) {
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
                    } else if (column == 3) { // Status column
                        String status = value != null ? value.toString() : "";
                        switch (status) {
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
                
                // Căn giữa text cho các cột Priority và Status
                if (column == 2 || column == 3) {
                    ((JLabel) component).setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    ((JLabel) component).setHorizontalAlignment(SwingConstants.LEFT);
                }
                
                // Thiết lập font cho cột
                component.setFont(new Font("SansSerif", Font.PLAIN, 12));
                
                // Thêm padding
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                
                return component;
            }
        };
        
        // Đặt renderer mặc định cho tất cả các cột
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
        }
        
        // Renderer đặc biệt cho cột Task để hiển thị icon pin
        getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                
                // Hiển thị icon pin cho những task được ghim
                int modelRow = table.convertRowIndexToModel(row);
                if (pinnedTaskRows.contains(modelRow)) {
                    label.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
                    label.setText("📌 " + value); // Thêm emoji pin
                    label.setIconTextGap(5); // Khoảng cách giữa icon và text
                } else {
                    label.setIcon(null);
                }
                
                return label;
            }
        });
    }
    
    // Thêm các listeners cho bảng
    public void addTableMouseListeners() {
        // Mouse click listener cho việc lựa chọn hàng và hiển thị menu ngữ cảnh
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                
                // Right-click cho context menu
                if (SwingUtilities.isRightMouseButton(e) && row >= 0) {
                    setRowSelectionInterval(row, row);
                    // Context menu được xử lý ở MainWindow
                }
                // Left-click để chọn/bỏ chọn hàng
                else if (row >= 0) {
                    if (isRowSelected(row)) {
                        clearSelection();
                    } else {
                        setRowSelectionInterval(row, row);
                    }
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // Reset hover state khi di chuyển chuột ra khỏi bảng
                hoveredRow = -1;
                repaint();
            }
        });
        
        // Mouse motion listener cho hover effect
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                
                if (row != hoveredRow) {
                    hoveredRow = row;
                    repaint(); // Vẽ lại với trạng thái hover mới
                }
            }
        });
        
        // Bắt keyboard shortcuts
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int selectedRow = getSelectedRow();
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DELETE:
                        // Delete key để xóa task
                        if (selectedRow >= 0) {
                            // Gửi thông báo xóa task
                            firePropertyChange("deleteTask", -1, selectedRow);
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                        // Enter key để sửa task
                        if (selectedRow >= 0) {
                            // Gửi thông báo sửa task
                            firePropertyChange("editTask", -1, selectedRow);
                        }
                        break;
                    case KeyEvent.VK_P:
                        // P key để pin/unpin task
                        if (selectedRow >= 0 && (e.isControlDown() || e.isMetaDown())) {
                            int modelRow = convertRowIndexToModel(selectedRow);
                            // Gửi thông báo toggle pin
                            firePropertyChange("togglePin", -1, modelRow);
                        }
                        break;
                }
            }
        });
    }
    
    // Quản lý các task được ghim
    public void updatePinnedTasks(LinkedHashSet<Integer> pinnedRows) {
        this.pinnedTaskRows = pinnedRows;
        repaint(); // Vẽ lại để cập nhật icon pin
    }
    
    public DefaultTableModel getTableModel() {
        return tableModel;
    }
    
    public int getHoveredRow() {
        return hoveredRow;
    }
    
    public void setHoveredRow(int row) {
        this.hoveredRow = row;
    }
    
    // Hàm tiện ích để lấy task được chọn
    public String getSelectedTask() {
        int selectedRow = getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = convertRowIndexToModel(selectedRow);
            return (String) tableModel.getValueAt(modelRow, 0);
        }
        return null;
    }
    
    // Hàm tiện ích để lọc tasks theo tiêu chí
    public void filterTasks(String text) {
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) getRowSorter();
        if (text == null || text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }
}