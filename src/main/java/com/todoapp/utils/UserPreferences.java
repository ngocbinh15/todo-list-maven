package com.todoapp.utils;

import java.util.prefs.Preferences;

/**
 * Quản lý user preferences cho application
 */
public class UserPreferences {
  private static final Preferences prefs = Preferences.userNodeForPackage(UserPreferences.class);

  // Auto-fill today's date setting
  private static final String AUTO_FILL_TODAY = "auto_fill_today";

  public static boolean isAutoFillTodayEnabled() {
    return prefs.getBoolean(AUTO_FILL_TODAY, true); // Default: enabled
  }

  public static void setAutoFillToday(boolean enabled) {
    prefs.putBoolean(AUTO_FILL_TODAY, enabled);
  }
}