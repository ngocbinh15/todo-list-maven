package com.todoapp.utils;

import com.todoapp.components.TaskTable;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;

public class PinnedRowSorter extends TableRowSorter<TableModel> {
    private TaskTable taskTable;
    private LinkedHashSet<Integer> pinnedTaskRows;
    
    public PinnedRowSorter(TableModel model, TaskTable taskTable, LinkedHashSet<Integer> pinnedTaskRows) {
        super(model);
        this.taskTable = taskTable;
        this.pinnedTaskRows = pinnedTaskRows;
        
        // Set default sort keys - sort by task name (column 0) ascending by default
        setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
    }
    
    @Override
    public void toggleSortOrder(int column) {
        List<SortKey> sortKeys = new ArrayList<>(getSortKeys());
        
        if (sortKeys.size() > 0) {
            SortKey sortKey = sortKeys.get(0);
            if (sortKey.getColumn() == column) {
                // Đảo ngược thứ tự sắp xếp cho cột hiện tại
                SortOrder newOrder = sortKey.getSortOrder() == SortOrder.ASCENDING ? 
                        SortOrder.DESCENDING : SortOrder.ASCENDING;
                sortKeys.set(0, new SortKey(column, newOrder));
            } else {
                // Sắp xếp theo cột mới
                sortKeys.set(0, new SortKey(column, SortOrder.ASCENDING));
            }
        } else {
            // Không có khóa sắp xếp trước đó
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
                // Tìm các hàng từ giá trị ô
                int row1 = findModelRowForValue(column, o1);
                int row2 = findModelRowForValue(column, o2);
                
                // Kiểm tra xem hàng nào được ghim
                boolean pin1 = pinnedTaskRows.contains(row1);
                boolean pin2 = pinnedTaskRows.contains(row2);
                
                // Các hàng ghim luôn ở trên cùng
                if (pin1 && !pin2) return -1;
                if (!pin1 && pin2) return 1;
                
                // Nếu cả hai hàng đều được ghim, giữ thứ tự pinnedTaskRows (LinkedHashSet)
                if (pin1 && pin2) {
                    return getPinnedPosition(row1) - getPinnedPosition(row2);
                }
                
                // Xử lý đặc biệt cho cột Priority để sắp xếp High-Medium-Low đúng thứ tự
                if (column == 2) { // Cột Priority
                    return Integer.compare(getPriorityValue(o1.toString()), 
                                         getPriorityValue(o2.toString()));
                }
                
                // Xử lý đặc biệt cho cột Status để sắp xếp theo thứ tự tùy chỉnh
                if (column == 3) { // Cột Status
                    return Integer.compare(getStatusValue(o1.toString()),
                                         getStatusValue(o2.toString()));
                }
                
                // Xử lý đặc biệt cho cột Due Date để sắp xếp đúng ngày tháng
                if (column == 1) { // Cột Due Date
                    return compareDates(o1.toString(), o2.toString());
                }
                
                // Các cột khác sử dụng bộ so sánh mặc định
                return ((Comparator<Object>) defaultComparator).compare(o1, o2);
            }
        };
    }
    
    // Lấy giá trị số cho việc so sánh Priority
    private int getPriorityValue(String priority) {
        switch (priority) {
            case "High": return 0;
            case "Medium": return 1; 
            case "Low": return 2;
            default: return 3;
        }
    }
    
    // Lấy giá trị số cho việc so sánh Status
    private int getStatusValue(String status) {
        switch (status) {
            case "In Progress": return 0;
            case "Pending": return 1;
            case "Completed": return 2;
            default: return 3;
        }
    }
    
    // So sánh ngày tháng dạng YYYY-MM-DD
    private int compareDates(String date1, String date2) {
        try {
            // Nếu là chuỗi ngày hợp lệ, so sánh theo thứ tự
            if (date1.matches("\\d{4}-\\d{2}-\\d{2}") && date2.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return date1.compareTo(date2);
            }
        } catch (Exception e) {
            // Nếu có lỗi, sử dụng so sánh chuỗi thông thường
        }
        return date1.compareTo(date2);
    }
    
    // Lấy vị trí của một hàng đã ghim trong tập hợp các hàng ghim
    private int getPinnedPosition(int modelRow) {
        int position = 0;
        for (Integer pinnedRow : pinnedTaskRows) {
            if (pinnedRow == modelRow) {
                return position;
            }
            position++;
        }
        return Integer.MAX_VALUE;  // Trả về giá trị lớn nhất nếu không tìm thấy
    }
    
    // Tìm hàng trong model dựa trên giá trị của ô
    private int findModelRowForValue(int column, Object value) {
        DefaultTableModel model = (DefaultTableModel) getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object cellValue = model.getValueAt(i, column);
            if ((cellValue == null && value == null) || 
                (cellValue != null && cellValue.equals(value))) {
                return i;
            }
        }
        return -1;  // Không tìm thấy
    }
    
    // Áp dụng sắp xếp mặc định và đảm bảo các hàng ghim ở đầu
    public void applySorting() {
        // Đặt khóa sắp xếp mặc định nếu không có
        if (getSortKeys().isEmpty()) {
            setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        }
        
        // Cập nhật sắp xếp
        sort();
    }
}