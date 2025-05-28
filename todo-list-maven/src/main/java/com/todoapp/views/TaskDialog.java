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

public class TaskDialog extends JDialog {
    private JTextField taskField;
    private JTextField dateField;
    private JComboBox<String> priorityBox;
    private JComboBox<String> statusBox;
    private boolean confirmed = false;

    public TaskDialog(JFrame parent, String title, String taskName, String dueDate,
            String priority, String status) {
        super(parent, title, true);

        // Set dialog properties
        setResizable(false);

        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        // Form panel with grid layout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Task name components
        JLabel taskLabel = new JLabel("Task Name:");
        taskLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        taskField = new JTextField(taskName, 30);
        taskField.setBorder(BorderFactory.createCompoundBorder(
                taskField.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Due date components
        JLabel dateLabel = new JLabel("Due Date:");
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel datePanel = new JPanel(new BorderLayout(5, 0));
        dateField = new JTextField(dueDate, 15);
        dateField.setToolTipText("Format: YYYY-MM-DD");
        dateField.setBorder(BorderFactory.createCompoundBorder(
                dateField.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JButton datePickerButton = new JButton("📅");
        datePickerButton.setToolTipText("Select date from calendar");
        datePickerButton.setFocusPainted(false);
        datePickerButton.setMargin(new Insets(2, 5, 2, 5));

        datePanel.add(dateField, BorderLayout.CENTER);
        datePanel.add(datePickerButton, BorderLayout.EAST);

        // Priority components
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        priorityBox = new JComboBox<>(new String[] { "High", "Medium", "Low" });
        priorityBox.setSelectedItem(priority);
        priorityBox.setRenderer(new PriorityListCellRenderer());

        // Status components (only shown in edit mode)
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusBox = new JComboBox<>(new String[] { "Pending", "In Progress", "Completed" });
        statusBox.setSelectedItem(status);
        statusBox.setRenderer(new StatusListCellRenderer());

        // Add components to form panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(taskLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(taskField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(dateLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(datePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(priorityLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(priorityBox, gbc);

        // Only show status in edit mode (when task name is not empty)
        if (!taskName.isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 6;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            formPanel.add(statusLabel, gbc);

            gbc.gridx = 0;
            gbc.gridy = 7;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            formPanel.add(statusBox, gbc);
        }

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        // Save button - updated UI to match Cancel button
        JButton saveButton = new JButton("Save");
        saveButton.setPreferredSize(new Dimension(100, 30));
        saveButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        saveButton.setBackground(Color.WHITE);
        saveButton.setForeground(Color.BLACK);
        saveButton.setFocusPainted(false);
        saveButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cancelButton.setBackground(Color.WHITE);
        cancelButton.setForeground(Color.BLACK);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));

        // Hover effects for Save and Cancel buttons
        MouseAdapter hoverEffect = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ((JButton) e.getSource()).setBackground(new Color(240, 240, 240));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ((JButton) e.getSource()).setBackground(Color.WHITE);
            }
        };

        saveButton.addMouseListener(hoverEffect);
        cancelButton.addMouseListener(hoverEffect);

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // Add action to date picker button
        datePickerButton.addActionListener(e -> {
            // Create a special version of DatePickerDialog just for picking a single date
            JDialog dateDialog = new JDialog(this, "Select Date", true);
            dateDialog.setSize(300, 350);
            dateDialog.setLocationRelativeTo(this);

            Calendar calendar = Calendar.getInstance();
            // If date field has a valid date, use it
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

        // Add actions to buttons
        saveButton.addActionListener(e -> {
            // Validate the form
            if (validateForm()) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(e -> dispose());

        // Add components to main panel
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Set dialog properties
        setContentPane(mainPanel);

        // Set default button
        getRootPane().setDefaultButton(saveButton);

        // Focus on task name field
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                taskField.requestFocusInWindow();
                taskField.selectAll();
            }
        });

        // Pack and position the dialog
        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel createSimpleDatePicker(Calendar calendar, DateSelectedCallback callback) {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Month navigation and label
        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
        JButton prevButton = new JButton("◀");
        JButton nextButton = new JButton("▶");
        JLabel monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        headerPanel.add(prevButton, BorderLayout.WEST);
        headerPanel.add(monthYearLabel, BorderLayout.CENTER);
        headerPanel.add(nextButton, BorderLayout.EAST);

        // Weekday header
        JPanel weekdayPanel = new JPanel(new GridLayout(1, 7));
        String[] weekdays = { "S", "M", "T", "W", "T", "F", "S" };
        for (String day : weekdays) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            weekdayPanel.add(dayLabel);
        }

        // Days grid
        JPanel daysPanel = new JPanel(new GridLayout(6, 7, 2, 2));

        // Function to update calendar display
        Runnable updateCalendar = new Runnable() {
            @Override
            public void run() {
                daysPanel.removeAll();

                // Update month/year label
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

                // Add navigation button actions
                prevButton.addActionListener(e -> {
                    calendar.add(Calendar.MONTH, -1);
                    run(); // Update the calendar
                });

                nextButton.addActionListener(e -> {
                    calendar.add(Calendar.MONTH, 1);
                    run(); // Update the calendar
                });

                daysPanel.revalidate();
                daysPanel.repaint();
            }
        };

        // Initial update
        updateCalendar.run();

        // Add all components to main panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(weekdayPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(daysPanel, BorderLayout.CENTER);

        // Add today button
        JButton todayButton = new JButton("Today");
        todayButton.addActionListener(e -> {
            calendar.setTime(new Date());
            updateCalendar.run();
        });
        mainPanel.add(todayButton, BorderLayout.SOUTH);

        return mainPanel;
    }

    // Callback interface for date selection
    private interface DateSelectedCallback {
        void onDateSelected(Date date);
    }

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

        // Date format validation - allow empty for current date
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

    // Custom renderer for priority combo box
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

    // Custom renderer for status combo box
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

    // Simple colored icon for priority indicator
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

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getTaskName() {
        return taskField.getText().trim();
    }

    public String getDueDate() {
        String date = dateField.getText().trim();
        if (date.isEmpty()) {
            // Use current date if empty
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
}