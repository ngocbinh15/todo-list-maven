package com.todoapp.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
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

public class DatePickerDialog extends JDialog {
  private JPanel calendarPanel;
  private JLabel monthYearLabel;
  private Calendar calendar;
  private SimpleDateFormat dateFormat;
  private JButton prevButton, nextButton;
  private DefaultTableModel tableModel;
  private Map<String, List<TaskInfo>> dateTaskMap;
  private JList<String> taskList;
  private DefaultListModel<String> taskListModel;
  private JLabel selectedDateLabel;
  private ArrayList<JButton> dayButtons;

  // Class to store task information
  private class TaskInfo {
    String name;
    String priority;
    String status;

    public TaskInfo(String name, String priority, String status) {
      this.name = name;
      this.priority = priority;
      this.status = status;
    }
  }

  public DatePickerDialog(JFrame parent, DefaultTableModel tableModel) {
    super(parent, "Calendar View", true);
    this.tableModel = tableModel;

    // Initialize date and calendar objects
    calendar = Calendar.getInstance();
    dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    dateTaskMap = new HashMap<>();

    // Set dialog properties
    setSize(800, 600);
    setLocationRelativeTo(parent);
    setLayout(new BorderLayout(10, 10));
    getContentPane().setBackground(new Color(245, 245, 250));

    // Create main panels
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    // Create calendar panel with header
    JPanel calendarContainer = new JPanel(new BorderLayout(5, 10));
    calendarContainer.setBorder(new CompoundBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 220), 1, true),
            "Calendar",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14)),
        BorderFactory.createEmptyBorder(10, 10, 10, 10)));

    // Create navigation panel
    JPanel navigationPanel = new JPanel(new BorderLayout(5, 0));
    navigationPanel.setBackground(new Color(240, 240, 245));
    navigationPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    // Month navigation buttons
    prevButton = new JButton("◀ Previous");
    prevButton.setFocusPainted(false);
    prevButton.setFont(new Font("SansSerif", Font.BOLD, 12));

    nextButton = new JButton("Next ▶");
    nextButton.setFocusPainted(false);
    nextButton.setFont(new Font("SansSerif", Font.BOLD, 12));

    // Month year label
    monthYearLabel = new JLabel("", SwingConstants.CENTER);
    monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    monthYearLabel.setForeground(new Color(50, 50, 120));

    navigationPanel.add(prevButton, BorderLayout.WEST);
    navigationPanel.add(monthYearLabel, BorderLayout.CENTER);
    navigationPanel.add(nextButton, BorderLayout.EAST);

    // Create weekday header
    JPanel weekDaysPanel = new JPanel(new GridLayout(1, 7));
    weekDaysPanel.setBackground(new Color(230, 230, 240));

    String[] weekdays = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
    for (String day : weekdays) {
      JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
      dayLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
      dayLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      weekDaysPanel.add(dayLabel);
    }

    // Create calendar grid
    calendarPanel = new JPanel(new GridLayout(6, 7, 5, 5));
    calendarPanel.setBackground(new Color(250, 250, 255));

    // Create task details panel
    JPanel taskPanel = new JPanel(new BorderLayout(5, 10));
    taskPanel.setBorder(new CompoundBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 220), 1, true),
            "Tasks for Selected Date",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14)),
        BorderFactory.createEmptyBorder(10, 10, 10, 10)));

    // Selected date indicator
    selectedDateLabel = new JLabel("No date selected", SwingConstants.CENTER);
    selectedDateLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    selectedDateLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

    // Task list
    taskListModel = new DefaultListModel<>();
    taskList = new JList<>(taskListModel);
    taskList.setCellRenderer(new TaskListCellRenderer());
    taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    taskList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JScrollPane taskScrollPane = new JScrollPane(taskList);
    taskScrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 235)));

    taskPanel.add(selectedDateLabel, BorderLayout.NORTH);
    taskPanel.add(taskScrollPane, BorderLayout.CENTER);

    // Close button
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.setBackground(new Color(245, 245, 250));

    JButton closeButton = new JButton("Close");
    closeButton.setFocusPainted(false);
    closeButton.setFont(new Font("SansSerif", Font.BOLD, 12));
    closeButton.addActionListener(e -> dispose());

    JButton todayButton = new JButton("Go to Today");
    todayButton.setFocusPainted(false);
    todayButton.setFont(new Font("SansSerif", Font.BOLD, 12));
    todayButton.addActionListener(e -> {
      calendar.setTime(new Date());
      updateCalendar();
    });

    buttonPanel.add(todayButton);
    buttonPanel.add(closeButton);

    // Add everything to the calendar container
    JPanel calHeader = new JPanel(new BorderLayout());
    calHeader.add(navigationPanel, BorderLayout.NORTH);
    calHeader.add(weekDaysPanel, BorderLayout.SOUTH);

    calendarContainer.add(calHeader, BorderLayout.NORTH);
    calendarContainer.add(calendarPanel, BorderLayout.CENTER);

    // Split pane for calendar and task list
    JSplitPane splitPane = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        calendarContainer,
        taskPanel);
    splitPane.setDividerLocation(500);
    splitPane.setOneTouchExpandable(true);
    splitPane.setContinuousLayout(true);

    mainPanel.add(splitPane, BorderLayout.CENTER);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel);

    // Add listeners
    prevButton.addActionListener(e -> {
      calendar.add(Calendar.MONTH, -1);
      updateCalendar();
    });

    nextButton.addActionListener(e -> {
      calendar.add(Calendar.MONTH, 1);
      updateCalendar();
    });

    // Process tasks from the table model
    processTableTasks();

    // Initialize calendar display
    updateCalendar();
  }

  private void processTableTasks() {
    dateTaskMap.clear();

    for (int i = 0; i < tableModel.getRowCount(); i++) {
      String taskName = (String) tableModel.getValueAt(i, 0);
      String dueDate = (String) tableModel.getValueAt(i, 1);
      String priority = (String) tableModel.getValueAt(i, 2);
      String status = (String) tableModel.getValueAt(i, 3);

      // Check if the date is valid
      if (dueDate != null && !dueDate.trim().isEmpty()) {
        try {
          // Ensure date is valid by parsing it
          dateFormat.parse(dueDate);

          // Add task to the map
          TaskInfo taskInfo = new TaskInfo(taskName, priority, status);
          dateTaskMap.computeIfAbsent(dueDate, k -> new ArrayList<>()).add(taskInfo);
        } catch (ParseException e) {
          // Skip invalid dates
        }
      }
    }
  }

  private void updateCalendar() {
    calendarPanel.removeAll();
    dayButtons = new ArrayList<>();

    // Update month/year label
    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy");
    monthYearLabel.setText(monthYearFormat.format(calendar.getTime()));

    // Get current month's first day and number of days
    Calendar tempCalendar = (Calendar) calendar.clone();
    tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
    int firstDayOfMonth = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1;
    int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

    // Get current date for highlighting today
    Calendar todayCal = Calendar.getInstance();
    boolean sameMonthYear = todayCal.get(Calendar.YEAR) == tempCalendar.get(Calendar.YEAR) &&
        todayCal.get(Calendar.MONTH) == tempCalendar.get(Calendar.MONTH);
    int today = sameMonthYear ? todayCal.get(Calendar.DAY_OF_MONTH) : -1;

    // Add empty spaces for days before the 1st of the month
    for (int i = 0; i < firstDayOfMonth; i++) {
      JPanel emptyPanel = new JPanel();
      emptyPanel.setBackground(new Color(250, 250, 255));
      calendarPanel.add(emptyPanel);
    }

    // Add day buttons
    for (int day = 1; day <= daysInMonth; day++) {
      final int currentDay = day;

      // Create button for day
      JButton dayButton = new JButton(String.valueOf(day));
      dayButton.setMargin(new Insets(2, 2, 2, 2));
      dayButton.setFocusPainted(false);

      // Style the button
      if (day == today) {
        // Highlight today
        dayButton.setBackground(new Color(230, 230, 250));
        dayButton.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 220), 2));
        dayButton.setFont(new Font("SansSerif", Font.BOLD, 12));
      } else {
        dayButton.setBackground(new Color(250, 250, 255));
        dayButton.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 235)));
        dayButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
      }

      // Check if date has tasks
      tempCalendar.set(Calendar.DAY_OF_MONTH, day);
      String dateString = dateFormat.format(tempCalendar.getTime());

      List<TaskInfo> tasksForDay = dateTaskMap.get(dateString);
      if (tasksForDay != null && !tasksForDay.isEmpty()) {
        // Highlight days with tasks
        dayButton.setForeground(new Color(50, 50, 180));

        // Add indicator for tasks
        int taskCount = tasksForDay.size();
        String taskCountStr = taskCount > 0 ? " (" + taskCount + ")" : "";

        // Add colored dot or badge based on task priority
        boolean hasHighPriority = false;
        boolean hasUncompletedTask = false;

        for (TaskInfo task : tasksForDay) {
          if ("High".equals(task.priority)) {
            hasHighPriority = true;
          }
          if (!"Completed".equals(task.status)) {
            hasUncompletedTask = true;
          }
        }

        // Lưu giá trị final cho anonymous class
        final boolean finalHasHighPriority = hasHighPriority;
        final boolean finalHasUncompletedTask = hasUncompletedTask;

        // Create a custom button with colored indicator
        dayButton = new JButton(new AbstractAction(String.valueOf(day)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            showTasksForDate(dateString);
          }
        }) {
          @Override
          protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Paint task indicator dot
            int dotSize = 8;
            int x = getWidth() - dotSize - 3;
            int y = 3;

            Color dotColor = finalHasHighPriority ? new Color(255, 80, 80) : // Red for high priority
                finalHasUncompletedTask ? new Color(255, 180, 0) : // Orange for incomplete
                    new Color(100, 180, 100); // Green for all completed

            g.setColor(dotColor);
            g.fillOval(x, y, dotSize, dotSize);
          }
        };

        // Restore styling
        if (day == today) {
          dayButton.setBackground(new Color(230, 230, 250));
          dayButton.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 220), 2));
          dayButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        } else {
          dayButton.setBackground(new Color(250, 250, 255));
          dayButton.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 235)));
          dayButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        }

        // Make font bold for days with tasks
        dayButton.setFont(new Font(dayButton.getFont().getName(), Font.BOLD, dayButton.getFont().getSize()));
      }

      // Add action to show tasks for this day
      final Calendar calendarRef = calendar; // Tạo bản sao final
      dayButton.addActionListener(e -> {
        Calendar selectedCal = (Calendar) calendarRef.clone();
        selectedCal.set(Calendar.DAY_OF_MONTH, currentDay);
        showTasksForDate(dateString);
      });

      calendarPanel.add(dayButton);
      dayButtons.add(dayButton);
    }

    // Add empty spaces for days after the end of the month
    int totalCells = 42; // 6 rows * 7 days
    int remainingCells = totalCells - (firstDayOfMonth + daysInMonth);
    for (int i = 0; i < remainingCells; i++) {
      JPanel emptyPanel = new JPanel();
      emptyPanel.setBackground(new Color(250, 250, 255));
      calendarPanel.add(emptyPanel);
    }

    calendarPanel.revalidate();
    calendarPanel.repaint();
  }

  private void showTasksForDate(String dateString) {
    selectedDateLabel.setText("Tasks for: " + dateString);
    taskListModel.clear();

    List<TaskInfo> tasksForDay = dateTaskMap.get(dateString);
    if (tasksForDay != null) {
      for (TaskInfo task : tasksForDay) {
        taskListModel.addElement(task.name + " (" + task.priority + " - " + task.status + ")");
      }
    }

    if (taskListModel.isEmpty()) {
      taskListModel.addElement("No tasks scheduled for this date");
    }
  }

  // Custom cell renderer for task list to show priority colors
  private class TaskListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {

      JLabel label = (JLabel) super.getListCellRendererComponent(
          list, value, index, isSelected, cellHasFocus);

      if (value != null) {
        String text = value.toString();

        if (text.contains("(High")) {
          label.setBackground(isSelected ? new Color(255, 150, 150) : new Color(255, 220, 220));
        } else if (text.contains("(Medium")) {
          label.setBackground(isSelected ? new Color(255, 200, 150) : new Color(255, 235, 220));
        } else if (text.contains("(Low")) {
          label.setBackground(isSelected ? new Color(200, 255, 200) : new Color(230, 255, 230));
        }

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

  // For future expansion: Method to navigate to specific date
  public void navigateToDate(String dateString) {
    try {
      Date date = dateFormat.parse(dateString);
      calendar.setTime(date);
      updateCalendar();
      showTasksForDate(dateString);
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
}