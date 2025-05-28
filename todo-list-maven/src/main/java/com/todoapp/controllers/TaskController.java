package com.todoapp.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.todoapp.components.TaskTable;
import com.todoapp.views.DatePickerDialog;
import com.todoapp.views.MainWindow;
import com.todoapp.views.TaskDialog;

public class TaskController {
  private MainWindow mainWindow;
  private TaskTable taskTable;
  private DefaultTableModel tableModel;
  private LinkedHashSet<Integer> pinnedTaskRows;
  private TableRowSorter<DefaultTableModel> rowSorter;

  public TaskController(MainWindow mainWindow, TaskTable taskTable) {
    this.mainWindow = mainWindow;
    this.taskTable = taskTable;
    this.tableModel = taskTable.getTableModel();
    this.pinnedTaskRows = new LinkedHashSet<>();

    // Khởi tạo RowSorter nếu cần
    if (taskTable.getRowSorter() != null &&
        taskTable.getRowSorter() instanceof TableRowSorter) {
      rowSorter = (TableRowSorter<DefaultTableModel>) taskTable.getRowSorter();
    }

    // Thêm các listeners để bắt sự kiện từ TaskTable
    setupListeners();
  }

  private void setupListeners() {
    // Bắt sự kiện từ TaskTable
    taskTable.addPropertyChangeListener(evt -> {
      if ("deleteTask".equals(evt.getPropertyName())) {
        int row = (Integer) evt.getNewValue();
        deleteTask(row);
      } else if ("editTask".equals(evt.getPropertyName())) {
        int row = (Integer) evt.getNewValue();
        editTask(row);
      } else if ("togglePin".equals(evt.getPropertyName())) {
        int modelRow = (Integer) evt.getNewValue();
        togglePinTask(modelRow);
      }
    });
  }

  /**
   * Thêm task mới
   */
  public void addTask() {
    TaskDialog dialog = new TaskDialog(mainWindow, "Add New Task", "", "", "Medium", "Pending");
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      // Lấy dữ liệu từ dialog
      String name = dialog.getTaskName();
      String dueDate = dialog.getDueDate();
      String priority = dialog.getPriority();
      String status = dialog.getStatus();

      // Thêm vào model
      tableModel.addRow(new Object[] { name, dueDate, priority, status });

      // Cập nhật số lượng task
      mainWindow.updateTaskCount();

      // Hiển thị thông báo thành công
      JOptionPane.showMessageDialog(mainWindow,
          "Task added successfully!",
          "Success",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Sửa task
   */
  public void editTask(int viewRow) {
    if (viewRow >= 0) {
      // Chuyển đổi từ view row sang model row
      int modelRow = taskTable.convertRowIndexToModel(viewRow);

      // Lấy dữ liệu hiện tại
      String currentTask = (String) tableModel.getValueAt(modelRow, 0);
      String currentDate = (String) tableModel.getValueAt(modelRow, 1);
      String currentPriority = (String) tableModel.getValueAt(modelRow, 2);
      String currentStatus = (String) tableModel.getValueAt(modelRow, 3);

      // Hiển thị dialog
      TaskDialog dialog = new TaskDialog(mainWindow, "Edit Task",
          currentTask, currentDate, currentPriority, currentStatus);
      dialog.setVisible(true);

      // Cập nhật dữ liệu nếu người dùng xác nhận
      if (dialog.isConfirmed()) {
        tableModel.setValueAt(dialog.getTaskName(), modelRow, 0);
        tableModel.setValueAt(dialog.getDueDate(), modelRow, 1);
        tableModel.setValueAt(dialog.getPriority(), modelRow, 2);
        tableModel.setValueAt(dialog.getStatus(), modelRow, 3);

        // Cập nhật số lượng task
        mainWindow.updateTaskCount();
      }
    } else {
      JOptionPane.showMessageDialog(mainWindow,
          "Please select a task to edit.",
          "No Task Selected", JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Xóa task
   */
  public void deleteTask(int viewRow) {
    if (viewRow >= 0) {
      // Chuyển đổi từ view row sang model row
      int modelRow = taskTable.convertRowIndexToModel(viewRow);
      String taskName = (String) tableModel.getValueAt(modelRow, 0);

      // Xác nhận xóa
      int confirm = JOptionPane.showConfirmDialog(mainWindow,
          "Are you sure you want to delete \"" + taskName + "\"?",
          "Confirm Delete", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);

      if (confirm == JOptionPane.YES_OPTION) {
        // Xóa task khỏi danh sách ghim nếu có
        pinnedTaskRows.remove(modelRow);

        // Cập nhật lại các mã hàng cho các task bị ảnh hưởng
        for (Integer pinnedRow : new ArrayList<>(pinnedTaskRows)) {
          if (pinnedRow > modelRow) {
            pinnedTaskRows.remove(pinnedRow);
            pinnedTaskRows.add(pinnedRow - 1);
          }
        }

        // Xóa task
        tableModel.removeRow(modelRow);

        // Cập nhật số lượng task
        mainWindow.updateTaskCount();
      }
    } else {
      JOptionPane.showMessageDialog(mainWindow,
          "Please select a task to delete.",
          "No Task Selected", JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Ghim/bỏ ghim task
   */
  public void togglePinTask(int modelRow) {
    if (pinnedTaskRows.contains(modelRow)) {
      pinnedTaskRows.remove(modelRow);
    } else {
      pinnedTaskRows.add(modelRow);
    }

    // Cập nhật bảng hiển thị
    taskTable.updatePinnedTasks(pinnedTaskRows);

    // Cập nhật sắp xếp
    if (rowSorter != null) {
      rowSorter.sort();
    }
  }

  /**
   * Sắp xếp tasks theo cột
   */
  public void sortTasks(int column) {
    if (rowSorter != null) {
      rowSorter.toggleSortOrder(column);
    }
  }

  /**
   * Hiển thị dialog sắp xếp task
   */
  public void showSortDialog() {
    String[] options = { "Task Name", "Due Date", "Priority", "Status" };
    String selection = (String) JOptionPane.showInputDialog(mainWindow,
        "Sort by which column?", "Sort Tasks",
        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

    if (selection != null) {
      int column = Arrays.asList(options).indexOf(selection);
      sortTasks(column);
    }
  }

  /**
   * Filter tasks theo từ khóa
   */
  public void filterTasks(String keyword) {
    taskTable.filterTasks(keyword);
  }

  /**
   * Export tasks ra file CSV
   */
  public void exportTasks() {
    // Configure file chooser
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Tasks");
    fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

    // Show save dialog
    int choice = fileChooser.showSaveDialog(mainWindow);

    if (choice == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      String path = file.getAbsolutePath();

      // Add .csv extension if not present
      if (!path.toLowerCase().endsWith(".csv")) {
        file = new File(path + ".csv");
      }

      try (PrintWriter writer = new PrintWriter(file)) {
        // Write CSV header
        writer.println("Task,DueDate,Priority,Status,Pinned");

        // Write tasks
        for (int i = 0; i < tableModel.getRowCount(); i++) {
          String task = (String) tableModel.getValueAt(i, 0);
          String dueDate = (String) tableModel.getValueAt(i, 1);
          String priority = (String) tableModel.getValueAt(i, 2);
          String status = (String) tableModel.getValueAt(i, 3);
          boolean isPinned = pinnedTaskRows.contains(i);

          // Write CSV row with comma escaping
          writer.println(
              escapeCSV(task) + "," +
                  escapeCSV(dueDate) + "," +
                  escapeCSV(priority) + "," +
                  escapeCSV(status) + "," +
                  (isPinned ? "true" : "false"));
        }

        JOptionPane.showMessageDialog(mainWindow,
            "Tasks successfully exported to " + file.getName(),
            "Export Complete", JOptionPane.INFORMATION_MESSAGE);

      } catch (IOException e) {
        JOptionPane.showMessageDialog(mainWindow,
            "Error exporting tasks: " + e.getMessage(),
            "Export Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    }
  }

  private String escapeCSV(String value) {
    if (value == null)
      return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  /**
   * Import tasks từ file CSV
   */
  public void importTasks() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Import Tasks");
    fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

    int choice = fileChooser.showOpenDialog(mainWindow);

    if (choice == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();

      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String line;
        boolean isFirstLine = true;
        int addedCount = 0;

        // Clear existing tasks
        int confirm = JOptionPane.showConfirmDialog(mainWindow,
            "Do you want to replace existing tasks or append imported tasks?",
            "Import Options", JOptionPane.YES_NO_CANCEL_OPTION);

        if (confirm == JOptionPane.CANCEL_OPTION) {
          return;
        } else if (confirm == JOptionPane.YES_OPTION) {
          // Clear all existing tasks
          while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
          }
          pinnedTaskRows.clear();
        }

        // Read CSV file
        while ((line = reader.readLine()) != null) {
          if (isFirstLine) {
            isFirstLine = false;
            if (line.startsWith("Task,")) {
              continue; // Skip header row
            }
          }

          // Parse CSV line
          String[] parts = parseCSVLine(line);
          if (parts.length >= 4) {
            tableModel.addRow(new Object[] {
                parts[0], parts[1], parts[2], parts[3]
            });

            // Check for pin status
            if (parts.length >= 5 && parts[4].trim().equalsIgnoreCase("true")) {
              pinnedTaskRows.add(tableModel.getRowCount() - 1);
            }

            addedCount++;
          }
        }

        // Update pinned tasks
        taskTable.updatePinnedTasks(pinnedTaskRows);

        // Re-sort if needed
        if (rowSorter != null) {
          rowSorter.sort();
        }

        // Update task count
        mainWindow.updateTaskCount();

        JOptionPane.showMessageDialog(mainWindow,
            addedCount + " tasks imported successfully!",
            "Import Complete", JOptionPane.INFORMATION_MESSAGE);

      } catch (IOException e) {
        JOptionPane.showMessageDialog(mainWindow,
            "Error importing tasks: " + e.getMessage(),
            "Import Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    }
  }

  private String[] parseCSVLine(String line) {
    ArrayList<String> result = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        // Quote handling
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          // Escaped quote
          field.append('"');
          i++; // Skip the next quote
        } else {
          // Start/end quote
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        // End of field
        result.add(field.toString());
        field.setLength(0);
      } else {
        field.append(c);
      }
    }

    // Add the last field
    result.add(field.toString());

    return result.toArray(new String[0]);
  }

  /**
   * Hiển thị dialog tiến độ
   */
  public void showProgressDialog() {
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

    // Hiển thị thông báo động lực dựa trên tiến độ
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

    JOptionPane.showMessageDialog(mainWindow,
        String.format("Progress: %d/%d tasks completed (%.1f%%)\n\n%s",
            completed, total, percentage, message),
        "Task Progress", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Hiển thị calendar dialog
   */
  public void showCalendarDialog() {
    DatePickerDialog dialog = new DatePickerDialog(mainWindow, tableModel);
    dialog.setVisible(true);
  }

  /**
   * Lấy LinkedHashSet các task được ghim
   */
  public LinkedHashSet<Integer> getPinnedTaskRows() {
    return pinnedTaskRows;
  }

  /**
   * Kiểm tra task nào đang được ghim
   */
  public boolean isTaskPinned(int modelRow) {
    return pinnedTaskRows.contains(modelRow);
  }

  /**
   * Thực hiện validate dữ liệu nhập vào
   */
  public boolean validateTaskData(String taskName, String dueDate) {
    if (taskName == null || taskName.trim().isEmpty()) {
      JOptionPane.showMessageDialog(mainWindow,
          "Task name cannot be empty.",
          "Validation Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    if (dueDate != null && !dueDate.trim().isEmpty()) {
      // Check date format
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        dateFormat.parse(dueDate);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(mainWindow,
            "Due date must be in YYYY-MM-DD format.",
            "Validation Error", JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }

    return true;
  }
}