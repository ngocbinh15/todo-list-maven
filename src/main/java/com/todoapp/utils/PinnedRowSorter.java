package com.todoapp.utils;

import com.todoapp.components.TaskTable;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;

/**
 * Custom TableRowSorter với khả năng giữ các pinned tasks ở đầu danh sách
 * Hỗ trợ sắp xếp đặc biệt cho Priority (High-Medium-Low) và Status
 */
public class PinnedRowSorter extends TableRowSorter<TableModel> {
    private final TaskTable taskTable;
    private final LinkedHashSet<Integer> pinnedTaskRows;

    // Constants for sorting order
    private static final Map<String, Integer> PRIORITY_ORDER = Map.of(
            "High", 0, "Medium", 1, "Low", 2);

    private static final Map<String, Integer> STATUS_ORDER = Map.of(
            "In Progress", 0, "Pending", 1, "Completed", 2);

    public PinnedRowSorter(TableModel model, TaskTable taskTable, LinkedHashSet<Integer> pinnedTaskRows) {
        super(model);
        this.taskTable = taskTable;
        this.pinnedTaskRows = pinnedTaskRows;

        // Sắp xếp mặc định theo tên task (cột 0) tăng dần
        setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
    }

    @Override
    public void toggleSortOrder(int column) {
        List<SortKey> sortKeys = new ArrayList<>(getSortKeys());

        if (!sortKeys.isEmpty()) {
            SortKey currentKey = sortKeys.get(0);
            if (currentKey.getColumn() == column) {
                // Đảo thứ tự sắp xếp cho cột hiện tại
                SortOrder newOrder = currentKey.getSortOrder() == SortOrder.ASCENDING ? SortOrder.DESCENDING
                        : SortOrder.ASCENDING;
                sortKeys.set(0, new SortKey(column, newOrder));
            } else {
                // Chuyển sang cột mới
                sortKeys.set(0, new SortKey(column, SortOrder.ASCENDING));
            }
        } else {
            // Khởi tạo sắp xếp mới
            sortKeys.add(new SortKey(column, SortOrder.ASCENDING));
        }

        setSortKeys(sortKeys);
    }

    @Override
    public Comparator<?> getComparator(int column) {
        final Comparator<?> defaultComparator = super.getComparator(column);

        return new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public int compare(Object o1, Object o2) {
                int row1 = findModelRowForValue(column, o1);
                int row2 = findModelRowForValue(column, o2);

                // Xử lý pinned tasks - luôn ở đầu
                boolean pin1 = pinnedTaskRows.contains(row1);
                boolean pin2 = pinnedTaskRows.contains(row2);

                if (pin1 && !pin2)
                    return -1;
                if (!pin1 && pin2)
                    return 1;

                // Cả hai đều pinned - giữ thứ tự trong LinkedHashSet
                if (pin1 && pin2) {
                    return Integer.compare(getPinnedPosition(row1), getPinnedPosition(row2));
                }

                // Sắp xếp theo loại cột
                return compareByColumnType(column, o1, o2, defaultComparator);
            }
        };
    }

    /**
     * So sánh theo loại cột cụ thể
     */
    @SuppressWarnings("unchecked")
    private int compareByColumnType(int column, Object o1, Object o2, Comparator<?> defaultComparator) {
        switch (column) {
            case 1: // Due Date
                return compareDates(o1.toString(), o2.toString());
            case 2: // Priority
                return Integer.compare(
                        PRIORITY_ORDER.getOrDefault(o1.toString(), 999),
                        PRIORITY_ORDER.getOrDefault(o2.toString(), 999));
            case 3: // Status
                return Integer.compare(
                        STATUS_ORDER.getOrDefault(o1.toString(), 999),
                        STATUS_ORDER.getOrDefault(o2.toString(), 999));
            default: // Task Name và các cột khác
                return ((Comparator<Object>) defaultComparator).compare(o1, o2);
        }
    }

    /**
     * So sánh ngày tháng định dạng YYYY-MM-DD
     */
    private int compareDates(String date1, String date2) {
        // Xử lý chuỗi rỗng
        if (date1.isEmpty() && date2.isEmpty())
            return 0;
        if (date1.isEmpty())
            return 1; // Ngày rỗng xếp cuối
        if (date2.isEmpty())
            return -1;

        try {
            // Kiểm tra định dạng ngày hợp lệ và so sánh
            if (isValidDateFormat(date1) && isValidDateFormat(date2)) {
                return date1.compareTo(date2);
            }
        } catch (Exception e) {
            // Fallback to string comparison
        }

        return date1.compareTo(date2);
    }

    /**
     * Kiểm tra định dạng ngày hợp lệ
     */
    private boolean isValidDateFormat(String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    /**
     * Lấy vị trí của task đã pin trong danh sách pinned
     */
    private int getPinnedPosition(int modelRow) {
        int position = 0;
        for (Integer pinnedRow : pinnedTaskRows) {
            if (pinnedRow.equals(modelRow)) {
                return position;
            }
            position++;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Tìm model row từ giá trị cell
     */
    private int findModelRowForValue(int column, Object value) {
        DefaultTableModel model = (DefaultTableModel) getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            Object cellValue = model.getValueAt(i, column);
            if (Objects.equals(cellValue, value)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Áp dụng sắp xếp với pinned tasks ở đầu
     */
    public void applySorting() {
        if (getSortKeys().isEmpty()) {
            setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        }
        sort();
    }

    /**
     * Cập nhật danh sách pinned tasks và refresh sắp xếp
     */
    public void updatePinnedTasks(LinkedHashSet<Integer> newPinnedRows) {
        pinnedTaskRows.clear();
        pinnedTaskRows.addAll(newPinnedRows);
        applySorting();
    }
}