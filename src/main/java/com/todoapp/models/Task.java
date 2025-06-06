package com.todoapp.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Model class representing a Task in the ToDo List application.
 * Implements Serializable to support saving tasks to file.
 */
public class Task implements Serializable, Comparable<Task> {
  private static final long serialVersionUID = 1L;

  private String name;
  private Date dueDate;
  private String priority;
  private String status;
  private boolean pinned;

  /**
   * Constructor to create a new task with all properties
   */
  public Task(String name, Date dueDate, String priority, String status, boolean pinned) {
    this.name = name;
    this.dueDate = dueDate;
    this.priority = priority;
    this.status = status;
    this.pinned = pinned;
  }

  /**
   * Simplified constructor with defaults
   */
  public Task(String name, Date dueDate) {
    this(name, dueDate, "Medium", "Pending", false);
  }

  // Thêm constructor này
  public Task(String name) {
    this(name, null, "Medium", "Pending", false);
  }

  // Getters and setters

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getDueDate() {
    return dueDate;
  }

  public void setDueDate(Date dueDate) {
    this.dueDate = dueDate;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isPinned() {
    return pinned;
  }

  public void setPinned(boolean pinned) {
    this.pinned = pinned;
  }

  /**
   * Toggle the pinned status of this task
   */
  public void togglePinned() {
    this.pinned = !this.pinned;
  }

  /**
   * Check if task is overdue based on current date
   */
  public boolean isOverdue() {
    if (dueDate == null) {
      return false;
    }

    if ("Completed".equals(status)) {
      return false;
    }

    Date today = new Date();

    // Reset time part for today to compare dates only
    SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd");
    String todayStr = dateOnlyFormat.format(today);
    Date todayDateOnly;
    try {
      todayDateOnly = dateOnlyFormat.parse(todayStr);
    } catch (ParseException e) {
      return false;
    }

    return dueDate.before(todayDateOnly);
  }

  /**
   * Get priority as integer value for sorting
   */
  public int getPriorityValue() {
    switch (priority) {
      case "High":
        return 0;
      case "Medium":
        return 1;
      case "Low":
        return 2;
      default:
        return 3;
    }
  }

  /**
   * Get status as integer value for sorting
   */
  public int getStatusValue() {
    switch (status) {
      case "In Progress":
        return 0;
      case "Pending":
        return 1;
      case "Completed":
        return 2;
      default:
        return 3;
    }
  }

  /**
   * Implementation of Comparable interface for default sorting
   * Orders by pinned status first, then by priority, then by due date
   */
  @Override
  public int compareTo(Task other) {
    // Pinned tasks come first
    if (this.pinned && !other.pinned)
      return -1;
    if (!this.pinned && other.pinned)
      return 1;

    // Then sort by priority
    int priorityCompare = Integer.compare(this.getPriorityValue(), other.getPriorityValue());
    if (priorityCompare != 0)
      return priorityCompare;

    // Then sort by due date
    if (this.dueDate != null && other.dueDate != null) {
      return this.dueDate.compareTo(other.dueDate);
    } else if (this.dueDate == null && other.dueDate != null) {
      return 1;
    } else if (this.dueDate != null && other.dueDate == null) {
      return -1;
    }

    // Finally sort by name
    return this.name.compareTo(other.name);
  }

  @Override
  public String toString() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String dueDateStr = (dueDate != null) ? dateFormat.format(dueDate) : "No due date";
    return name + " (Due: " + dueDateStr + ", Priority: " + priority + ", Status: " + status + ")";
  }
}
