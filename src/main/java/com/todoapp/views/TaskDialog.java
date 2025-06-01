package com.todoapp.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.todoapp.utils.UserPreferences;

/**
 * Dialog for adding/editing tasks
 * Provides form input with validation and date picker
 */
public class TaskDialog extends JDialog {

    // ==================== COMPONENTS ====================
    private JTextField taskField;
    private JTextField dateField;
    private JComboBox<String> priorityBox;
    private JComboBox<String> statusBox;
    private boolean confirmed = false;

    // ==================== INITIALIZATION ====================

    public TaskDialog(JFrame parent, String title, String taskName, String dueDate,
            String priority, String status) {
        super(parent, title, true);

        setResizable(false);
        createMainLayout(taskName, dueDate, priority, status);
        setupEventHandlers();
        finalizeDialog(parent);
    }

    /**
     * Create main layout structure
     */
    private void createMainLayout(String taskName, String dueDate, String priority, String status) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JPanel formPanel = createFormPanel(taskName, dueDate, priority, status);
        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    // ==================== FORM CREATION ====================

    /**
     * Create form panel with input fields
     */
    private JPanel createFormPanel(String taskName, String dueDate, String priority, String status) {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        addTaskNameField(formPanel, gbc, taskName);
        addDateField(formPanel, gbc, dueDate);
        addPriorityField(formPanel, gbc, priority);

        // Only show status in edit mode
        if (!taskName.isEmpty()) {
            addStatusField(formPanel, gbc, status);
        }

        return formPanel;
    }

    /**
     * Add task name input field
     */
    private void addTaskNameField(JPanel parent, GridBagConstraints gbc, String taskName) {
        JLabel taskLabel = new JLabel("Task Name:");
        taskLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        taskField = new JTextField(taskName, 30);
        taskField.setBorder(BorderFactory.createCompoundBorder(
                taskField.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        parent.add(taskLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        parent.add(taskField, gbc);
    }

    /**
     * Add date input field với calendar picker và auto-select today
     */
    private void addDateField(JPanel parent, GridBagConstraints gbc, String dueDate) {
        JLabel dateLabel = new JLabel("Due Date:");
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel datePanel = createDatePanel(dueDate);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        parent.add(dateLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        parent.add(datePanel, gbc);
    }

    /**
     * Create date panel với conditional auto-select today
     */
    private JPanel createDatePanel(String dueDate) {
        JPanel datePanel = new JPanel(new BorderLayout(5, 0));

        // Auto-fill today's date based on user preference
        String defaultDate = dueDate;
        if ((dueDate == null || dueDate.trim().isEmpty()) && UserPreferences.isAutoFillTodayEnabled()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            defaultDate = dateFormat.format(new Date());
        }

        dateField = new JTextField(defaultDate, 15);
        dateField.setToolTipText("Format: YYYY-MM-DD (Auto-filled with today's date)");
        dateField.setBorder(BorderFactory.createCompoundBorder(
                dateField.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JButton datePickerButton = new JButton("📅");
        datePickerButton.setToolTipText("Select date from calendar");
        datePickerButton.setFocusPainted(false);
        datePickerButton.setMargin(new Insets(2, 5, 2, 5));

        // Add "Today" button for quick selection
        JButton todayButton = new JButton("Today");
        todayButton.setToolTipText("Set to today's date");
        todayButton.setFocusPainted(false);
        todayButton.setMargin(new Insets(2, 8, 2, 8));
        todayButton.setFont(new Font("SansSerif", Font.PLAIN, 10));

        setupDatePickerAction(datePickerButton);
        setupTodayButtonAction(todayButton);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        buttonPanel.add(datePickerButton);
        buttonPanel.add(todayButton);

        datePanel.add(dateField, BorderLayout.CENTER);
        datePanel.add(buttonPanel, BorderLayout.EAST);

        return datePanel;
    }

    /**
     * Setup "Today" button action
     */
    private void setupTodayButtonAction(JButton todayButton) {
        todayButton.addActionListener(e -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateField.setText(dateFormat.format(new Date()));
        });
    }

    /**
     * Add priority selection field
     */
    private void addPriorityField(JPanel parent, GridBagConstraints gbc, String priority) {
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        priorityBox = new JComboBox<>(new String[] { "High", "Medium", "Low" });
        priorityBox.setSelectedItem(priority);
        priorityBox.setRenderer(new PriorityListCellRenderer());

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        parent.add(priorityLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        parent.add(priorityBox, gbc);
    }

    /**
     * Add status selection field (edit mode only)
     */
    private void addStatusField(JPanel parent, GridBagConstraints gbc, String status) {
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        statusBox = new JComboBox<>(new String[] { "Pending", "In Progress", "Completed" });
        statusBox.setSelectedItem(status);
        statusBox.setRenderer(new StatusListCellRenderer());

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        parent.add(statusLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        parent.add(statusBox, gbc);
    }

    /**
     * Create button panel with Save and Cancel
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        JButton saveButton = createStyledButton("Save");
        JButton cancelButton = createStyledButton("Cancel");

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }

    /**
     * Create button with consistent styling
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(100, 30));
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(240, 240, 240));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });

        return button;
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Setup event handlers for dialog
     */
    private void setupEventHandlers() {
        JPanel buttonPanel = (JPanel) ((JPanel) getContentPane()).getComponent(1);
        JButton saveButton = (JButton) buttonPanel.getComponent(0);
        JButton cancelButton = (JButton) buttonPanel.getComponent(1);

        saveButton.addActionListener(e -> {
            if (validateForm()) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(e -> dispose());

        getRootPane().setDefaultButton(saveButton);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                taskField.requestFocusInWindow();
                taskField.selectAll();
            }
        });
    }

    /**
     * Setup date picker button action
     */
    private void setupDatePickerAction(JButton datePickerButton) {
        datePickerButton.addActionListener(e -> {
            JDialog dateDialog = new JDialog(this, "Select Date", true);
            dateDialog.setSize(300, 350);
            dateDialog.setLocationRelativeTo(this);

            Calendar calendar = Calendar.getInstance();

            // Parse existing date if valid
            if (!dateField.getText().isEmpty()) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = dateFormat.parse(dateField.getText());
                    calendar.setTime(date);
                } catch (Exception ex) {
                    // Use current date if parsing fails
                }
            }

            JPanel calendarPanel = createSimpleDatePicker(calendar, date -> {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateField.setText(dateFormat.format(date));
                dateDialog.dispose();
            });

            dateDialog.add(calendarPanel);
            dateDialog.setVisible(true);
        });
    }

    private void finalizeDialog(JFrame parent) {
        pack();
        setLocationRelativeTo(parent);
    }

    // ==================== DATE PICKER ====================

    /**
     * Create simple date picker component
     */
    private JPanel createSimpleDatePicker(Calendar calendar, DateSelectedCallback callback) {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = createCalendarHeader(calendar);
        JPanel weekdayPanel = createWeekdayHeader();
        JPanel daysPanel = new JPanel(new GridLayout(6, 7, 2, 2));

        // Update calendar display function
        Runnable updateCalendar = createUpdateCalendarFunction(calendar, headerPanel, daysPanel, callback);
        updateCalendar.run();

        JButton todayButton = new JButton("Today");
        todayButton.addActionListener(e -> {
            calendar.setTime(new Date());
            updateCalendar.run();
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(weekdayPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(daysPanel, BorderLayout.CENTER);
        mainPanel.add(todayButton, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * Create calendar header with navigation
     */
    private JPanel createCalendarHeader(Calendar calendar) {
        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));

        JButton prevButton = new JButton("◀");
        JButton nextButton = new JButton("▶");
        JLabel monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        headerPanel.add(prevButton, BorderLayout.WEST);
        headerPanel.add(monthYearLabel, BorderLayout.CENTER);
        headerPanel.add(nextButton, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Create weekday header
     */
    private JPanel createWeekdayHeader() {
        JPanel weekdayPanel = new JPanel(new GridLayout(1, 7));
        String[] weekdays = { "S", "M", "T", "W", "T", "F", "S" };

        for (String day : weekdays) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            weekdayPanel.add(dayLabel);
        }

        return weekdayPanel;
    }

    /**
     * Create update calendar function
     */
    private Runnable createUpdateCalendarFunction(Calendar calendar, JPanel headerPanel,
            JPanel daysPanel, DateSelectedCallback callback) {
        return new Runnable() {
            @Override
            public void run() {
                daysPanel.removeAll();

                // Update month/year label
                JLabel monthYearLabel = (JLabel) headerPanel.getComponent(1);
                SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy");
                monthYearLabel.setText(monthYearFormat.format(calendar.getTime()));

                // Clone calendar to avoid modifying the original
                Calendar tempCal = (Calendar) calendar.clone();
                tempCal.set(Calendar.DAY_OF_MONTH, 1);

                int firstDayOfMonth = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
                int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

                // Add empty spaces before first day
                for (int i = 0; i < firstDayOfMonth; i++) {
                    daysPanel.add(new JLabel());
                }

                // Get current day for highlighting
                Calendar todayCal = Calendar.getInstance();
                boolean sameMonth = todayCal.get(Calendar.MONTH) == tempCal.get(Calendar.MONTH) &&
                        todayCal.get(Calendar.YEAR) == tempCal.get(Calendar.YEAR);
                int today = sameMonth ? todayCal.get(Calendar.DAY_OF_MONTH) : -1;

                // Add day buttons
                for (int day = 1; day <= daysInMonth; day++) {
                    JButton dayButton = new JButton(String.valueOf(day));
                    dayButton.setMargin(new Insets(1, 1, 1, 1));
                    dayButton.setFocusPainted(false);

                    // Highlight today
                    if (day == today) {
                        dayButton.setBackground(new Color(173, 216, 230));
                        dayButton.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215), 1));
                    }

                    // Set action to select date
                    final int selectedDay = day;
                    dayButton.addActionListener(e -> {
                        Calendar selectedCal = (Calendar) calendar.clone();
                        selectedCal.set(Calendar.DAY_OF_MONTH, selectedDay);
                        callback.onDateSelected(selectedCal.getTime());
                    });

                    daysPanel.add(dayButton);
                }

                // Setup navigation buttons
                JButton prevButton = (JButton) headerPanel.getComponent(0);
                JButton nextButton = (JButton) headerPanel.getComponent(2);

                // Clear existing listeners
                for (var listener : prevButton.getActionListeners()) {
                    prevButton.removeActionListener(listener);
                }
                for (var listener : nextButton.getActionListeners()) {
                    nextButton.removeActionListener(listener);
                }

                prevButton.addActionListener(e -> {
                    calendar.add(Calendar.MONTH, -1);
                    run();
                });

                nextButton.addActionListener(e -> {
                    calendar.add(Calendar.MONTH, 1);
                    run();
                });

                daysPanel.revalidate();
                daysPanel.repaint();
            }
        };
    }

    // ==================== VALIDATION ====================

    /**
     * Validate form inputs
     */
    private boolean validateForm() {
        // Task name validation
        if (taskField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a task name.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            taskField.requestFocusInWindow();
            return false;
        }

        // Date format validation
        if (!dateField.getText().trim().isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateFormat.setLenient(false);
                dateFormat.parse(dateField.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid date in format YYYY-MM-DD.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE);
                dateField.requestFocusInWindow();
                return false;
            }
        }

        return true;
    }

    // ==================== CUSTOM RENDERERS ====================

    /**
     * Custom renderer for priority combo box
     */
    private class PriorityListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value != null) {
                String priority = value.toString();
                switch (priority) {
                    case "High":
                        if (!isSelected)
                            label.setBackground(new Color(255, 200, 200));
                        label.setIcon(new ColorIcon(10, 10, new Color(255, 80, 80)));
                        break;
                    case "Medium":
                        if (!isSelected)
                            label.setBackground(new Color(255, 235, 200));
                        label.setIcon(new ColorIcon(10, 10, new Color(255, 180, 0)));
                        break;
                    case "Low":
                        if (!isSelected)
                            label.setBackground(new Color(220, 255, 220));
                        label.setIcon(new ColorIcon(10, 10, new Color(100, 180, 100)));
                        break;
                }
                label.setIconTextGap(10);
            }

            return label;
        }
    }

    /**
     * Custom renderer for status combo box
     */
    private class StatusListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value != null) {
                String status = value.toString();
                switch (status) {
                    case "Pending":
                        if (!isSelected)
                            label.setBackground(new Color(240, 240, 240));
                        label.setText("⌛ Pending");
                        break;
                    case "In Progress":
                        if (!isSelected)
                            label.setBackground(new Color(230, 220, 255));
                        label.setText("⏳ In Progress");
                        break;
                    case "Completed":
                        if (!isSelected)
                            label.setBackground(new Color(200, 230, 255));
                        label.setText("✓ Completed");
                        break;
                }
            }

            return label;
        }
    }

    // ==================== UTILITY CLASSES ====================

    /**
     * Simple colored icon for priority indicator
     */
    private class ColorIcon implements Icon {
        private int width;
        private int height;
        private Color color;

        public ColorIcon(int width, int height, Color color) {
            this.width = width;
            this.height = height;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, width, height);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, width, height);
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    /**
     * Callback interface for date selection
     */
    private interface DateSelectedCallback {
        void onDateSelected(Date date);
    }

    // ==================== GETTERS ====================

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getTaskName() {
        return taskField.getText().trim();
    }

    public String getDueDate() {
        String date = dateField.getText().trim();
        if (date.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.format(new Date());
        }
        return date;
    }

    public String getPriority() {
        return (String) priorityBox.getSelectedItem();
    }

    public String getStatus() {
        return statusBox != null ? (String) statusBox.getSelectedItem() : "Pending";
    }

    public Date getDueDateObject() {
        try {
            if (dateField.getText().trim().isEmpty()) {
                return null;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.parse(dateField.getText());
        } catch (ParseException e) {
            return null;
        }
    }
}