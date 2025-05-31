package com.todoapp.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Calendar dialog hiển thị tasks theo ngày
 * Cung cấp giao diện calendar với task indicators và task details
 */
public class DatePickerDialog extends JDialog {
  // ==================== CONSTANTS ====================
  private static final Color BACKGROUND_COLOR = new Color(245, 245, 250);
  private static final Color CALENDAR_BG = new Color(250, 250, 255);
  private static final Color TODAY_COLOR = new Color(230, 230, 250);
  private static final Color BORDER_COLOR = new Color(200, 200, 220);

  private static final Color HIGH_PRIORITY_COLOR = new Color(255, 80, 80);
  private static final Color INCOMPLETE_COLOR = new Color(255, 180, 0);
  private static final Color COMPLETED_COLOR = new Color(100, 180, 100);

  private static final String[] WEEKDAYS = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  // ==================== COMPONENTS ====================
  private JPanel calendarPanel;
  private JLabel monthYearLabel;
  private JLabel selectedDateLabel;
  private JButton prevButton, nextButton;
  private JList<String> taskList;
  private DefaultListModel<String> taskListModel;

  // ==================== DATA ====================
  private final Calendar calendar;
  private final DefaultTableModel tableModel;
  private final Map<String, List<TaskInfo>> dateTaskMap;

  /**
   * Task information container
   */
  private static class TaskInfo {
    final String name;
    final String priority;
    final String status;

    TaskInfo(String name, String priority, String status) {
      this.name = name;
      this.priority = priority;
      this.status = status;
    }
  }

  public DatePickerDialog(JFrame parent, DefaultTableModel tableModel) {
    super(parent, "Calendar View", true);
    this.tableModel = tableModel;
    this.calendar = Calendar.getInstance();
    this.dateTaskMap = new HashMap<>();

    initializeDialog();
    buildUI();
    setupEventHandlers();
    loadTaskData();
    updateCalendar();
  }

  // ==================== INITIALIZATION ====================

  /**
   * Thiết lập cơ bản cho dialog
   */
  private void initializeDialog() {
    setSize(900, 650); // Tăng size để hiển thị tốt hơn
    setLocationRelativeTo(getParent());
    setLayout(new BorderLayout(10, 10));
    getContentPane().setBackground(BACKGROUND_COLOR);
    setResizable(true); // Cho phép resize
  }

  /**
   * Xây dựng giao diện người dùng
   */
  private void buildUI() {
    JPanel mainPanel = createMainPanel();
    JSplitPane splitPane = createSplitPane();
    JPanel buttonPanel = createButtonPanel();

    mainPanel.add(splitPane, BorderLayout.CENTER);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    add(mainPanel);
  }

  /**
   * Tạo panel chính
   */
  private JPanel createMainPanel() {
    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    return panel;
  }

  /**
   * Tạo split pane cho calendar và task list
   */
  private JSplitPane createSplitPane() {
    JSplitPane splitPane = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        createCalendarPanel(),
        createTaskPanel());

    splitPane.setDividerLocation(550); // Tăng space cho calendar
    splitPane.setOneTouchExpandable(true);
    splitPane.setContinuousLayout(true);
    splitPane.setResizeWeight(0.6); // Calendar có 60% width

    return splitPane;
  }

  // ==================== CALENDAR PANEL ====================

  /**
   * Tạo panel calendar
   */
  private JPanel createCalendarPanel() {
    JPanel container = new JPanel(new BorderLayout(5, 5));
    container.setBorder(createTitledBorder("Calendar"));
    container.setBackground(BACKGROUND_COLOR);

    // Navigation panel
    JPanel navPanel = createNavigationPanel();

    // Weekday header
    JPanel weekdayPanel = createWeekdayHeader();

    // Calendar grid - SỬA ĐỔI CHÍNH TẠI ĐÂY
    calendarPanel = new JPanel(new GridLayout(6, 7, 2, 2));
    calendarPanel.setBackground(CALENDAR_BG);
    calendarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    // Đặt preferred size cho calendar
    calendarPanel.setPreferredSize(new java.awt.Dimension(420, 240));

    // Combine weekday và calendar vào một panel
    JPanel calendarContainer = new JPanel(new BorderLayout(0, 2));
    calendarContainer.add(weekdayPanel, BorderLayout.NORTH);
    calendarContainer.add(calendarPanel, BorderLayout.CENTER);

    container.add(navPanel, BorderLayout.NORTH);
    container.add(calendarContainer, BorderLayout.CENTER);

    return container;
  }

  /**
   * Tạo panel navigation
   */
  private JPanel createNavigationPanel() {
    JPanel panel = new JPanel(new BorderLayout(5, 0));
    panel.setBackground(new Color(240, 240, 245));
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    // Navigation buttons
    prevButton = createNavButton("◀ Previous");
    nextButton = createNavButton("Next ▶");

    // Month/Year label
    monthYearLabel = new JLabel("", SwingConstants.CENTER);
    monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    monthYearLabel.setForeground(new Color(50, 50, 120));

    panel.add(prevButton, BorderLayout.WEST);
    panel.add(monthYearLabel, BorderLayout.CENTER);
    panel.add(nextButton, BorderLayout.EAST);

    return panel;
  }

  /**
   * Tạo nút navigation
   */
  private JButton createNavButton(String text) {
    JButton button = new JButton(text);
    button.setFocusPainted(false);
    button.setFont(new Font("SansSerif", Font.BOLD, 12));
    return button;
  }

  /**
   * Tạo header weekday với size cố định
   */
  private JPanel createWeekdayHeader() {
    JPanel panel = new JPanel(new GridLayout(1, 7, 2, 2));
    panel.setBackground(new Color(230, 230, 240));
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.setPreferredSize(new java.awt.Dimension(420, 30));

    for (String day : WEEKDAYS) {
      JLabel label = new JLabel(day, SwingConstants.CENTER);
      label.setFont(new Font("SansSerif", Font.BOLD, 12));
      label.setOpaque(true);
      label.setBackground(new Color(220, 220, 235));
      label.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
      panel.add(label);
    }

    return panel;
  }

  // ==================== TASK PANEL ====================

  /**
   * Tạo panel task details
   */
  private JPanel createTaskPanel() {
    JPanel panel = new JPanel(new BorderLayout(5, 10));
    panel.setBorder(createTitledBorder("Tasks for Selected Date"));

    // Selected date label
    selectedDateLabel = new JLabel("No date selected", SwingConstants.CENTER);
    selectedDateLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    selectedDateLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

    // Task list
    taskListModel = new DefaultListModel<>();
    taskList = new JList<>(taskListModel);
    taskList.setCellRenderer(new TaskListCellRenderer());
    taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    taskList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JScrollPane scrollPane = new JScrollPane(taskList);
    scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 235)));

    panel.add(selectedDateLabel, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Tạo panel buttons
   */
  private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.setBackground(BACKGROUND_COLOR);

    JButton todayButton = createButton("Go to Today");
    JButton closeButton = createButton("Close");

    panel.add(todayButton);
    panel.add(closeButton);

    return panel;
  }

  /**
   * Tạo button với style nhất quán
   */
  private JButton createButton(String text) {
    JButton button = new JButton(text);
    button.setFocusPainted(false);
    button.setFont(new Font("SansSerif", Font.BOLD, 12));
    return button;
  }

  /**
   * Tạo titled border
   */
  private CompoundBorder createTitledBorder(String title) {
    return new CompoundBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14)),
        BorderFactory.createEmptyBorder(10, 10, 10, 10));
  }

  // ==================== EVENT HANDLERS ====================

  /**
   * Thiết lập event handlers
   */
  private void setupEventHandlers() {
    prevButton.addActionListener(e -> navigateMonth(-1));
    nextButton.addActionListener(e -> navigateMonth(1));

    // Tìm button "Go to Today" và "Close" từ button panel
    JPanel buttonPanel = (JPanel) ((JPanel) getContentPane().getComponent(0))
        .getComponent(1); // mainPanel -> buttonPanel

    JButton todayButton = (JButton) buttonPanel.getComponent(0);
    JButton closeButton = (JButton) buttonPanel.getComponent(1);

    todayButton.addActionListener(e -> goToToday());
    closeButton.addActionListener(e -> dispose());
  }

  /**
   * Navigate tháng
   */
  private void navigateMonth(int direction) {
    calendar.add(Calendar.MONTH, direction);
    updateCalendar();
  }

  /**
   * Về ngày hôm nay
   */
  private void goToToday() {
    calendar.setTime(new Date());
    updateCalendar();
  }

  // ==================== DATA OPERATIONS ====================

  /**
   * Load task data từ table model
   */
  private void loadTaskData() {
    dateTaskMap.clear();

    for (int i = 0; i < tableModel.getRowCount(); i++) {
      String taskName = (String) tableModel.getValueAt(i, 0);
      String dueDate = (String) tableModel.getValueAt(i, 1);
      String priority = (String) tableModel.getValueAt(i, 2);
      String status = (String) tableModel.getValueAt(i, 3);

      if (isValidDate(dueDate)) {
        TaskInfo task = new TaskInfo(taskName, priority, status);
        dateTaskMap.computeIfAbsent(dueDate, k -> new ArrayList<>()).add(task);
      }
    }
  }

  /**
   * Kiểm tra date hợp lệ
   */
  private boolean isValidDate(String dateString) {
    if (dateString == null || dateString.trim().isEmpty()) {
      return false;
    }

    try {
      DATE_FORMAT.parse(dateString);
      return true;
    } catch (ParseException e) {
      return false;
    }
  }

  // ==================== CALENDAR UPDATE ====================

  /**
   * Cập nhật calendar display
   */
  private void updateCalendar() {
    calendarPanel.removeAll();

    updateMonthYearLabel();
    addEmptyStartCells();
    addDayButtons();
    addEmptyEndCells();

    calendarPanel.revalidate();
    calendarPanel.repaint();
  }

  /**
   * Cập nhật label tháng/năm
   */
  private void updateMonthYearLabel() {
    SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy");
    monthYearLabel.setText(formatter.format(calendar.getTime()));
  }

  /**
   * Thêm các ô trống đầu tháng
   */
  private void addEmptyStartCells() {
    Calendar temp = (Calendar) calendar.clone();
    temp.set(Calendar.DAY_OF_MONTH, 1);
    int firstDay = temp.get(Calendar.DAY_OF_WEEK) - 1;

    for (int i = 0; i < firstDay; i++) {
      calendarPanel.add(createEmptyCell());
    }
  }

  /**
   * Thêm các nút ngày
   */
  private void addDayButtons() {
    Calendar temp = (Calendar) calendar.clone();
    temp.set(Calendar.DAY_OF_MONTH, 1);
    int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
    int today = getTodayIfSameMonth();

    for (int day = 1; day <= daysInMonth; day++) {
      JButton dayButton = createDayButton(day, day == today, temp);
      calendarPanel.add(dayButton);
    }
  }

  /**
   * Lấy ngày hôm nay nếu cùng tháng/năm
   */
  private int getTodayIfSameMonth() {
    Calendar today = Calendar.getInstance();
    boolean sameMonth = today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
        today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH);
    return sameMonth ? today.get(Calendar.DAY_OF_MONTH) : -1;
  }

  /**
   * Tạo button cho ngày với size cố định
   */
  private JButton createDayButton(int day, boolean isToday, Calendar temp) {
    temp.set(Calendar.DAY_OF_MONTH, day);
    String dateString = DATE_FORMAT.format(temp.getTime());
    List<TaskInfo> tasks = dateTaskMap.get(dateString);

    JButton button;
    if (tasks != null && !tasks.isEmpty()) {
      button = createTaskDayButton(day, isToday, dateString, tasks);
    } else {
      button = createSimpleDayButton(day, isToday, dateString);
    }

    // ĐẢM BẢO TẤT CẢ BUTTONS CÓ CÙNG SIZE
    button.setPreferredSize(new java.awt.Dimension(55, 35));
    button.setMinimumSize(new java.awt.Dimension(55, 35));
    button.setMaximumSize(new java.awt.Dimension(55, 35));

    return button;
  }

  /**
   * Tạo button cho ngày có tasks
   */
  private JButton createTaskDayButton(int day, boolean isToday, String dateString, List<TaskInfo> tasks) {
    TaskDayButton button = new TaskDayButton(day, tasks);
    styleDayButton(button, isToday, true);
    button.addActionListener(e -> showTasksForDate(dateString));
    return button;
  }

  /**
   * Tạo button cho ngày không có tasks
   */
  private JButton createSimpleDayButton(int day, boolean isToday, String dateString) {
    JButton button = new JButton(String.valueOf(day));
    styleDayButton(button, isToday, false);
    button.addActionListener(e -> showTasksForDate(dateString));
    return button;
  }

  /**
   * Style cho day button
   */
  private void styleDayButton(JButton button, boolean isToday, boolean hasTasks) {
    button.setMargin(new Insets(2, 2, 2, 2));
    button.setFocusPainted(false);

    if (isToday) {
      button.setBackground(TODAY_COLOR);
      button.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 220), 2));
      button.setFont(new Font("SansSerif", Font.BOLD, 12));
    } else {
      button.setBackground(CALENDAR_BG);
      button.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 235)));
      button.setFont(new Font("SansSerif", hasTasks ? Font.BOLD : Font.PLAIN, 12));
    }

    if (hasTasks) {
      button.setForeground(new Color(50, 50, 180));
    }
  }

  /**
   * Thêm các ô trống cuối tháng
   */
  private void addEmptyEndCells() {
    int totalCells = 42; // 6 rows * 7 days
    int usedCells = calendarPanel.getComponentCount();
    int remaining = totalCells - usedCells;

    for (int i = 0; i < remaining; i++) {
      calendarPanel.add(createEmptyCell());
    }
  }

  /**
   * Tạo ô trống với size cố định
   */
  private JPanel createEmptyCell() {
    JPanel panel = new JPanel();
    panel.setBackground(CALENDAR_BG);
    panel.setPreferredSize(new java.awt.Dimension(55, 35));
    panel.setMinimumSize(new java.awt.Dimension(55, 35));
    panel.setMaximumSize(new java.awt.Dimension(55, 35));
    panel.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 245)));
    return panel;
  }

  // ==================== TASK DISPLAY ====================

  /**
   * Hiển thị tasks cho ngày được chọn
   */
  private void showTasksForDate(String dateString) {
    selectedDateLabel.setText("Tasks for: " + dateString);
    taskListModel.clear();

    List<TaskInfo> tasks = dateTaskMap.get(dateString);
    if (tasks != null && !tasks.isEmpty()) {
      for (TaskInfo task : tasks) {
        String displayText = String.format("%s (%s - %s)",
            task.name, task.priority, task.status);
        taskListModel.addElement(displayText);
      }
    } else {
      taskListModel.addElement("No tasks scheduled for this date");
    }
  }

  // ==================== CUSTOM COMPONENTS ====================

  /**
   * Custom button cho ngày có tasks với indicator
   */
  private class TaskDayButton extends JButton {
    private final List<TaskInfo> tasks;
    private final Color indicatorColor;

    TaskDayButton(int day, List<TaskInfo> tasks) {
      super(String.valueOf(day));
      this.tasks = tasks;
      this.indicatorColor = getIndicatorColor(tasks);
    }

    private Color getIndicatorColor(List<TaskInfo> tasks) {
      boolean hasHigh = tasks.stream().anyMatch(t -> "High".equals(t.priority));
      boolean hasIncomplete = tasks.stream().anyMatch(t -> !"Completed".equals(t.status));

      if (hasHigh)
        return HIGH_PRIORITY_COLOR;
      if (hasIncomplete)
        return INCOMPLETE_COLOR;
      return COMPLETED_COLOR;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);

      // Vẽ indicator dot
      int dotSize = 8;
      int x = getWidth() - dotSize - 3;
      int y = 3;

      g.setColor(indicatorColor);
      g.fillOval(x, y, dotSize, dotSize);
    }
  }

  /**
   * Custom cell renderer cho task list
   */
  private class TaskListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {

      JLabel label = (JLabel) super.getListCellRendererComponent(
          list, value, index, isSelected, cellHasFocus);

      if (value != null) {
        String text = value.toString();

        // Color coding theo priority
        if (text.contains("(High")) {
          label.setBackground(isSelected ? new Color(255, 150, 150) : new Color(255, 220, 220));
        } else if (text.contains("(Medium")) {
          label.setBackground(isSelected ? new Color(255, 200, 150) : new Color(255, 235, 220));
        } else if (text.contains("(Low")) {
          label.setBackground(isSelected ? new Color(200, 255, 200) : new Color(230, 255, 230));
        }

        // Icon theo status
        if (text.contains("- Completed")) {
          label.setText("✓ " + text);
        } else if (text.contains("- In Progress")) {
          label.setText("⏳ " + text);
        } else if (text.contains("- Pending")) {
          label.setText("⌛ " + text);
        }
      }

      return label;
    }
  }

  // ==================== PUBLIC API ====================

  /**
   * Navigate đến ngày cụ thể
   */
  public void navigateToDate(String dateString) {
    try {
      Date date = DATE_FORMAT.parse(dateString);
      calendar.setTime(date);
      updateCalendar();
      showTasksForDate(dateString);
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
}