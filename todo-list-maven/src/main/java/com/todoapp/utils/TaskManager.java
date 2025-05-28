package com.todoapp.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import com.todoapp.models.Task;

/**
 * Utility class to manage tasks and provide serialization/deserialization
 * for saving and loading tasks from file
 */
public class TaskManager {
  private List<Task> tasks;
  private LinkedHashSet<Integer> pinnedTaskRows;

  public TaskManager() {
    tasks = new ArrayList<>();
    pinnedTaskRows = new LinkedHashSet<>();
  }

  /**
   * Load tasks from the table model
   */
  public void loadFromTableModel(DefaultTableModel model, LinkedHashSet<Integer> pinnedRows) {
    tasks.clear();
    pinnedTaskRows = new LinkedHashSet<>(pinnedRows);

    for (int i = 0; i < model.getRowCount(); i++) {
      String name = (String) model.getValueAt(i, 0);
      String dueDate = (String) model.getValueAt(i, 1);
      String priority = (String) model.getValueAt(i, 2);
      String status = (String) model.getValueAt(i, 3);
      boolean isPinned = pinnedTaskRows.contains(i);

      Task task = new Task(name, dueDate, priority, status, isPinned);
      tasks.add(task);
    }
  }

  /**
   * Save tasks to the table model
   */
  public void saveToTableModel(DefaultTableModel model) {
    // Clear the model
    while (model.getRowCount() > 0) {
      model.removeRow(0);
    }

    // Add tasks to model
    pinnedTaskRows.clear();
    int index = 0;

    for (Task task : tasks) {
      model.addRow(new Object[] {
          task.getName(),
          task.getDueDate(),
          task.getPriority(),
          task.getStatus()
      });

      if (task.isPinned()) {
        pinnedTaskRows.add(index);
      }

      index++;
    }
  }

  /**
   * Save tasks to binary file
   */
  public boolean saveToBinaryFile(File file) {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
      oos.writeObject(tasks);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Load tasks from binary file
   */
  @SuppressWarnings("unchecked")
  public boolean loadFromBinaryFile(File file) {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      tasks = (List<Task>) ois.readObject();

      // Rebuild pinned set
      pinnedTaskRows.clear();
      for (int i = 0; i < tasks.size(); i++) {
        if (tasks.get(i).isPinned()) {
          pinnedTaskRows.add(i);
        }
      }

      return true;
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Get all tasks
   */
  public List<Task> getTasks() {
    return tasks;
  }

  /**
   * Get pinned task indices
   */
  public LinkedHashSet<Integer> getPinnedTaskRows() {
    return pinnedTaskRows;
  }

  /**
   * Get tasks by date
   */
  public List<Task> getTasksByDate(String dateStr) {
    List<Task> result = new ArrayList<>();

    for (Task task : tasks) {
      if (dateStr.equals(task.getDueDate())) {
        result.add(task);
      }
    }

    return result;
  }

  /**
   * Get overdue tasks
   */
  public List<Task> getOverdueTasks() {
    List<Task> result = new ArrayList<>();

    for (Task task : tasks) {
      if (task.isOverdue()) {
        result.add(task);
      }
    }

    return result;
  }
}
