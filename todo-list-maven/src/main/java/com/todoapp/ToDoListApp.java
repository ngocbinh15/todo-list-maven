package com.todoapp;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.todoapp.views.MainWindow;

public class ToDoListApp {
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    SwingUtilities.invokeLater(() -> {
      MainWindow mainWindow = new MainWindow();
      setApplicationIcon(mainWindow);

      mainWindow.setVisible(true);
    });
  }

  private static void setApplicationIcon(MainWindow window) {
    try {
      ImageIcon originalIcon = new ImageIcon(ToDoListApp.class.getResource("/icons/icon.png"));

      window.setIconImage(originalIcon.getImage());

      System.out.println("Đã thiết lập icon ứng dụng thành công");
    } catch (Exception e) {
      System.err.println("Không thể tải icon ứng dụng: " + e.getMessage());
    }
  }
}