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
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
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
import com.todoapp.models.Task;
import com.todoapp.utils.PinnedRowSorter;
import com.todoapp.utils.TaskManager;
import com.todoapp.utils.UserPreferences;

/**
 * Main window cho ToDo List application
 * Quản lý giao diện chính và điều phối các component
 */
public class MainWindow extends JFrame {
  private TaskController taskController;
  private TaskManager taskManager;

  private TaskTable taskTable;
  private DefaultTableModel tableModel;
  private JButton addButton, editButton, deleteButton, sortButton, calendarButton, progressButton;
  private LinkedHashSet<Integer> pinnedTaskRows = new LinkedHashSet<>();
  private TableRowSorter<TableModel> mainRowSorter;
  private int hoveredRow = -1;
  private JLabel taskCountLabel;
  private JTextField searchField;

  public MainWindow() {
    this.taskManager = new TaskManager();

    setTitle("ToDo List App");
    setSize(800, 600);
    setMinimumSize(new Dimension(600, 400));
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setLocationRelativeTo(null);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        confirmExitApplication();
      }
    });

    initUI();
    this.taskController = new TaskController(this, taskTable);
    loadInitialData();
    setupSaveShortcut();
  }

  /**
   * Load dữ liệu ban đầu và đồng bộ với UI
   */
  private void loadInitialData() {
    boolean loaded = taskManager.loadTasksFromFile();
    syncTasksFromManagerToUI();
    updateTaskCount();
  }

  /**
   * Đồng bộ tasks từ TaskManager lên UI
   */
  private void syncTasksFromManagerToUI() {
    DefaultTableModel model = (DefaultTableModel) taskTable.getModel();
    model.setRowCount(0);

    List<Task> allTasks = taskManager.getAllTasks();

    for (Task task : allTasks) {
      String dueDate = task.getDueDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(task.getDueDate()) : "";

      model.addRow(new Object[] {
          task.getName(),
          dueDate,
          task.getPriority(),
          task.getStatus()
      });
    }
  }

  private void initUI() {
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    setContentPane(mainPanel);

    JMenuBar menuBar = new JMenuBar();
    setupFileMenu(menuBar);
    setJMenuBar(menuBar);

    createHeaderPanel();
    createTaskListPanel();
    createButtonPanel();
  }

  /**
   * Confirm exit và save changes nếu cần
   */
  private void confirmExitApplication() {
    if (taskManager.hasUnsavedChanges()) {
      int option = JOptionPane.showConfirmDialog(
          this,
          "Bạn có thay đổi chưa được lưu. Bạn có muốn lưu trước khi thoát không?",
          "Xác nhận thoát",
          JOptionPane.YES_NO_CANCEL_OPTION,
          JOptionPane.QUESTION_MESSAGE);

      if (option == JOptionPane.YES_OPTION) {
        boolean saved = taskManager.saveTasksToFile();
        if (saved) {
          dispose();
          System.exit(0);
        } else {
          JOptionPane.showMessageDialog(
              this,
              "Không thể lưu dữ liệu. Vui lòng thử lại hoặc kiểm tra quyền truy cập file.",
              "Lỗi lưu dữ liệu",
              JOptionPane.ERROR_MESSAGE);
        }
      } else if (option == JOptionPane.NO_OPTION) {
        dispose();
        System.exit(0);
      }
    } else {
      dispose();
      System.exit(0);
    }
  }

  /**
   * Setup File menu với Save, Load, Settings, Exit options
   */
  private void setupFileMenu(JMenuBar menuBar) {
    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);

    JMenuItem saveItem = new JMenuItem("Lưu", KeyEvent.VK_S);
    saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
    saveItem.addActionListener(e -> saveData());

    JMenuItem loadItem = new JMenuItem("Tải lại từ file", KeyEvent.VK_L);
    loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
    loadItem.addActionListener(e -> reloadData());

    // Settings submenu
    JMenu settingsMenu = new JMenu("Settings");

    JCheckBoxMenuItem autoFillTodayItem = new JCheckBoxMenuItem("Auto-fill today's date");
    autoFillTodayItem.setSelected(UserPreferences.isAutoFillTodayEnabled());
    autoFillTodayItem.addActionListener(e -> {
      UserPreferences.setAutoFillToday(autoFillTodayItem.isSelected());
      JOptionPane.showMessageDialog(this,
          "Setting saved. Will take effect for new tasks.",
          "Settings", JOptionPane.INFORMATION_MESSAGE);
    });

    settingsMenu.add(autoFillTodayItem);

    JMenuItem exitItem = new JMenuItem("Thoát", KeyEvent.VK_X);
    exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
    exitItem.addActionListener(e -> confirmExitApplication());

    fileMenu.add(saveItem);
    fileMenu.add(loadItem);
    fileMenu.addSeparator();
    fileMenu.add(settingsMenu);
    fileMenu.addSeparator();
    fileMenu.add(exitItem);

    menuBar.add(fileMenu);
  }

  /**
   * Refresh task table với dữ liệu từ TaskManager
   */
  private void refreshTaskTable() {
    tableModel.setRowCount(0);

    List<Task> allTasks = taskManager.getAllTasks();
    for (Task task : allTasks) {
      Object[] rowData = {
          task.getName(),
          task.getDueDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(task.getDueDate()) : "",
          task.getPriority(),
          task.getStatus()
      };
      tableModel.addRow(rowData);

      if (task.isPinned()) {
        pinnedTaskRows.add(tableModel.getRowCount() - 1);
      }
    }

    taskTable.repaint();
    updateTaskCount();
  }

  private void createHeaderPanel() {
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

    // Title panel
    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    titlePanel.setBackground(new Color(245, 245, 245));

    JLabel iconLabel = new JLabel(UIManager.getIcon("FileView.fileIcon"));
    JLabel titleLabel = new JLabel("To-Do List Manager");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
    titleLabel.setForeground(new Color(25, 25, 112));

    JPanel iconTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    iconTitlePanel.setOpaque(false);
    iconTitlePanel.add(iconLabel);
    iconTitlePanel.add(titleLabel);

    titlePanel.add(iconTitlePanel, BorderLayout.WEST);

    JSeparator separator = new JSeparator();
    separator.setForeground(Color.LIGHT_GRAY);

    // Toolbar panel
    JPanel toolbarPanel = new JPanel(new BorderLayout());
    toolbarPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    toolbarPanel.setBackground(Color.WHITE);

    taskCountLabel = new JLabel("8 tasks");
    taskCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    // Search panel
    JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    searchPanel.setOpaque(false);

    searchField = new JTextField(15) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);

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

    searchField.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(5, new Color(200, 200, 200)),
        BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    searchField.setPreferredSize(new Dimension(200, 25));

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

    toolbarPanel.add(taskCountLabel, BorderLayout.WEST);
    toolbarPanel.add(searchPanel, BorderLayout.EAST);

    headerPanel.add(titlePanel);
    headerPanel.add(separator);
    headerPanel.add(toolbarPanel);

    add(headerPanel, BorderLayout.NORTH);
  }

  /**
   * Custom rounded border cho search field
   */
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

  private void performSearch() {
    if (mainRowSorter == null)
      return;

    String searchText = searchField.getText().toLowerCase().trim();

    try {
      if (searchText.isEmpty()) {
        mainRowSorter.setRowFilter(null);
      } else {
        mainRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText, 0));
      }
    } catch (Exception e) {
      mainRowSorter.setRowFilter(null);
    }

    updateTaskCount();
  }

  private void createTaskListPanel() {
    tableModel = new DefaultTableModel();
    tableModel.addColumn("Task");
    tableModel.addColumn("Due Date");
    tableModel.addColumn("Priority");
    tableModel.addColumn("Status");

    taskTable = new TaskTable();
    taskTable.setModel(tableModel);

    // Table properties
    taskTable.setRowHeight(30);
    taskTable.setShowGrid(true);
    taskTable.setGridColor(new Color(230, 230, 230));
    taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    taskTable.setFillsViewportHeight(true);
    taskTable.setIntercellSpacing(new Dimension(5, 5));
    taskTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
    taskTable.getTableHeader().setBackground(new Color(240, 240, 240));
    taskTable.getTableHeader().setReorderingAllowed(false);

    // Column widths
    taskTable.getColumnModel().getColumn(0).setPreferredWidth(300);
    taskTable.getColumnModel().getColumn(1).setPreferredWidth(100);
    taskTable.getColumnModel().getColumn(2).setPreferredWidth(80);
    taskTable.getColumnModel().getColumn(3).setPreferredWidth(100);

    // Reset renderers
    for (int i = 0; i < taskTable.getColumnCount(); i++) {
      taskTable.getColumnModel().getColumn(i).setCellRenderer(null);
    }

    // Default cell renderer với color coding
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

          if (column == 2) { // Priority
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
          } else if (column == 3) { // Status
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

    // Pin icon renderer cho task name column
    taskTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
          boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column);

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

    mainRowSorter = new PinnedRowSorter(tableModel, (TaskTable) taskTable, pinnedTaskRows);
    taskTable.setRowSorter(mainRowSorter);

    setupMouseListeners();
    addSampleTasks();

    JPanel taskPanel = new JPanel(new BorderLayout());
    JScrollPane scrollPane = new JScrollPane(taskTable);
    scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

    taskPanel.add(scrollPane, BorderLayout.CENTER);
    add(taskPanel, BorderLayout.CENTER);
  }

  private void setupMouseListeners() {
    for (MouseListener listener : taskTable.getMouseListeners()) {
      taskTable.removeMouseListener(listener);
    }

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
    JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 10, 10));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    buttonPanel.setBackground(Color.WHITE);

    addButton = createCleanButton("Add", UIManager.getIcon("FileView.fileIcon"));
    editButton = createCleanButton("Edit", UIManager.getIcon("FileView.directoryIcon"));
    deleteButton = createCleanButton("Delete", UIManager.getIcon("Table.descendingSortIcon"));
    sortButton = createCleanButton("Sort", UIManager.getIcon("Table.ascendingSortIcon"));

    calendarButton = createCleanButton("Calendar", UIManager.getIcon("FileChooser.detailsViewIcon"));
    progressButton = createCleanButton("Progress", UIManager.getIcon("OptionPane.informationIcon"));
    JButton exportButton = createCleanButton("Export", UIManager.getIcon("FileView.hardDriveIcon"));
    JButton importButton = createCleanButton("Import", UIManager.getIcon("FileView.fileIcon"));

    // Action listeners
    addButton.addActionListener(e -> addTask());
    editButton.addActionListener(e -> editTask());
    deleteButton.addActionListener(e -> deleteTask());
    sortButton.addActionListener(e -> showSortDialog());
    calendarButton.addActionListener(e -> showCalendarDialog());
    progressButton.addActionListener(e -> showProgressDialog());
    exportButton.addActionListener(e -> exportTasks());
    importButton.addActionListener(e -> importTasks());

    buttonPanel.add(addButton);
    buttonPanel.add(editButton);
    buttonPanel.add(deleteButton);
    buttonPanel.add(sortButton);
    buttonPanel.add(calendarButton);
    buttonPanel.add(progressButton);
    buttonPanel.add(exportButton);
    buttonPanel.add(importButton);

    add(buttonPanel, BorderLayout.SOUTH);
  }

  private JButton createCleanButton(String text, Icon icon) {
    JButton button = new JButton(text);
    button.setFont(new Font("SansSerif", Font.BOLD, 12));
    button.setIcon(icon);

    button.setVerticalTextPosition(SwingConstants.BOTTOM);
    button.setHorizontalTextPosition(SwingConstants.CENTER);
    button.setHorizontalAlignment(SwingConstants.CENTER);

    button.setBackground(Color.WHITE);
    button.setForeground(Color.BLACK);

    button.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
        BorderFactory.createEmptyBorder(5, 10, 5, 10)));

    button.setFocusPainted(false);

    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        button.setBackground(new Color(245, 245, 245));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        button.setBackground(Color.WHITE);
      }
    });

    return button;
  }

  private void addSampleTasks() {
    updateTaskCount();
  }

  /**
   * Cập nhật task count label với thống kê
   */
  public void updateTaskCount() {
    int totalTasks = taskTable.getRowCount();
    int completedTasks = 0;

    DefaultTableModel model = (DefaultTableModel) taskTable.getModel();
    for (int i = 0; i < model.getRowCount(); i++) {
      String status = (String) model.getValueAt(i, 3);
      if ("Completed".equals(status)) {
        completedTasks++;
      }
    }

    // Cập nhật task count label nếu có
    if (taskCountLabel != null) {
      taskCountLabel.setText(totalTasks + " tasks, " + completedTasks + " completed (" +
          (totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0) + "%)");
    }
  }

  public void togglePinTask(int modelRow) {
    if (pinnedTaskRows.contains(modelRow)) {
      pinnedTaskRows.remove(modelRow);
    } else {
      pinnedTaskRows.add(modelRow);
    }

    if (mainRowSorter != null && mainRowSorter instanceof PinnedRowSorter) {
      ((PinnedRowSorter) mainRowSorter).applySorting();
    }
    taskTable.repaint();
  }

  // ==================== TASK OPERATIONS ====================

  private void addTask() {
    // Để empty string cho dueDate để trigger auto-fill today
    TaskDialog dialog = new TaskDialog(this, "Add New Task", "", "", "Medium", "Pending");
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      Task newTask = new Task(dialog.getTaskName());
      newTask.setDueDate(dialog.getDueDateObject());
      newTask.setPriority(dialog.getPriority());
      newTask.setStatus(dialog.getStatus());

      taskManager.addTask(newTask);

      tableModel.addRow(new Object[] {
          dialog.getTaskName(), dialog.getDueDate(), dialog.getPriority(), dialog.getStatus()
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
        Task updatedTask = new Task(dialog.getTaskName());
        updatedTask.setDueDate(dialog.getDueDateObject());
        updatedTask.setPriority(dialog.getPriority());
        updatedTask.setStatus(dialog.getStatus());
        updatedTask.setPinned(pinnedTaskRows.contains(modelRow));

        taskManager.updateTask(modelRow, updatedTask);

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
        taskManager.deleteTask(modelRow);

        pinnedTaskRows.remove(modelRow);

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

  /**
   * Hiển thị progress dialog với thống kê và visualization
   */
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

    JPanel progressPanel = new JPanel(new BorderLayout(0, 10));
    progressPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JLabel titleLabel = new JLabel("Task Progress Report", SwingConstants.CENTER);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    progressPanel.add(titleLabel, BorderLayout.NORTH);

    // Progress bar visualization
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

        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRoundRect(x, y, width, height, 10, 10);

        if (total > 0) {
          g2d.setColor(new Color(76, 175, 80));
          int progressWidth = (int) (width * percentage / 100);
          g2d.fillRoundRect(x, y, progressWidth, height, 10, 10);

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

    // Motivational message
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

  /**
   * Export tasks ra file text với header và summary
   */
  private void exportTasks() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Tasks");
    fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

    int choice = fileChooser.showSaveDialog(this);

    if (choice == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      String path = file.getAbsolutePath();

      if (!path.toLowerCase().endsWith(".txt")) {
        file = new File(path + ".txt");
      }

      try (PrintWriter writer = new PrintWriter(file)) {
        writer.println("# Todo List Export");
        writer.println("# Format: Task Name|Due Date|Priority|Status");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        writer.println("# Generated on: " + dateFormat.format(new Date()));
        writer.println();

        int total = 0;
        int completed = 0;
        int inProgress = 0;
        int pending = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
          String task = (String) tableModel.getValueAt(i, 0);
          String dueDate = (String) tableModel.getValueAt(i, 1);
          String priority = (String) tableModel.getValueAt(i, 2);
          String status = (String) tableModel.getValueAt(i, 3);

          task = task.replace("|", "\\|");

          writer.println(task + "|" + dueDate + "|" + priority + "|" + status);

          total++;
          if ("Completed".equals(status)) {
            completed++;
          } else if ("In Progress".equals(status)) {
            inProgress++;
          } else {
            pending++;
          }
        }

        writer.println();
        writer.println("# Export Summary:");
        writer.println("# Total tasks exported: " + total);
        writer.println("# Completed: " + completed + ", In Progress: " + inProgress + ", Pending: " + pending);

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

  /**
   * Import tasks từ file text với option replace/append
   */
  private void importTasks() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Import Tasks");
    fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

    int choice = fileChooser.showOpenDialog(this);

    if (choice == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();

      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        ArrayList<String[]> importedTasks = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
          if (line.trim().startsWith("#") || line.trim().isEmpty()) {
            continue;
          }

          String[] parts = line.split("\\|");

          if (parts.length >= 4) {
            parts[0] = parts[0].replace("\\|", "|");
            importedTasks.add(new String[] { parts[0], parts[1], parts[2], parts[3] });
          }
        }

        if (importedTasks.isEmpty()) {
          JOptionPane.showMessageDialog(this,
              "No valid tasks found in the selected file.",
              "Import Failed",
              JOptionPane.WARNING_MESSAGE);
          return;
        }

        Object[] options = { "Clear Current Tasks", "Keep Current Tasks" };
        int response = JOptionPane.showOptionDialog(this,
            "Found " + importedTasks.size() + " tasks to import.\nDo you want to clear current tasks or keep them?",
            "Import Options",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1]);

        if (response == JOptionPane.YES_OPTION) {
          while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
          }
          pinnedTaskRows.clear();
        }

        for (String[] task : importedTasks) {
          tableModel.addRow(task);
        }

        updateTaskCount();

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

  private void createUIComponents() {
    taskTable = new TaskTable();
  }

  public TaskManager getTaskManager() {
    return this.taskManager;
  }

  private void initializeTaskData() {
    refreshTaskTable();
  }

  /**
   * Manual save data (Ctrl+S)
   */
  private void saveData() {
    boolean saved = taskManager.saveTasksToFile();
    String message = saved ? "Đã lưu dữ liệu thành công" : "Không thể lưu dữ liệu. Vui lòng thử lại.";
    int messageType = saved ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
    JOptionPane.showMessageDialog(this, message, saved ? "Thông báo" : "Lỗi", messageType);
  }

  /**
   * Reload data from file với confirmation
   */
  private void reloadData() {
    if (taskManager.hasUnsavedChanges()) {
      int option = JOptionPane.showConfirmDialog(this,
          "Bạn có thay đổi chưa được lưu. Tải lại sẽ xóa các thay đổi này. Bạn có muốn tiếp tục không?",
          "Xác nhận tải lại", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

      if (option != JOptionPane.YES_OPTION)
        return;
    }

    boolean loaded = taskManager.loadTasksFromFile();
    if (loaded) {
      refreshTaskTable();
      JOptionPane.showMessageDialog(this, "Đã tải dữ liệu thành công", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    } else {
      JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu hoặc file dữ liệu không tồn tại.", "Lỗi",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  public TaskController getTaskController() {
    return taskController;
  }

  private void setupSaveShortcut() {
    KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);

    getRootPane().registerKeyboardAction(e -> {
      System.out.println("MainWindow: Nhận phím Ctrl+S - Lưu từ UI");

      // Lưu trực tiếp từ UI giống như export
      boolean saved = taskController.saveTasksFromUI();

      if (saved) {
        JOptionPane.showMessageDialog(this,
            "Đã lưu dữ liệu thành công vào resources/data/data.txt!",
            "Lưu thành công",
            JOptionPane.INFORMATION_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(this,
            "Không thể lưu dữ liệu!",
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);
      }
    }, ctrlS, JComponent.WHEN_IN_FOCUSED_WINDOW);
  }
}