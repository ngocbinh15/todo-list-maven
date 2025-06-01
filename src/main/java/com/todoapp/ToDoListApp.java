package com.todoapp;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.todoapp.views.MainWindow;

/**
 * Main entry point cho ToDo List Application
 * Khởi tạo Look & Feel, setup shutdown hooks và launch UI
 */
public class ToDoListApp {

  // ==================== APPLICATION STARTUP ====================

  public static void main(String[] args) {
    initializeLookAndFeel();

    SwingUtilities.invokeLater(() -> {
      MainWindow mainWindow = createMainWindow();
      setupShutdownHook(mainWindow);
      mainWindow.setVisible(true);
    });
  }

  /**
   * Initialize system look and feel
   */
  private static void initializeLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      System.err.println("Failed to set Look and Feel: " + e.getMessage());
    }
  }

  /**
   * Create and configure main window
   */
  private static MainWindow createMainWindow() {
    MainWindow mainWindow = new MainWindow();
    setApplicationIcon(mainWindow);
    return mainWindow;
  }

  // ==================== SHUTDOWN MANAGEMENT ====================

  /**
   * Register shutdown hook để auto-save khi thoát đột ngột
   */
  private static void setupShutdownHook(MainWindow mainWindow) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Application shutting down, saving data...");

      if (mainWindow != null && mainWindow.getTaskController() != null) {
        boolean saved = mainWindow.getTaskController().saveTasksFromUI();

        if (saved) {
          System.out.println("Data saved successfully on shutdown.");
        } else {
          System.err.println("Failed to save data on shutdown.");
        }
      }
    }));
  }

  // ==================== UI CONFIGURATION ====================

  /**
   * Set application icon from resources
   */
  private static void setApplicationIcon(MainWindow window) {
    try {
      ImageIcon originalIcon = new ImageIcon(ToDoListApp.class.getResource("/icons/icon.png"));
      window.setIconImage(originalIcon.getImage());
      System.out.println("Application icon set successfully");
    } catch (Exception e) {
      System.err.println("Failed to load application icon: " + e.getMessage());
      // Continue without icon - not critical
    }
  }
}