package com.todoapp.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.todoapp.components.TaskTable;
import com.todoapp.controllers.TaskController;
import com.todoapp.utils.PinnedRowSorter;

public class MainWindow extends JFrame {
  private JTable taskTable;
  private DefaultTableModel tableModel;
  private JButton addButton, editButton, deleteButton, sortButton, calendarButton, progressButton;
  private LinkedHashSet<Integer> pinnedTaskRows = new LinkedHashSet<>();
  private TableRowSorter<TableModel> mainRowSorter;
  private int hoveredRow = -1;
  private JLabel taskCountLabel;
  private JTextField searchField;
  private TaskController taskController;

  public MainWindow() {
    // Set up the frame
    setTitle("To-Do List Application");
    setSize(800, 600);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    // Create main panel with border layout and margins
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    setContentPane(mainPanel);

    // Initialize components
    createHeaderPanel();
    createTaskListPanel();
    createButtonPanel();

    // Create task controller
    taskController = new TaskController(this, (TaskTable) taskTable);

    // Clear selection when clicking outside the table
    mainPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        Component source = e.getComponent();
        Point point = e.getPoint();
        Component clicked = SwingUtilities.getDeepestComponentAt(source, point.x, point.y);

        if (clicked != taskTable && !SwingUtilities.isDescendingFrom(clicked, taskTable)) {
          taskTable.clearSelection();
        }
      }
    });

    // Set custom selection color
    taskTable.setSelectionBackground(new Color(173, 216, 230));

    // Set focus to task table on startup
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(WindowEvent e) {
        taskTable.requestFocusInWindow();
      }
    });

    // KHÔNG gọi phương thức setupNavigationButtons() ở đây
  }

  private void createHeaderPanel() {
    // Tạo panel chính cho phần header với layout BoxLayout dọc
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

    // 1. Panel tiêu đề
    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    titlePanel.setBackground(new Color(245, 245, 245));

    // Icon và tiêu đề ứng dụng
    JLabel iconLabel = new JLabel(UIManager.getIcon("FileView.fileIcon"));
    JLabel titleLabel = new JLabel("To-Do List Manager");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
    titleLabel.setForeground(new Color(25, 25, 112));

    JPanel iconTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    iconTitlePanel.setOpaque(false);
    iconTitlePanel.add(iconLabel);
    iconTitlePanel.add(titleLabel);

    titlePanel.add(iconTitlePanel, BorderLayout.WEST);

    // 2. Đường kẻ ngang
    JSeparator separator = new JSeparator();
    separator.setForeground(Color.LIGHT_GRAY);

    // 3. Thanh công cụ (toolbar)
    JPanel toolbarPanel = new JPanel(new BorderLayout());
    toolbarPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    toolbarPanel.setBackground(Color.WHITE);

    // Panel bên trái chứa thông tin số lượng task
    taskCountLabel = new JLabel("8 tasks");
    taskCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    // Panel bên phải chứa ô tìm kiếm
    JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    searchPanel.setOpaque(false);

    // Tạo ô tìm kiếm với placeholder
    searchField = new JTextField(15) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Vẽ placeholder nếu ô tìm kiếm trống và không có focus
        if (getText().isEmpty() && !hasFocus()) {
          Graphics2D g2 = (Graphics2D) g.create();
          g2.setColor(Color.GRAY);
          g2.setFont(getFont().deriveFont(Font.ITALIC));
          FontMetrics fm = g2.getFontMetrics();
          String placeholder = "Search...";
          int x = getInsets().left;
          int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
          g2.drawString(placeholder, x, y);
          g2.dispose();
        }
      }
    };

    // Thiết lập bo góc cho ô tìm kiếm
    searchField.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(5, new Color(200, 200, 200)),
        BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    searchField.setPreferredSize(new Dimension(200, 25));

    // Thêm focus listener để vẽ lại khi focus thay đổi
    searchField.addFocusListener(new java.awt.event.FocusListener() {
      @Override
      public void focusGained(java.awt.event.FocusEvent e) {
        searchField.repaint();
      }

      @Override
      public void focusLost(java.awt.event.FocusEvent e) {
        searchField.repaint();
      }
    });

    // Thêm DocumentListener để tự động cập nhật khi gõ
    searchField.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        performSearch();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        performSearch();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        performSearch();
      }
    });

    searchPanel.add(searchField);

    // Thêm các panel vào toolbar
    toolbarPanel.add(taskCountLabel, BorderLayout.WEST);
    toolbarPanel.add(searchPanel, BorderLayout.EAST);

    // Thêm tất cả các thành phần vào header theo thứ tự
    headerPanel.add(titlePanel);
    headerPanel.add(separator);
    headerPanel.add(toolbarPanel);

    // Thêm header panel vào frame
    add(headerPanel, BorderLayout.NORTH);
  }

  // Thêm class RoundedBorder để tạo border bo tròn
  private static class RoundedBorder extends javax.swing.border.AbstractBorder {
    private final int radius;
    private final Color color;

    RoundedBorder(int radius, Color color) {
      this.radius = radius;
      this.color = color;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(color);
      g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
      g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
      return new Insets(radius / 2, radius, radius / 2, radius);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
      insets.left = radius;
      insets.top = radius / 2;
      insets.right = radius;
      insets.bottom = radius / 2;
      return insets;
    }
  }

  // Thêm phương thức performSearch nếu chưa có
  private void performSearch() {
    if (mainRowSorter == null)
      return; // Đảm bảo đã khởi tạo sorter

    String searchText = searchField.getText().toLowerCase().trim();

    try {
      if (searchText.isEmpty()) {
        // Nếu ô tìm kiếm trống, hiển thị tất cả các nhiệm vụ
        mainRowSorter.setRowFilter(null);
      } else {
        // Tạo bộ lọc để tìm văn bản trong tên nhiệm vụ (cột 0)
        mainRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText, 0));
      }
    } catch (Exception e) {
      // Xử lý lỗi regex (nếu người dùng nhập ký tự đặc biệt)
      mainRowSorter.setRowFilter(null);
    }

    // Cập nhật số lượng nhiệm vụ hiển thị
    updateTaskCount();
  }

  private void createTaskListPanel() {
    // Create table model with columns
    tableModel = new DefaultTableModel();
    tableModel.addColumn("Task");
    tableModel.addColumn("Due Date");
    tableModel.addColumn("Priority");
    tableModel.addColumn("Status");

    // Create task table with custom model
    taskTable = new TaskTable();
    taskTable.setModel(tableModel);

    // Set table properties - matching the cleaner style
    taskTable.setRowHeight(30);
    taskTable.setShowGrid(true);
    taskTable.setGridColor(new Color(230, 230, 230));
    taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    taskTable.setFillsViewportHeight(true);
    taskTable.setIntercellSpacing(new Dimension(5, 5));
    taskTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
    taskTable.getTableHeader().setBackground(new Color(240, 240, 240));
    taskTable.getTableHeader().setReorderingAllowed(false);

    // Set column widths
    taskTable.getColumnModel().getColumn(0).setPreferredWidth(300);
    taskTable.getColumnModel().getColumn(1).setPreferredWidth(100);
    taskTable.getColumnModel().getColumn(2).setPreferredWidth(80);
    taskTable.getColumnModel().getColumn(3).setPreferredWidth(100);

    // Reset all renderers
    for (int i = 0; i < taskTable.getColumnCount(); i++) {
      taskTable.getColumnModel().getColumn(i).setCellRenderer(null);
    }

    // Set default renderer for all cells
    DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
          boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (isSelected) {
          c.setBackground(new Color(173, 216, 230));
          c.setForeground(Color.BLACK);
        } else if (row == hoveredRow) {
          c.setBackground(new Color(240, 248, 255));
          c.setForeground(Color.BLACK);
        } else {
          c.setBackground(table.getBackground());
          c.setForeground(table.getForeground());

          if (column == 2) { // Priority column
            String priority = value != null ? value.toString() : "";
            switch (priority) {
              case "High":
                c.setBackground(new Color(255, 200, 200));
                break;
              case "Medium":
                c.setBackground(new Color(255, 235, 200));
                break;
              case "Low":
                c.setBackground(new Color(220, 255, 220));
                break;
            }
          } else if (column == 3) { // Status column
            String status = value != null ? value.toString() : "";
            switch (status) {
              case "Completed":
                c.setBackground(new Color(200, 230, 255));
                break;
              case "In Progress":
                c.setBackground(new Color(230, 220, 255));
                break;
              case "Pending":
                c.setBackground(new Color(240, 240, 240));
                break;
            }
          }
        }

        if (column == 2 || column == 3) {
          ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
        }

        return c;
      }
    };

    taskTable.setDefaultRenderer(Object.class, defaultRenderer);

    // Special renderer for task name column (first column) to show pin icons
    taskTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
          boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column);

        // Add pin icon if needed
        int modelRow = table.convertRowIndexToModel(row);
        if (pinnedTaskRows.contains(modelRow)) {
          label.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
          label.setText("📌 " + value);
        } else {
          label.setIcon(null);
        }

        return label;
      }
    });

    // Khởi tạo mainRowSorter
    mainRowSorter = new PinnedRowSorter(tableModel, (TaskTable) taskTable, pinnedTaskRows);
    taskTable.setRowSorter(mainRowSorter);

    // Thiết lập mouse listeners
    setupMouseListeners();

    // Add sample tasks
    addSampleTasks();

    // Simple task panel without search bar
    JPanel taskPanel = new JPanel(new BorderLayout());
    JScrollPane scrollPane = new JScrollPane(taskTable);
    scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

    taskPanel.add(scrollPane, BorderLayout.CENTER);
    add(taskPanel, BorderLayout.CENTER);
  }

  private void setupMouseListeners() {
    // Remove existing listeners
    for (MouseListener listener : taskTable.getMouseListeners()) {
      taskTable.removeMouseListener(listener);
    }

    // Add comprehensive mouse listener
    taskTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int row = taskTable.rowAtPoint(e.getPoint());

        // Right-click for context menu
        if (SwingUtilities.isRightMouseButton(e) && row >= 0) {
          taskTable.setRowSelectionInterval(row, row);
          int modelRow = taskTable.convertRowIndexToModel(row);

          JPopupMenu popup = new JPopupMenu();
          JMenuItem pinItem = new JMenuItem(
              pinnedTaskRows.contains(modelRow) ? "Unpin Task" : "Pin Task to Top");

          JMenuItem editItem = new JMenuItem("Edit Task");
          JMenuItem deleteItem = new JMenuItem("Delete Task");
          JMenuItem completeItem = new JMenuItem("Mark as Completed");

          pinItem.addActionListener(evt -> togglePinTask(modelRow));
          editItem.addActionListener(evt -> editTask());
          deleteItem.addActionListener(evt -> deleteTask());

          completeItem.addActionListener(evt -> {
            tableModel.setValueAt("Completed", modelRow, 3);
            updateTaskCount();
          });

          popup.add(pinItem);
          popup.addSeparator();
          popup.add(editItem);
          popup.add(deleteItem);
          popup.addSeparator();
          popup.add(completeItem);

          popup.show(taskTable, e.getX(), e.getY());
        }
        // Left-click toggles selection
        else if (row >= 0) {
          if (taskTable.isRowSelected(row)) {
            taskTable.clearSelection();
          } else {
            taskTable.setRowSelectionInterval(row, row);
          }
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hoveredRow = -1;
        taskTable.repaint();
      }
    });

    // Add hover effect
    taskTable.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int row = taskTable.rowAtPoint(e.getPoint());
        if (row != hoveredRow) {
          hoveredRow = row;
          taskTable.repaint();
        }
      }
    });
  }

  private void createButtonPanel() {
    // Tạo panel với GridLayout cho 2 hàng, 4 cột, khoảng cách 10px
    JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 10, 10));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    buttonPanel.setBackground(Color.WHITE); // Đặt màu nền trắng

    // Tạo các nút với thiết kế đơn giản, không viền màu

    // Hàng 1: Add, Edit, Delete, Sort
    addButton = createCleanButton("Add", UIManager.getIcon("FileView.fileIcon"));
    editButton = createCleanButton("Edit", UIManager.getIcon("FileView.directoryIcon"));
    deleteButton = createCleanButton("Delete", UIManager.getIcon("Table.descendingSortIcon"));
    sortButton = createCleanButton("Sort", UIManager.getIcon("Table.ascendingSortIcon"));

    // Hàng 2: Calendar, Progress, Export, Import
    calendarButton = createCleanButton("Calendar", UIManager.getIcon("FileChooser.detailsViewIcon"));
    progressButton = createCleanButton("Progress", UIManager.getIcon("OptionPane.informationIcon"));
    JButton exportButton = createCleanButton("Export", UIManager.getIcon("FileView.hardDriveIcon"));
    JButton importButton = createCleanButton("Import", UIManager.getIcon("FileView.fileIcon"));

    // Thêm action listeners
    addButton.addActionListener(e -> addTask());
    editButton.addActionListener(e -> editTask());
    deleteButton.addActionListener(e -> deleteTask());
    sortButton.addActionListener(e -> showSortDialog());
    calendarButton.addActionListener(e -> showCalendarDialog());
    progressButton.addActionListener(e -> showProgressDialog());
    exportButton.addActionListener(e -> exportTasks());
    importButton.addActionListener(e -> importTasks());

    // Thêm các nút vào panel
    buttonPanel.add(addButton);
    buttonPanel.add(editButton);
    buttonPanel.add(deleteButton);
    buttonPanel.add(sortButton);
    buttonPanel.add(calendarButton);
    buttonPanel.add(progressButton);
    buttonPanel.add(exportButton);
    buttonPanel.add(importButton);

    // Thêm panel vào layout chính (ở dưới cùng)
    add(buttonPanel, BorderLayout.SOUTH);
  }

  // Tạo nút với giao diện sạch, không viền màu
  private JButton createCleanButton(String text, Icon icon) {
    JButton button = new JButton(text);
    button.setFont(new Font("SansSerif", Font.BOLD, 12));
    button.setIcon(icon);

    // Căn chỉnh icon và text
    button.setVerticalTextPosition(SwingConstants.BOTTOM);
    button.setHorizontalTextPosition(SwingConstants.CENTER);
    button.setHorizontalAlignment(SwingConstants.CENTER);

    // Thiết lập màu sắc
    button.setBackground(Color.WHITE);
    button.setForeground(Color.BLACK);

    // Tạo viền đơn giản
    button.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
        BorderFactory.createEmptyBorder(5, 10, 5, 10)));

    // Bỏ màu focus painted mặc định
    button.setFocusPainted(false);

    // Hiệu ứng khi hover
    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        button.setBackground(new Color(245, 245, 245)); // Màu xám nhạt khi hover
      }

      @Override
      public void mouseExited(MouseEvent e) {
        button.setBackground(Color.WHITE);
      }
    });

    return button;
  }

  private void addSampleTasks() {
    // Add some initial tasks with variety of priorities, statuses and dates
    tableModel.addRow(new Object[] { "Complete Java assignment", "2025-05-28", "High", "Pending" });
    tableModel.addRow(new Object[] { "Buy groceries", "2025-05-26", "Medium", "Pending" });
    tableModel.addRow(new Object[] { "Schedule dentist appointment", "2025-06-15", "Low", "Pending" });
    tableModel.addRow(new Object[] { "Complete project documentation", "2025-06-02", "High", "Completed" });
    tableModel.addRow(new Object[] { "Plan summer vacation", "2025-07-15", "Medium", "In Progress" });

    // Add a task for today
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String today = dateFormat.format(new Date());
    tableModel.addRow(new Object[] { "Urgent client meeting", today, "High", "In Progress" });
    tableModel.addRow(new Object[] { "Review code changes", "2025-06-03", "Medium", "Pending" });
    tableModel.addRow(new Object[] { "Team lunch", today, "Low", "Pending" });

    updateTaskCount();
  }

  /**
   * Cập nhật thông tin số lượng task hiển thị trên giao diện
   */
  public void updateTaskCount() {
    int total = tableModel.getRowCount();
    int completed = 0;
    int inProgress = 0;
    int pending = 0;

    // Đếm số lượng task theo từng trạng thái
    for (int i = 0; i < total; i++) {
      String status = (String) tableModel.getValueAt(i, 3);
      if ("Completed".equals(status)) {
        completed++;
      } else if ("In Progress".equals(status)) {
        inProgress++;
      } else {
        pending++;
      }
    }

    // Tính tỷ lệ hoàn thành
    double completionRate = total > 0 ? (completed * 100.0 / total) : 0;

    // Cập nhật nhãn hiển thị số lượng task
    if (taskCountLabel != null) {
      taskCountLabel.setText(String.format("%d tasks, %d completed (%.1f%%)",
          total, completed, completionRate));
    }

    // Bỏ bộ lọc trên table nếu kết quả lọc không có task nào
    if (total == 0 && mainRowSorter != null && mainRowSorter.getRowFilter() != null) {
      mainRowSorter.setRowFilter(null);
      if (searchField != null) {
        searchField.setText("");
      }
    }
  }

  public void togglePinTask(int modelRow) {
    if (pinnedTaskRows.contains(modelRow)) {
      pinnedTaskRows.remove(modelRow);
    } else {
      pinnedTaskRows.add(modelRow);
    }

    // Đảm bảo mainRowSorter đã được khởi tạo và là một PinnedRowSorter
    if (mainRowSorter != null && mainRowSorter instanceof PinnedRowSorter) {
      ((PinnedRowSorter) mainRowSorter).applySorting();
    }
    taskTable.repaint();
  }

  private void addTask() {
    TaskDialog dialog = new TaskDialog(this, "Add New Task", "", "", "Medium", "Pending");
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      tableModel.addRow(new Object[] {
          dialog.getTaskName(),
          dialog.getDueDate(),
          dialog.getPriority(),
          dialog.getStatus()
      });
      updateTaskCount();
    }
  }

  private void editTask() {
    int selectedRow = taskTable.getSelectedRow();
    if (selectedRow >= 0) {
      int modelRow = taskTable.convertRowIndexToModel(selectedRow);

      String currentTask = (String) tableModel.getValueAt(modelRow, 0);
      String currentDate = (String) tableModel.getValueAt(modelRow, 1);
      String currentPriority = (String) tableModel.getValueAt(modelRow, 2);
      String currentStatus = (String) tableModel.getValueAt(modelRow, 3);

      TaskDialog dialog = new TaskDialog(this, "Edit Task",
          currentTask, currentDate, currentPriority, currentStatus);
      dialog.setVisible(true);

      if (dialog.isConfirmed()) {
        tableModel.setValueAt(dialog.getTaskName(), modelRow, 0);
        tableModel.setValueAt(dialog.getDueDate(), modelRow, 1);
        tableModel.setValueAt(dialog.getPriority(), modelRow, 2);
        tableModel.setValueAt(dialog.getStatus(), modelRow, 3);
        updateTaskCount();
      }
    } else {
      JOptionPane.showMessageDialog(this,
          "Please select a task to edit.",
          "No Task Selected", JOptionPane.WARNING_MESSAGE);
    }
  }

  private void deleteTask() {
    int selectedRow = taskTable.getSelectedRow();
    if (selectedRow >= 0) {
      int modelRow = taskTable.convertRowIndexToModel(selectedRow);
      String taskName = (String) tableModel.getValueAt(modelRow, 0);

      int confirm = JOptionPane.showConfirmDialog(this,
          "Are you sure you want to delete \"" + taskName + "\"?",
          "Confirm Delete", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);

      if (confirm == JOptionPane.YES_OPTION) {
        // Xóa khỏi danh sách pin nếu cần
        pinnedTaskRows.remove(modelRow);

        // Cập nhật các chỉ số pin cho các task sau task bị xóa
        for (Integer pinnedRow : new ArrayList<>(pinnedTaskRows)) {
          if (pinnedRow > modelRow) {
            pinnedTaskRows.remove(pinnedRow);
            pinnedTaskRows.add(pinnedRow - 1);
          }
        }

        tableModel.removeRow(modelRow);
        updateTaskCount();
      }
    } else {
      JOptionPane.showMessageDialog(this,
          "Please select a task to delete.",
          "No Task Selected", JOptionPane.WARNING_MESSAGE);
    }
  }

  private void showSortDialog() {
    String[] options = { "Task Name", "Due Date", "Priority", "Status" };
    String selection = (String) JOptionPane.showInputDialog(this,
        "Sort by which column?", "Sort Tasks",
        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

    if (selection != null) {
      int column = java.util.Arrays.asList(options).indexOf(selection);
      mainRowSorter.toggleSortOrder(column);
    }
  }

  private void showCalendarDialog() {
    DatePickerDialog dialog = new DatePickerDialog(this, tableModel);
    dialog.setVisible(true);
  }

  private void showProgressDialog() {
    int total = tableModel.getRowCount();
    int completed = 0;
    int inProgress = 0;
    int pending = 0;

    for (int i = 0; i < total; i++) {
      String status = (String) tableModel.getValueAt(i, 3);
      switch (status) {
        case "Completed":
          completed++;
          break;
        case "In Progress":
          inProgress++;
          break;
        case "Pending":
          pending++;
          break;
      }
    }

    double percentage = total > 0 ? (completed * 100.0 / total) : 0;

    // Create progress panel with nice visualization
    JPanel progressPanel = new JPanel(new BorderLayout(0, 10));
    progressPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    // Title
    JLabel titleLabel = new JLabel("Task Progress Report", SwingConstants.CENTER);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    progressPanel.add(titleLabel, BorderLayout.NORTH);

    // Progress visualization
    JPanel chartPanel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth() - 40;
        int height = 20;
        int x = 20;
        int y = getHeight() / 2 - height / 2;

        // Background
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRoundRect(x, y, width, height, 10, 10);

        // Progress bar
        if (total > 0) {
          g2d.setColor(new Color(76, 175, 80));
          int progressWidth = (int) (width * percentage / 100);
          g2d.fillRoundRect(x, y, progressWidth, height, 10, 10);

          // Percentage text
          g2d.setColor(Color.BLACK);
          g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
          String percentText = String.format("%.1f%%", percentage);
          FontMetrics fm = g2d.getFontMetrics();
          int textX = x + width / 2 - fm.stringWidth(percentText) / 2;
          int textY = y + height / 2 + fm.getAscent() / 2;
          g2d.drawString(percentText, textX, textY);
        }
      }

      @Override
      public Dimension getPreferredSize() {
        return new Dimension(400, 60);
      }
    };

    progressPanel.add(chartPanel, BorderLayout.CENTER);

    // Stats panel
    JPanel statsPanel = new JPanel(new GridLayout(4, 2, 10, 5));
    statsPanel.add(new JLabel("Total Tasks:"));
    statsPanel.add(new JLabel(String.valueOf(total)));
    statsPanel.add(new JLabel("Completed:"));
    statsPanel.add(new JLabel(completed + " (" + String.format("%.1f%%", percentage) + ")"));
    statsPanel.add(new JLabel("In Progress:"));
    statsPanel.add(new JLabel(String.valueOf(inProgress)));
    statsPanel.add(new JLabel("Pending:"));
    statsPanel.add(new JLabel(String.valueOf(pending)));

    progressPanel.add(statsPanel, BorderLayout.SOUTH);

    // Show motivational message based on progress
    String message;
    if (percentage == 100) {
      message = "Congratulations! You've completed all your tasks! 🎉";
    } else if (percentage >= 75) {
      message = "Great progress! You're almost there! 💪";
    } else if (percentage >= 50) {
      message = "Halfway there! Keep up the good work! 👍";
    } else if (percentage >= 25) {
      message = "Good start! Keep going! 🚀";
    } else if (percentage > 0) {
      message = "Just getting started! You can do this! 😊";
    } else {
      message = "No tasks completed yet. Time to get started! 🏁";
    }

    JOptionPane.showMessageDialog(this, progressPanel,
        message, JOptionPane.INFORMATION_MESSAGE);
  }

  private void exportTasks() {
    // Tạo file chooser để chọn vị trí lưu file
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Tasks");
    fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

    // Show save dialog
    int choice = fileChooser.showSaveDialog(this);

    if (choice == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      String path = file.getAbsolutePath();

      // Add .txt extension if not present
      if (!path.toLowerCase().endsWith(".txt")) {
        file = new File(path + ".txt");
      }

      try (PrintWriter writer = new PrintWriter(file)) {
        // Write header
        writer.println("# Todo List Export");
        writer.println("# Format: Task Name|Due Date|Priority|Status");

        // Write current date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        writer.println("# Generated on: " + dateFormat.format(new Date()));
        writer.println();

        // Write tasks
        int total = 0;
        int completed = 0;
        int inProgress = 0;
        int pending = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
          String task = (String) tableModel.getValueAt(i, 0);
          String dueDate = (String) tableModel.getValueAt(i, 1);
          String priority = (String) tableModel.getValueAt(i, 2);
          String status = (String) tableModel.getValueAt(i, 3);

          // Escape vertical bars in values
          task = task.replace("|", "\\|");

          // Write task line
          writer.println(task + "|" + dueDate + "|" + priority + "|" + status);

          // Count statuses
          total++;
          if ("Completed".equals(status)) {
            completed++;
          } else if ("In Progress".equals(status)) {
            inProgress++;
          } else {
            pending++;
          }
        }

        // Write summary
        writer.println();
        writer.println("# Export Summary:");
        writer.println("# Total tasks exported: " + total);
        writer.println("# Completed: " + completed + ", In Progress: " + inProgress + ", Pending: " + pending);

        // Calculate completion rate
        double completionRate = total > 0 ? (completed * 100.0 / total) : 0;
        writer.printf("# Completion rate: %.1f%%", completionRate);

        JOptionPane.showMessageDialog(this,
            "Tasks exported successfully to " + file.getName(),
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE);

      } catch (IOException e) {
        JOptionPane.showMessageDialog(this,
            "Error exporting tasks: " + e.getMessage(),
            "Export Failed",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void importTasks() {
    // Tạo file chooser để chọn file cần import
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Import Tasks");
    fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

    // Show open dialog
    int choice = fileChooser.showOpenDialog(this);

    if (choice == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();

      // Đọc file và import tasks
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        // Tạo danh sách tạm để lưu các task được đọc từ file
        ArrayList<String[]> importedTasks = new ArrayList<>();
        String line;

        // Đọc từng dòng của file
        while ((line = reader.readLine()) != null) {
          // Bỏ qua các dòng comment (bắt đầu bằng #)
          if (line.trim().startsWith("#") || line.trim().isEmpty()) {
            continue;
          }

          // Split dòng theo dấu |
          String[] parts = line.split("\\|");

          // Đảm bảo line có đủ 4 phần: task, due date, priority, status
          if (parts.length >= 4) {
            // Khôi phục các dấu | trong tên task nếu đã được escape
            parts[0] = parts[0].replace("\\|", "|");

            // Thêm task vào danh sách tạm
            importedTasks.add(new String[] { parts[0], parts[1], parts[2], parts[3] });
          }
        }

        // Nếu không có task nào được import
        if (importedTasks.isEmpty()) {
          JOptionPane.showMessageDialog(this,
              "No valid tasks found in the selected file.",
              "Import Failed",
              JOptionPane.WARNING_MESSAGE);
          return;
        }

        // Hỏi người dùng có muốn xóa tasks hiện tại hay không
        Object[] options = { "Clear Current Tasks", "Keep Current Tasks" };
        int response = JOptionPane.showOptionDialog(this,
            "Found " + importedTasks.size() + " tasks to import.\nDo you want to clear current tasks or keep them?",
            "Import Options",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1]); // Default option is "Keep"

        // Nếu người dùng chọn xóa tasks hiện tại
        if (response == JOptionPane.YES_OPTION) {
          // Xóa tất cả các hàng trong bảng
          while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
          }

          // Xóa danh sách tasks đã ghim
          pinnedTaskRows.clear();
        }

        // Thêm các task đã import vào bảng
        for (String[] task : importedTasks) {
          tableModel.addRow(task);
        }

        // Cập nhật số lượng task
        updateTaskCount();

        // Thông báo thành công
        JOptionPane.showMessageDialog(this,
            importedTasks.size() + " tasks imported successfully!",
            "Import Complete",
            JOptionPane.INFORMATION_MESSAGE);

      } catch (IOException e) {
        JOptionPane.showMessageDialog(this,
            "Error importing tasks: " + e.getMessage(),
            "Import Failed",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void showAddTaskDialog() {
    // Tạo dialog mới cho việc thêm task
    JDialog dialog = new JDialog(this, "Add Task", true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setSize(400, 300);
    dialog.setLocationRelativeTo(this);

    // Panel chính cho dialog
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    dialog.add(panel);

    // Tiêu đề
    JLabel titleLabel = new JLabel("Add New Task", SwingConstants.CENTER);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    panel.add(titleLabel, BorderLayout.NORTH);

    // Panel chứa các trường nhập liệu
    JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
    panel.add(inputPanel, BorderLayout.CENTER);

    // Tên task
    JLabel nameLabel = new JLabel("Task Name:");
    JTextField nameField = new JTextField();
    inputPanel.add(nameLabel);
    inputPanel.add(nameField);

    // Ngày hết hạn
    JLabel dateLabel = new JLabel("Due Date:");
    JTextField dateField = new JTextField();
    inputPanel.add(dateLabel);
    inputPanel.add(dateField);

    // Độ ưu tiên
    JLabel priorityLabel = new JLabel("Priority:");
    String[] priorities = { "Low", "Medium", "High" };
    JComboBox<String> priorityCombo = new JComboBox<>(priorities);
    inputPanel.add(priorityLabel);
    inputPanel.add(priorityCombo);

    // Trạng thái
    JLabel statusLabel = new JLabel("Status:");
    String[] statuses = { "Pending", "In Progress", "Completed" };
    JComboBox<String> statusCombo = new JComboBox<>(statuses);
    inputPanel.add(statusLabel);
    inputPanel.add(statusCombo);

    // Panel chứa các nút
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    buttonPanel.setOpaque(false);
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

    // Tạo nút Save
    JButton saveButton = new JButton("Save");
    saveButton.setPreferredSize(new Dimension(100, 30));
    saveButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
    saveButton.setBackground(Color.WHITE);
    saveButton.setForeground(Color.BLACK);
    saveButton.setFocusPainted(false);
    // Sử dụng CHÍNH XÁC cùng loại viền với nút Cancel
    saveButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));

    // Tạo nút Cancel
    JButton cancelButton = new JButton("Cancel");
    cancelButton.setPreferredSize(new Dimension(100, 30));
    cancelButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
    cancelButton.setBackground(Color.WHITE);
    cancelButton.setForeground(Color.BLACK);
    cancelButton.setFocusPainted(false);
    // Sử dụng CHÍNH XÁC cùng loại viền với nút Save
    cancelButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));

    // Tạo một MouseAdapter chung cho cả hai nút
    MouseAdapter hoverEffect = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        ((JButton) e.getSource()).setBackground(new Color(240, 240, 240));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        ((JButton) e.getSource()).setBackground(Color.WHITE);
      }
    };

    // Áp dụng hiệu ứng hover cho cả hai nút
    saveButton.addMouseListener(hoverEffect);
    cancelButton.addMouseListener(hoverEffect);

    // Thêm các nút vào panel
    buttonPanel.add(saveButton);
    buttonPanel.add(cancelButton);

    panel.add(buttonPanel, BorderLayout.SOUTH);

    // Xử lý sự kiện cho nút Save
    saveButton.addActionListener(e -> {
      String taskName = nameField.getText().trim();
      String dueDate = dateField.getText().trim();
      String priority = (String) priorityCombo.getSelectedItem();
      String status = (String) statusCombo.getSelectedItem();

      // Kiểm tra tính hợp lệ của dữ liệu nhập vào
      if (taskName.isEmpty() || dueDate.isEmpty() || priority == null || status == null) {
        JOptionPane.showMessageDialog(dialog,
            "Please fill in all fields.",
            "Invalid Input", JOptionPane.ERROR_MESSAGE);
        return;
      }

      // Thêm task mới vào bảng
      tableModel.addRow(new Object[] { taskName, dueDate, priority, status });
      updateTaskCount();

      dialog.dispose();
    });

    // Xử lý sự kiện cho nút Cancel
    cancelButton.addActionListener(e -> dialog.dispose());

    dialog.setVisible(true);
  }
}