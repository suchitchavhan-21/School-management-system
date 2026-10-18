import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Student Daily Attendance Tracking System.
 * Enables teachers and school administrators to mark daily attendance,
 * search attendance logs, calculate attendance statistics, and modify records.
 * Deletions strictly require Administrator authorization via Admin.verifyAdmin.
 */
public class Student_Attendance_System extends javax.swing.JFrame {

    private Connection conn;

    // UI Components
    private JTextField txt_roll;
    private JTextField txt_name;
    private JTextField txt_class;
    private JTextField txt_date;
    private JComboBox<String> combo_status;
    private JTextField txt_remarks;

    private JTextField txt_search_roll;
    private JTextField txt_search_class;
    private JTextField txt_search_date;

    private JTable table_attendance;
    private DefaultTableModel tableModel;

    public Student_Attendance_System() {
        this.conn = javaconnect.connerDb();
        initComponents();
        loadAttendanceData();
    }

    public Student_Attendance_System(Connection conn) {
        this.conn = conn;
        initComponents();
        loadAttendanceData();
    }

    private void initComponents() {
        setTitle("Student Attendance Management System");
        setSize(980, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        // 1. Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titleLabel = new JLabel("Student Attendance Management");
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton btnBack = new JButton("Home / Dashboard");
        btnBack.setFont(new Font("Tahoma", Font.BOLD, 11));
        btnBack.setBackground(new Color(52, 73, 94));
        btnBack.setForeground(Color.WHITE);
        btnBack.addActionListener(e -> {
            new Students_and_Teachers_Management().setVisible(true);
            dispose();
        });
        headerPanel.add(btnBack, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Main Content Split (Form on Left, Table & Filters on Right)
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Left Panel: Entry Form
        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setBorder(BorderFactory.createTitledBorder("Mark Attendance"));
        formContainer.setPreferredSize(new Dimension(340, 500));

        JPanel formFields = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Roll
        gbc.gridx = 0; gbc.gridy = 0;
        formFields.add(new JLabel("Roll Number:"), gbc);
        gbc.gridx = 1;
        JPanel rollPanel = new JPanel(new BorderLayout(4, 0));
        txt_roll = new JTextField(8);
        JButton btnLookup = new JButton("Lookup");
        btnLookup.addActionListener(e -> lookupStudent());
        rollPanel.add(txt_roll, BorderLayout.CENTER);
        rollPanel.add(btnLookup, BorderLayout.EAST);
        formFields.add(rollPanel, gbc);

        // Student Name
        gbc.gridx = 0; gbc.gridy = 1;
        formFields.add(new JLabel("Student Name:"), gbc);
        gbc.gridx = 1;
        txt_name = new JTextField(14);
        formFields.add(txt_name, gbc);

        // Class
        gbc.gridx = 0; gbc.gridy = 2;
        formFields.add(new JLabel("Class / Grade:"), gbc);
        gbc.gridx = 1;
        txt_class = new JTextField(14);
        formFields.add(txt_class, gbc);

        // Attendance Date
        gbc.gridx = 0; gbc.gridy = 3;
        formFields.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        txt_date = new JTextField(14);
        txt_date.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        formFields.add(txt_date, gbc);

        // Status
        gbc.gridx = 0; gbc.gridy = 4;
        formFields.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        combo_status = new JComboBox<>(new String[]{"Present", "Absent", "Late", "Excused"});
        formFields.add(combo_status, gbc);

        // Remarks
        gbc.gridx = 0; gbc.gridy = 5;
        formFields.add(new JLabel("Remarks:"), gbc);
        gbc.gridx = 1;
        txt_remarks = new JTextField(14);
        formFields.add(txt_remarks, gbc);

        formContainer.add(formFields, BorderLayout.CENTER);

        // Form Buttons
        JPanel formBtnGrid = new JPanel(new GridLayout(2, 2, 6, 6));
        formBtnGrid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton btnSave = new JButton("Save / Mark");
        btnSave.setBackground(new Color(39, 174, 96));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnSave.addActionListener(e -> saveAttendance());

        JButton btnDelete = new JButton("Delete Record");
        btnDelete.setBackground(new Color(192, 57, 43));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnDelete.addActionListener(e -> deleteAttendance());

        JButton btnSummary = new JButton("View Summary");
        btnSummary.setBackground(new Color(142, 68, 173));
        btnSummary.setForeground(Color.WHITE);
        btnSummary.setFont(new Font("Tahoma", Font.BOLD, 11));
        btnSummary.addActionListener(e -> showSummary());

        JButton btnClear = new JButton("Clear Form");
        btnClear.addActionListener(e -> clearForm());

        formBtnGrid.add(btnSave);
        formBtnGrid.add(btnDelete);
        formBtnGrid.add(btnSummary);
        formBtnGrid.add(btnClear);

        formContainer.add(formBtnGrid, BorderLayout.SOUTH);
        mainPanel.add(formContainer, BorderLayout.WEST);

        // Right Panel: Table & Filters
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filterBar.setBorder(BorderFactory.createTitledBorder("Search & Filter"));

        filterBar.add(new JLabel("Roll:"));
        txt_search_roll = new JTextField(5);
        filterBar.add(txt_search_roll);

        filterBar.add(new JLabel("Class:"));
        txt_search_class = new JTextField(4);
        filterBar.add(txt_search_class);

        filterBar.add(new JLabel("Date:"));
        txt_search_date = new JTextField(8);
        filterBar.add(txt_search_date);

        JButton btnSearch = new JButton("Filter");
        btnSearch.addActionListener(e -> filterData());
        filterBar.add(btnSearch);

        JButton btnResetFilter = new JButton("Show All");
        btnResetFilter.addActionListener(e -> {
            txt_search_roll.setText("");
            txt_search_class.setText("");
            txt_search_date.setText("");
            loadAttendanceData();
        });
        filterBar.add(btnResetFilter);

        rightPanel.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Roll", "Name", "Class", "Date", "Status", "Remarks"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table_attendance = new JTable(tableModel);
        table_attendance.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table_attendance.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                populateFormFromSelectedRow();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table_attendance);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(rightPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void lookupStudent() {
        String rollStr = txt_roll.getText().trim();
        if (rollStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Roll number to lookup.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int roll = Integer.parseInt(rollStr);
            String sql = "SELECT First_name, Last_name, Class FROM student_info WHERE Roll = ?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, roll);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        String fullName = rs.getString("First_name") + " " + rs.getString("Last_name");
                        int studentClass = rs.getInt("Class");
                        txt_name.setText(fullName.trim());
                        txt_class.setText(String.valueOf(studentClass));
                    } else {
                        JOptionPane.showMessageDialog(this, "No student found with Roll number: " + roll, "Not Found", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Roll number must be a valid integer.", "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database lookup error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAttendance() {
        String rollStr = txt_roll.getText().trim();
        String name = txt_name.getText().trim();
        String classStr = txt_class.getText().trim();
        String date = txt_date.getText().trim();
        String status = (String) combo_status.getSelectedItem();
        String remarks = txt_remarks.getText().trim();

        if (rollStr.isEmpty() || name.isEmpty() || classStr.isEmpty() || date.isEmpty() || status == null) {
            JOptionPane.showMessageDialog(this, "Please complete Roll, Name, Class, Date, and Status fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int roll = Integer.parseInt(rollStr);
            int studentClass = Integer.parseInt(classStr);

            // Basic ISO date format validation: YYYY-MM-DD
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(this, "Date must follow YYYY-MM-DD format (e.g. 2026-10-18).", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            AttendanceDAO.AttendanceRecord record = new AttendanceDAO.AttendanceRecord(
                    roll, name, studentClass, date, status, remarks);

            boolean saved = AttendanceDAO.saveOrUpdate(conn, record);
            if (saved) {
                JOptionPane.showMessageDialog(this, "Attendance marked successfully for Roll " + roll + " on " + date + ".", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAttendanceData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save attendance record.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Roll and Class must be numeric integers.", "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error saving attendance: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAttendance() {
        String rollStr = txt_roll.getText().trim();
        String date = txt_date.getText().trim();

        if (rollStr.isEmpty() || date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an attendance record or specify Roll and Date to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int roll = Integer.parseInt(rollStr);

            // Administrator authorization guard
            if (!Admin.verifyAdmin(this, conn)) {
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to permanently delete the attendance record for Roll " + roll + " on " + date + "?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            boolean deleted = AttendanceDAO.delete(conn, roll, date);
            if (deleted) {
                JOptionPane.showMessageDialog(this, "Attendance record deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadAttendanceData();
            } else {
                JOptionPane.showMessageDialog(this, "No matching record found to delete.", "Not Found", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Roll must be a valid integer.", "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error deleting attendance: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSummary() {
        String rollStr = txt_roll.getText().trim();
        if (rollStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify a Student Roll number to view attendance summary.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int roll = Integer.parseInt(rollStr);
            AttendanceDAO.AttendanceSummary summary = AttendanceDAO.calculateSummary(conn, roll);

            String message = String.format(
                    "Attendance Summary for Student Roll: %d\n" +
                    "-----------------------------------------\n" +
                    "Total Tracked Days:   %d\n" +
                    "Present Days:         %d\n" +
                    "Late Days:            %d\n" +
                    "Excused Days:         %d\n" +
                    "Absent Days:          %d\n" +
                    "-----------------------------------------\n" +
                    "Overall Attendance:   %.2f%%",
                    summary.getRoll(),
                    summary.getTotalDays(),
                    summary.getPresentDays(),
                    summary.getLateDays(),
                    summary.getExcusedDays(),
                    summary.getAbsentDays(),
                    summary.getAttendancePercentage()
            );

            JOptionPane.showMessageDialog(this, message, "Attendance Analytics", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Roll must be an integer.", "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error calculating summary: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateFormFromSelectedRow() {
        int selectedRow = table_attendance.getSelectedRow();
        if (selectedRow >= 0) {
            txt_roll.setText(tableModel.getValueAt(selectedRow, 1).toString());
            txt_name.setText(tableModel.getValueAt(selectedRow, 2).toString());
            txt_class.setText(tableModel.getValueAt(selectedRow, 3).toString());
            txt_date.setText(tableModel.getValueAt(selectedRow, 4).toString());
            combo_status.setSelectedItem(tableModel.getValueAt(selectedRow, 5).toString());
            Object remarks = tableModel.getValueAt(selectedRow, 6);
            txt_remarks.setText(remarks != null ? remarks.toString() : "");
        }
    }

    private void clearForm() {
        txt_roll.setText("");
        txt_name.setText("");
        txt_class.setText("");
        txt_date.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        combo_status.setSelectedIndex(0);
        txt_remarks.setText("");
        table_attendance.clearSelection();
    }

    private void loadAttendanceData() {
        tableModel.setRowCount(0);
        try {
            List<AttendanceDAO.AttendanceRecord> records = AttendanceDAO.findAll(conn);
            for (AttendanceDAO.AttendanceRecord r : records) {
                tableModel.addRow(new Object[]{
                        r.getAttendanceId(),
                        r.getRoll(),
                        r.getStudentName(),
                        r.getStudentClass(),
                        r.getAttendanceDate(),
                        r.getStatus(),
                        r.getRemarks()
                });
            }
        } catch (SQLException e) {
            System.err.println("[Student_Attendance_System] Error loading attendance: " + e.getMessage());
        }
    }

    private void filterData() {
        String searchRoll = txt_search_roll.getText().trim();
        String searchClass = txt_search_class.getText().trim();
        String searchDate = txt_search_date.getText().trim();

        tableModel.setRowCount(0);
        try {
            if (!searchRoll.isEmpty()) {
                int roll = Integer.parseInt(searchRoll);
                List<AttendanceDAO.AttendanceRecord> records = AttendanceDAO.findByRoll(conn, roll);
                for (AttendanceDAO.AttendanceRecord r : records) {
                    tableModel.addRow(new Object[]{
                            r.getAttendanceId(), r.getRoll(), r.getStudentName(),
                            r.getStudentClass(), r.getAttendanceDate(), r.getStatus(), r.getRemarks()
                    });
                }
            } else if (!searchClass.isEmpty() && !searchDate.isEmpty()) {
                int cls = Integer.parseInt(searchClass);
                List<AttendanceDAO.AttendanceRecord> records = AttendanceDAO.findByClassAndDate(conn, cls, searchDate);
                for (AttendanceDAO.AttendanceRecord r : records) {
                    tableModel.addRow(new Object[]{
                            r.getAttendanceId(), r.getRoll(), r.getStudentName(),
                            r.getStudentClass(), r.getAttendanceDate(), r.getStatus(), r.getRemarks()
                    });
                }
            } else {
                loadAttendanceData();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Roll and Class filters must be integers.", "Filter Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error during filter: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Student_Attendance_System().setVisible(true));
    }
}
