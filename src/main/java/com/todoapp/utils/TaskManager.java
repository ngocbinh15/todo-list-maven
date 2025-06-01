package com.todoapp.utils;

import java.util.ArrayList;
import java.util.List;

import com.todoapp.models.Task;

/**
 * Manager quản lý tasks trong bộ nhớ và đồng bộ với storage
 * Cung cấp CRUD operations và tracking changes
 */
public class TaskManager {
  private final List<Task> tasks;
  private final TaskStorage taskStorage;
  private boolean hasUnsavedChanges;

  public TaskManager() {
    this.tasks = new ArrayList<>();
    this.taskStorage = new TaskStorage();
    this.hasUnsavedChanges = false;

    // Auto-load existing data if available
    if (taskStorage.dataFileExists()) {
      loadTasksFromFile();
    }
  }

  // ==================== TASK OPERATIONS ====================

  /**
   * Thêm task mới
   */
  public void addTask(Task task) {
    if (task != null) {
      tasks.add(task);
      markAsChanged();
    }
  }

  /**
   * Cập nhật task tại index
   */
  public void updateTask(int index, Task task) {
    if (isValidIndex(index) && task != null) {
      tasks.set(index, task);
      markAsChanged();
    }
  }

  /**
   * Xóa task tại index
   */
  public void deleteTask(int index) {
    if (isValidIndex(index)) {
      tasks.remove(index);
      markAsChanged();
    }
  }

  /**
   * Xóa tất cả tasks
   */
  public void clearAllTasks() {
    tasks.clear();
    markAsChanged();
  }

  /**
   * Lấy task tại index
   */
  public Task getTask(int index) {
    return isValidIndex(index) ? tasks.get(index) : null;
  }

  /**
   * Lấy tất cả tasks (defensive copy)
   */
  public List<Task> getAllTasks() {
    return new ArrayList<>(tasks);
  }

  /**
   * Lấy số lượng tasks
   */
  public int getTaskCount() {
    return tasks.size();
  }

  /**
   * Kiểm tra có tasks hay không
   */
  public boolean isEmpty() {
    return tasks.isEmpty();
  }

  // ==================== FILE OPERATIONS ====================

  /**
   * Lưu tasks vào file
   */
  public boolean saveTasksToFile() {
    boolean saved = taskStorage.saveTasksToFile(tasks);
    if (saved) {
      hasUnsavedChanges = false;
    }
    return saved;
  }

  /**
   * Tải tasks từ file
   */
  public boolean loadTasksFromFile() {
    List<Task> loadedTasks = taskStorage.loadTasksFromFile();

    if (loadedTasks != null) {
      tasks.clear();
      tasks.addAll(loadedTasks);
      hasUnsavedChanges = false;
      return !loadedTasks.isEmpty();
    }

    return false;
  }

  /**
   * Lưu danh sách tasks từ UI và đồng bộ với memory
   */
  public boolean saveTasksFromUIList(List<Task> uiTasks) {
    if (uiTasks == null) {
      return false;
    }

    boolean saved = taskStorage.saveTasksToFile(uiTasks);

    if (saved) {
      // Sync memory with UI data
      tasks.clear();
      tasks.addAll(uiTasks);
      hasUnsavedChanges = false;
    }

    return saved;
  }

  // ==================== CHANGE TRACKING ====================

  /**
   * Kiểm tra có thay đổi chưa lưu
   */
  public boolean hasUnsavedChanges() {
    return hasUnsavedChanges;
  }

  /**
   * Đánh dấu có thay đổi
   */
  public void markAsChanged() {
    hasUnsavedChanges = true;
  }

  /**
   * Đánh dấu đã lưu
   */
  public void markAsSaved() {
    hasUnsavedChanges = false;
  }

  // ==================== SEARCH & FILTER ====================

  /**
   * Tìm tasks theo tên
   */
  public List<Task> findTasksByName(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return getAllTasks();
    }

    List<Task> results = new ArrayList<>();
    String searchTerm = keyword.toLowerCase().trim();

    for (Task task : tasks) {
      if (task.getName().toLowerCase().contains(searchTerm)) {
        results.add(task);
      }
    }

    return results;
  }

  /**
   * Lấy tasks theo status
   */
  public List<Task> getTasksByStatus(String status) {
    List<Task> results = new ArrayList<>();

    for (Task task : tasks) {
      if (status.equals(task.getStatus())) {
        results.add(task);
      }
    }

    return results;
  }

  /**
   * Lấy tasks theo priority
   */
  public List<Task> getTasksByPriority(String priority) {
    List<Task> results = new ArrayList<>();

    for (Task task : tasks) {
      if (priority.equals(task.getPriority())) {
        results.add(task);
      }
    }

    return results;
  }

  /**
   * Lấy tasks đã pin
   */
  public List<Task> getPinnedTasks() {
    List<Task> results = new ArrayList<>();

    for (Task task : tasks) {
      if (task.isPinned()) {
        results.add(task);
      }
    }

    return results;
  }

  // ==================== STATISTICS ====================

  /**
   * Lấy số tasks theo status
   */
  public int getTaskCountByStatus(String status) {
    return (int) tasks.stream()
        .filter(task -> status.equals(task.getStatus()))
        .count();
  }

  /**
   * Tính phần trăm hoàn thành
   */
  public double getCompletionPercentage() {
    if (tasks.isEmpty()) {
      return 0.0;
    }

    long completedCount = tasks.stream()
        .filter(task -> "Completed".equals(task.getStatus()))
        .count();

    return (completedCount * 100.0) / tasks.size();
  }

  // ==================== UTILITY METHODS ====================

  /**
   * Kiểm tra index hợp lệ
   */
  private boolean isValidIndex(int index) {
    return index >= 0 && index < tasks.size();
  }

  /**
   * Lấy thông tin debug
   */
  public String getDebugInfo() {
    return String.format("TaskManager: %d tasks, unsaved: %s",
        tasks.size(), hasUnsavedChanges);
  }

  @Override
  public String toString() {
    return getDebugInfo();
  }
}