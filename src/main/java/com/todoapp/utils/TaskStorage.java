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
  private static final String DATA_FILENAME = "data/data.txt"; // Thay đổi để bao gồm thư mục data
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private final File dataFile;

  public TaskStorage() {
    // Tạo đường dẫn tới src/main/resources/data/data.txt
    String projectDir = System.getProperty("user.dir");
    String resourcesPath = "src/main/resources/" + DATA_FILENAME;
    this.dataFile = new File(projectDir, resourcesPath);
    setupDataFile();
  }

  // ==================== FILE SETUP ====================

  /**
   * Thiết lập file data.txt trong thư mục resources/data
   */
  private void setupDataFile() {
    try {
      System.out.println("TaskStorage: Thiết lập file tại: " + dataFile.getAbsolutePath());

      // Tạo thư mục data nếu chưa tồn tại
      File dataDir = dataFile.getParentFile();
      if (!dataDir.exists()) {
        boolean created = dataDir.mkdirs();
        System.out.println("TaskStorage: Tạo thư mục data: " + (created ? "thành công" : "thất bại"));
      }

      // Tạo file nếu chưa tồn tại
      if (!dataFile.exists()) {
        boolean created = dataFile.createNewFile();
        if (created) {
          System.out.println("TaskStorage: Tạo file data.txt mới");
          createEmptyDataFile();
        }
      } else {
        System.out.println("TaskStorage: File data.txt đã tồn tại, kích thước: " + dataFile.length() + " bytes");
      }
    } catch (IOException e) {
      System.err.println("TaskStorage: Lỗi khi thiết lập file: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Tạo file rỗng với header
   */
  private void createEmptyDataFile() {
    try (PrintWriter writer = new PrintWriter(dataFile, "UTF-8")) {
      writer.println("# Todo List App Data");
      writer.println("# Format: TaskName|DueDate|Priority|Status|IsPinned");
      writer.println("# Created: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
      writer.println();
      System.out.println("TaskStorage: Đã tạo file với header");
    } catch (IOException e) {
      System.err.println("TaskStorage: Lỗi khi tạo file rỗng: " + e.getMessage());
    }
  }

  // ==================== SAVE OPERATIONS ====================

  /**
   * Lưu danh sách tasks vào file
   */
  public boolean saveTasksToFile(List<Task> tasks) {
    if (tasks == null) {
      System.err.println("TaskStorage: Danh sách task null");
      return false;
    }

    System.out.println("TaskStorage: Đang lưu " + tasks.size() + " task vào: " + dataFile.getAbsolutePath());

    // Backup file hiện tại
    createBackup();

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile, false))) {
      writeHeader(writer);
      int savedCount = writeTasks(writer, tasks);

      writer.flush();
      System.out.println("TaskStorage: Đã lưu " + savedCount + " task thành công");
      return savedCount >= 0; // Cho phép lưu 0 task (xóa hết)

    } catch (IOException e) {
      System.err.println("TaskStorage: Lỗi khi lưu tasks: " + e.getMessage());
      e.printStackTrace();
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

    System.out.println("TaskStorage: Đang tải dữ liệu từ: " + dataFile.getAbsolutePath());

    if (!dataFile.exists()) {
      System.out.println("TaskStorage: File không tồn tại");
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

      System.out.println("TaskStorage: Đã tải " + tasks.size() + " task thành công");

    } catch (IOException e) {
      System.err.println("TaskStorage: Lỗi khi tải tasks: " + e.getMessage());
      e.printStackTrace();
    }

    return tasks;
  }

  /**
   * Parse dòng text thành Task object
   */
  private Task parseTaskLine(String line, int lineNumber) {
    String[] parts = line.split("\\|", -1); // -1 để giữ empty strings

    if (parts.length < 4) {
      System.err.println("TaskStorage: Định dạng dòng không hợp lệ tại dòng " + lineNumber + ": " + line);
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
          System.err.println("TaskStorage: Định dạng ngày không hợp lệ tại dòng " + lineNumber + ": " + parts[1]);
        }
      }

      return task;

    } catch (Exception e) {
      System.err.println("TaskStorage: Lỗi khi parse dòng " + lineNumber + ": " + e.getMessage());
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
        System.out.println("TaskStorage: Đã tạo backup tại: " + backupFile.getAbsolutePath());
      } catch (IOException e) {
        System.err.println("TaskStorage: Không thể tạo backup: " + e.getMessage());
      }
    }
  }

  /**
   * Kiểm tra file data có tồn tại không
   */
  public boolean dataFileExists() {
    boolean exists = dataFile.exists() && dataFile.length() > 0;
    System.out.println("TaskStorage: File tồn tại và có dữ liệu: " + exists);
    return exists;
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
        boolean deleted = dataFile.delete();
        System.out.println("TaskStorage: Xóa file: " + (deleted ? "thành công" : "thất bại"));
        return deleted;
      }
      return true;
    } catch (Exception e) {
      System.err.println("TaskStorage: Lỗi khi xóa file: " + e.getMessage());
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

      boolean isValid = totalDataLines == 0 || validLines == totalDataLines;
      System.out.println(
          "TaskStorage: Validation - Total: " + totalDataLines + ", Valid: " + validLines + ", Result: " + isValid);
      return isValid;

    } catch (IOException e) {
      System.err.println("TaskStorage: Lỗi khi validate file: " + e.getMessage());
      return false;
    }
  }
}
