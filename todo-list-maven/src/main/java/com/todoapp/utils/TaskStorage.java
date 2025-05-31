package com.todoapp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.todoapp.models.Task;

/**
 * Quản lý việc lưu trữ và tải dữ liệu tasks từ file
 * Sử dụng format: TaskName|DueDate|Priority|Status|IsPinned
 */
public class TaskStorage {
  private static final String DATA_FILENAME = "data.txt";
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private final File dataFile;

  public TaskStorage() {
    this.dataFile = new File(System.getProperty("user.dir"), DATA_FILENAME);
    setupDataFile();
  }

  // ==================== FILE SETUP ====================

  /**
   * Thiết lập file data.txt
   */
  private void setupDataFile() {
    try {
      if (!dataFile.exists()) {
        boolean created = dataFile.createNewFile();
        if (created) {
          createEmptyDataFile();
        }
      }
    } catch (IOException e) {
      System.err.println("Error setting up data file: " + e.getMessage());
    }
  }

  /**
   * Tạo file rỗng với header
   */
  private void createEmptyDataFile() {
    try (PrintWriter writer = new PrintWriter(dataFile)) {
      writer.println("# Todo List App Data");
      writer.println("# Format: TaskName|DueDate|Priority|Status|IsPinned");
      writer.println("# Created: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
      writer.println();
    } catch (IOException e) {
      System.err.println("Error creating empty data file: " + e.getMessage());
    }
  }

  // ==================== SAVE OPERATIONS ====================

  /**
   * Lưu danh sách tasks vào file
   */
  public boolean saveTasksToFile(List<Task> tasks) {
    if (tasks == null) {
      return false;
    }

    // Backup file hiện tại
    createBackup();

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile, false))) {
      writeHeader(writer);
      int savedCount = writeTasks(writer, tasks);

      writer.flush();
      return savedCount > 0;

    } catch (IOException e) {
      System.err.println("Error saving tasks: " + e.getMessage());
      return false;
    }
  }

  /**
   * Ghi header vào file
   */
  private void writeHeader(BufferedWriter writer) throws IOException {
    writer.write("# Todo List App Data");
    writer.newLine();
    writer.write("# Format: TaskName|DueDate|Priority|Status|IsPinned");
    writer.newLine();
    writer.write("# Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    writer.newLine();
    writer.newLine();
  }

  /**
   * Ghi tasks vào file
   */
  private int writeTasks(BufferedWriter writer, List<Task> tasks) throws IOException {
    int savedCount = 0;

    for (Task task : tasks) {
      if (task == null || isInvalidTask(task)) {
        continue;
      }

      String line = formatTaskLine(task);
      writer.write(line);
      writer.newLine();
      savedCount++;
    }

    return savedCount;
  }

  /**
   * Format task thành string line
   */
  private String formatTaskLine(Task task) {
    StringBuilder line = new StringBuilder();

    // Task name (escape special chars)
    String taskName = task.getName().replace("|", "\\|");
    line.append(taskName).append("|");

    // Due date
    if (task.getDueDate() != null) {
      line.append(DATE_FORMAT.format(task.getDueDate()));
    }
    line.append("|");

    // Priority, Status, Pinned
    String priority = task.getPriority() != null ? task.getPriority() : "Medium";
    String status = task.getStatus() != null ? task.getStatus() : "Pending";

    line.append(priority).append("|")
        .append(status).append("|")
        .append(task.isPinned());

    return line.toString();
  }

  /**
   * Kiểm tra task không hợp lệ
   */
  private boolean isInvalidTask(Task task) {
    return task.getName() == null || task.getName().trim().isEmpty();
  }

  // ==================== LOAD OPERATIONS ====================

  /**
   * Tải danh sách tasks từ file
   */
  public List<Task> loadTasksFromFile() {
    List<Task> tasks = new ArrayList<>();

    if (!dataFile.exists()) {
      return tasks;
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
      String line;
      int lineNumber = 0;

      while ((line = reader.readLine()) != null) {
        lineNumber++;

        // Skip comments và empty lines
        if (isCommentOrEmpty(line)) {
          continue;
        }

        Task task = parseTaskLine(line, lineNumber);
        if (task != null) {
          tasks.add(task);
        }
      }

    } catch (IOException e) {
      System.err.println("Error loading tasks: " + e.getMessage());
    }

    return tasks;
  }

  /**
   * Parse dòng text thành Task object
   */
  private Task parseTaskLine(String line, int lineNumber) {
    String[] parts = line.split("\\|", -1); // -1 để giữ empty strings

    if (parts.length < 4) {
      System.err.println("Invalid line format at line " + lineNumber + ": " + line);
      return null;
    }

    try {
      String name = unescapeString(parts[0]);
      String priority = parts[2].isEmpty() ? "Medium" : parts[2];
      String status = parts[3].isEmpty() ? "Pending" : parts[3];
      boolean isPinned = parts.length >= 5 ? Boolean.parseBoolean(parts[4]) : false;

      Task task = new Task(name);
      task.setPriority(priority);
      task.setStatus(status);
      task.setPinned(isPinned);

      // Parse due date
      if (!parts[1].isEmpty()) {
        try {
          task.setDueDate(DATE_FORMAT.parse(parts[1]));
        } catch (ParseException e) {
          System.err.println("Invalid date format at line " + lineNumber + ": " + parts[1]);
        }
      }

      return task;

    } catch (Exception e) {
      System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
      return null;
    }
  }

  // ==================== UTILITY METHODS ====================

  /**
   * Kiểm tra dòng comment hoặc rỗng
   */
  private boolean isCommentOrEmpty(String line) {
    String trimmed = line.trim();
    return trimmed.isEmpty() || trimmed.startsWith("#");
  }

  /**
   * Unescape string (đảo ngược escape)
   */
  private String unescapeString(String text) {
    return text.replace("\\|", "|");
  }

  /**
   * Tạo backup file
   */
  private void createBackup() {
    if (dataFile.exists() && dataFile.length() > 0) {
      try {
        File backupFile = new File(dataFile.getAbsolutePath() + ".backup");
        Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        System.err.println("Warning: Could not create backup: " + e.getMessage());
      }
    }
  }

  /**
   * Kiểm tra file data có tồn tại không
   */
  public boolean dataFileExists() {
    return dataFile.exists() && dataFile.length() > 0;
  }

  /**
   * Lấy đường dẫn file data
   */
  public String getDataFilePath() {
    return dataFile.getAbsolutePath();
  }

  /**
   * Lấy kích thước file
   */
  public long getFileSize() {
    return dataFile.exists() ? dataFile.length() : 0;
  }

  /**
   * Xóa file data (để reset)
   */
  public boolean deleteDataFile() {
    try {
      if (dataFile.exists()) {
        return dataFile.delete();
      }
      return true;
    } catch (Exception e) {
      System.err.println("Error deleting data file: " + e.getMessage());
      return false;
    }
  }

  /**
   * Validate file integrity
   */
  public boolean validateDataFile() {
    if (!dataFile.exists()) {
      return false;
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
      String line;
      int validLines = 0;
      int totalDataLines = 0;

      while ((line = reader.readLine()) != null) {
        if (isCommentOrEmpty(line)) {
          continue;
        }

        totalDataLines++;
        String[] parts = line.split("\\|");

        if (parts.length >= 4 && !parts[0].trim().isEmpty()) {
          validLines++;
        }
      }

      return totalDataLines == 0 || validLines == totalDataLines;

    } catch (IOException e) {
      return false;
    }
  }
}
