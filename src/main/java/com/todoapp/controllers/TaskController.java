package com.todoapp.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.todoapp.components.TaskTable;
import com.todoapp.models.Task;
import com.todoapp.views.DatePickerDialog;
import com.todoapp.views.MainWindow;
import com.todoapp.views.TaskDialog;

/**
 * Controller để xử lý các thao tác với tasks
 * Quản lý CRUD operations, import/export, sorting và filtering
 */
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

    if (taskTable.getRowSorter() != null &&
        taskTable.getRowSorter() instanceof TableRowSorter) {
      rowSorter = (TableRowSorter<DefaultTableModel>) taskTable.getRowSorter();
    }

    setupListeners();
  }

  /**
   * Thiết lập event listeners cho TaskTable
   */
  private void setupListeners() {
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

  // ==================== CRUD OPERATIONS ====================

  /**
   * Thêm task mới thông qua dialog
   */
  public void addTask() {
    TaskDialog dialog = new TaskDialog(mainWindow, "Add New Task", "", "", "Medium", "Pending");
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      String name = dialog.getTaskName();
      String dueDate = dialog.getDueDate();
      String priority = dialog.getPriority();
      String status = dialog.getStatus();

      tableModel.addRow(new Object[] { name, dueDate, priority, status });
      mainWindow.updateTaskCount();

      JOptionPane.showMessageDialog(mainWindow,
          "Task added successfully!",
          "Success",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Sửa task đã chọn
   */
  public void editTask(int viewRow) {
    if (viewRow < 0) {
      JOptionPane.showMessageDialog(mainWindow,
          "Please select a task to edit.",
          "No Task Selected", JOptionPane.WARNING_MESSAGE);
      return;
    }

    int modelRow = taskTable.convertRowIndexToModel(viewRow);

    String currentTask = (String) tableModel.getValueAt(modelRow, 0);
    String currentDate = (String) tableModel.getValueAt(modelRow, 1);
    String currentPriority = (String) tableModel.getValueAt(modelRow, 2);
    String currentStatus = (String) tableModel.getValueAt(modelRow, 3);

    TaskDialog dialog = new TaskDialog(mainWindow, "Edit Task",
        currentTask, currentDate, currentPriority, currentStatus);
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      tableModel.setValueAt(dialog.getTaskName(), modelRow, 0);
      tableModel.setValueAt(dialog.getDueDate(), modelRow, 1);
      tableModel.setValueAt(dialog.getPriority(), modelRow, 2);
      tableModel.setValueAt(dialog.getStatus(), modelRow, 3);
      mainWindow.updateTaskCount();
    }
  }

  /**
   * Xóa task đã chọn
   */
  public void deleteTask(int viewRow) {
    if (viewRow < 0) {
      JOptionPane.showMessageDialog(mainWindow,
          "Please select a task to delete.",
          "No Task Selected", JOptionPane.WARNING_MESSAGE);
      return;
    }

    int modelRow = taskTable.convertRowIndexToModel(viewRow);
    String taskName = (String) tableModel.getValueAt(modelRow, 0);

    int confirm = JOptionPane.showConfirmDialog(mainWindow,
        "Are you sure you want to delete \"" + taskName + "\"?",
        "Confirm Delete", JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE);

    if (confirm == JOptionPane.YES_OPTION) {
      pinnedTaskRows.remove(modelRow);

      // Cập nhật lại index cho các pinned tasks
      for (Integer pinnedRow : new ArrayList<>(pinnedTaskRows)) {
        if (pinnedRow > modelRow) {
          pinnedTaskRows.remove(pinnedRow);
          pinnedTaskRows.add(pinnedRow - 1);
        }
      }

      tableModel.removeRow(modelRow);
      mainWindow.updateTaskCount();
    }
  }

  // ==================== PIN & SORT OPERATIONS ====================

  /**
   * Chuyển đổi trạng thái pin của task
   */
  public void togglePinTask(int modelRow) {
    if (pinnedTaskRows.contains(modelRow)) {
      pinnedTaskRows.remove(modelRow);
    } else {
      pinnedTaskRows.add(modelRow);
    }

    taskTable.updatePinnedTasks(pinnedTaskRows);

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
   * Hiển thị dialog chọn cột để sắp xếp
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
   * Lọc tasks theo từ khóa
   */
  public void filterTasks(String keyword) {
    taskTable.filterTasks(keyword);
  }

  // ==================== IMPORT/EXPORT OPERATIONS ====================

  /**
   * Export tasks ra file CSV
   */
  public void exportTasks() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Tasks");
    fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

    int choice = fileChooser.showSaveDialog(mainWindow);

    if (choice == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      String path = file.getAbsolutePath();

      if (!path.toLowerCase().endsWith(".csv")) {
        file = new File(path + ".csv");
      }

      try (PrintWriter writer = new PrintWriter(file)) {
        writer.println("Task,DueDate,Priority,Status,Pinned");

        for (int i = 0; i < tableModel.getRowCount(); i++) {
          String task = (String) tableModel.getValueAt(i, 0);
          String dueDate = (String) tableModel.getValueAt(i, 1);
          String priority = (String) tableModel.getValueAt(i, 2);
          String status = (String) tableModel.getValueAt(i, 3);
          boolean isPinned = pinnedTaskRows.contains(i);

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

        int confirm = JOptionPane.showConfirmDialog(mainWindow,
            "Do you want to replace existing tasks or append imported tasks?",
            "Import Options", JOptionPane.YES_NO_CANCEL_OPTION);

        if (confirm == JOptionPane.CANCEL_OPTION) {
          return;
        } else if (confirm == JOptionPane.YES_OPTION) {
          while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
          }
          pinnedTaskRows.clear();
        }

        while ((line = reader.readLine()) != null) {
          if (isFirstLine) {
            isFirstLine = false;
            if (line.startsWith("Task,")) {
              continue;
            }
          }

          String[] parts = parseCSVLine(line);
          if (parts.length >= 4) {
            tableModel.addRow(new Object[] {
                parts[0], parts[1], parts[2], parts[3]
            });

            if (parts.length >= 5 && parts[4].trim().equalsIgnoreCase("true")) {
              pinnedTaskRows.add(tableModel.getRowCount() - 1);
            }

            addedCount++;
          }
        }

        taskTable.updatePinnedTasks(pinnedTaskRows);

        if (rowSorter != null) {
          rowSorter.sort();
        }

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

  // ==================== SAVE OPERATIONS ====================

  /**
   * Lưu tất cả tasks từ UI vào file data.txt
   * Sử dụng cách tiếp cận tương tự export để đảm bảo tính nhất quán
   */
  public boolean saveTasksFromUI() {
    try {
      // Lấy đường dẫn file data.txt trong resources/data/
      String projectDir = System.getProperty("user.dir");
      String resourcesPath = "src/main/resources/data/data.txt";
      File dataFile = new File(projectDir, resourcesPath);

      System.out.println("TaskController: Lưu dữ liệu vào: " + dataFile.getAbsolutePath());

      // Tạo backup trước khi lưu
      createBackupBeforeSave(dataFile);

      // Đảm bảo thư mục tồn tại
      File parentDir = dataFile.getParentFile();
      if (!parentDir.exists()) {
        parentDir.mkdirs();
      }

      // Lấy dữ liệu trực tiếp từ bảng UI (giống như export)
      DefaultTableModel tableModel = (DefaultTableModel) taskTable.getModel();
      int rowCount = tableModel.getRowCount();

      System.out.println("TaskController: Số dòng trong bảng UI: " + rowCount);

      try (PrintWriter writer = new PrintWriter(dataFile, "UTF-8")) {
        // Ghi header giống như TaskStorage
        writer.println("# Todo List App Data");
        writer.println("# Format: TaskName|DueDate|Priority|Status|IsPinned");
        writer.println("# Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println();

        int savedCount = 0;

        // Lưu từng dòng từ bảng UI
        for (int i = 0; i < rowCount; i++) {
          String taskName = getTableValue(tableModel, i, 0);
          String dueDate = getTableValue(tableModel, i, 1);
          String priority = getTableValue(tableModel, i, 2);
          String status = getTableValue(tableModel, i, 3);

          // Bỏ qua dòng trống
          if (taskName == null || taskName.trim().isEmpty()) {
            continue;
          }

          // Format dòng dữ liệu
          String line = formatTaskLineFromUI(taskName, dueDate, priority, status);
          writer.println(line);
          savedCount++;

          System.out.println("TaskController: Lưu task " + savedCount + ": " + taskName);
        }

        System.out.println("TaskController: Đã lưu " + savedCount + " task thành công từ UI");
        return savedCount >= 0; // Cho phép lưu file trống

      } catch (IOException e) {
        System.err.println("TaskController: Lỗi khi ghi file: " + e.getMessage());
        e.printStackTrace();
        return false;
      }

    } catch (Exception e) {
      System.err.println("TaskController: Lỗi khi lưu từ UI: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Tạo bản sao lưu trước khi lưu file data.txt
   */
  private void createBackupBeforeSave(File dataFile) {
    if (dataFile.exists() && dataFile.length() > 0) {
      try {
        File backupFile = new File(dataFile.getAbsolutePath() + ".backup");
        Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("TaskController: Đã tạo backup tại: " + backupFile.getAbsolutePath());
      } catch (IOException e) {
        System.err.println("TaskController: Không thể tạo backup: " + e.getMessage());
      }
    }
  }

  /**
   * Format task line từ dữ liệu UI
   */
  private String formatTaskLineFromUI(String taskName, String dueDate, String priority, String status) {
    StringBuilder line = new StringBuilder();

    // Task name (escape ký tự |)
    line.append(taskName.replace("|", "\\|")).append("|");

    // Due date
    line.append(dueDate != null ? dueDate.trim() : "").append("|");

    // Priority (mặc định Medium)
    String priorityValue = (priority != null && !priority.trim().isEmpty()) ? priority.trim() : "Medium";
    line.append(priorityValue).append("|");

    // Status (mặc định Pending)
    String statusValue = (status != null && !status.trim().isEmpty()) ? status.trim() : "Pending";
    line.append(statusValue).append("|");

    // IsPinned (mặc định false)
    line.append("false");

    return line.toString();
  }

  /**
   * Lấy giá trị từ bảng với xử lý null safety
   */
  private String getTableValue(DefaultTableModel model, int row, int col) {
    try {
      Object value = model.getValueAt(row, col);
      return value != null ? value.toString().trim() : "";
    } catch (Exception e) {
      System.err.println("TaskController: Lỗi khi lấy giá trị tại [" + row + "," + col + "]: " + e.getMessage());
      return "";
    }
  }

  // ==================== UTILITY METHODS ====================

  /**
   * Escape ký tự đặc biệt cho CSV
   */
  private String escapeCSV(String value) {
    if (value == null)
      return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  /**
   * Parse dòng CSV với xử lý quotes
   */
  private String[] parseCSVLine(String line) {
    ArrayList<String> result = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          field.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        result.add(field.toString());
        field.setLength(0);
      } else {
        field.append(c);
      }
    }

    result.add(field.toString());
    return result.toArray(new String[0]);
  }

  /**
   * Đồng bộ dữ liệu từ UI về TaskManager
   */
  private void syncUIToTaskManager() {
    try {
      DefaultTableModel model = (DefaultTableModel) taskTable.getModel();
      List<Task> uiTasks = new ArrayList<>();

      for (int i = 0; i < model.getRowCount(); i++) {
        String name = (String) model.getValueAt(i, 0);
        String dueDate = (String) model.getValueAt(i, 1);
        String priority = (String) model.getValueAt(i, 2);
        String status = (String) model.getValueAt(i, 3);

        if (name != null && !name.trim().isEmpty()) {
          Task task = new Task(name.trim());
          task.setPriority(priority != null ? priority : "Medium");
          task.setStatus(status != null ? status : "Pending");

          if (dueDate != null && !dueDate.trim().isEmpty()) {
            try {
              SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
              task.setDueDate(dateFormat.parse(dueDate));
            } catch (Exception e) {
              // Ignore date parsing errors
            }
          }

          if (pinnedTaskRows != null && pinnedTaskRows.contains(i)) {
            task.setPinned(true);
          }

          uiTasks.add(task);
        }
      }

      mainWindow.getTaskManager().clearAllTasks();
      for (Task task : uiTasks) {
        mainWindow.getTaskManager().addTask(task);
      }

      System.out.println("Synced " + uiTasks.size() + " tasks from UI to TaskManager");

    } catch (Exception e) {
      System.err.println("Error syncing UI to TaskManager: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Validate dữ liệu task input
   */
  public boolean validateTaskData(String taskName, String dueDate) {
    if (taskName == null || taskName.trim().isEmpty()) {
      JOptionPane.showMessageDialog(mainWindow,
          "Task name cannot be empty.",
          "Validation Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    if (dueDate != null && !dueDate.trim().isEmpty()) {
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

  // ==================== DIALOG METHODS ====================

  /**
   * Hiển thị dialog tiến độ với thông báo động lực
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

  // ==================== GETTERS ====================

  public LinkedHashSet<Integer> getPinnedTaskRows() {
    return pinnedTaskRows;
  }

  public boolean isTaskPinned(int modelRow) {
    return pinnedTaskRows.contains(modelRow);
  }
}