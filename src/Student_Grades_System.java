import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Student Examination Marks & Grade Report Card Management System.
 * Production-ready Swing GUI providing marks entry, automated grading,
 * live tabular overview, search, and official report card generation.
 */
public class Student_Grades_System extends javax.swing.JFrame {

    private Connection conn = null;
    private javax.swing.Timer clockTimer;

    // UI Components
    private JTextField txt_roll;
    private JTextField txt_name;
    private JTextField txt_class;
    private JComboBox<String> combo_term;
    private JTextField txt_year;

    private JTextField txt_english;
    private JTextField txt_math;
    private JTextField txt_science;
    private JTextField txt_social;
    private JTextField txt_ict;

    private JTextField txt_total;
    private JTextField txt_percentage;
    private JTextField txt_grade;
    private JTextField txt_gpa;
    private JTextField txt_remarks;

    private JTextField txt_search;
    private JTable table_marks;
    private DefaultTableModel tableModel;

    private JLabel lbl_date;
    private JLabel lbl_time;
    private JLabel lbl_db_status;

    public Student_Grades_System() {
        initComponents();
        conn = javaconnect.connerDb();
        initClock();
        loadTableData();
    }

    public Student_Grades_System(int prefillRoll) {
        this();
        txt_roll.setText(String.valueOf(prefillRoll));
        fetchStudentInfo();
    }

    private void initClock() {
        clockTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
                Date now = new Date();
                lbl_date.setText("Date: " + df.format(now));
                lbl_time.setText("Time: " + tf.format(now));
            }
        });
        clockTimer.start();
    }

    @Override
    public void dispose() {
        if (clockTimer != null && clockTimer.isRunning()) {
            clockTimer.stop();
        }
        super.dispose();
    }

    private void initComponents() {
        setTitle("Student Examination & Grade Report Management");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1080, 720);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout(10, 10));

        // Top Header Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(24, 44, 97));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("School Management System - Academic Examination & Grades");
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel clockPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        clockPanel.setOpaque(false);
        lbl_date = new JLabel("Date: --/--/----");
        lbl_date.setForeground(new Color(220, 220, 220));
        lbl_date.setFont(new Font("Tahoma", Font.PLAIN, 13));
        lbl_time = new JLabel("Time: --:--:--");
        lbl_time.setForeground(new Color(220, 220, 220));
        lbl_time.setFont(new Font("Tahoma", Font.PLAIN, 13));
        lbl_db_status = new JLabel("DB: " + javaconnect.getActiveDbType());
        lbl_db_status.setForeground(new Color(129, 236, 236));
        lbl_db_status.setFont(new Font("Tahoma", Font.BOLD, 13));

        clockPanel.add(lbl_date);
        clockPanel.add(lbl_time);
        clockPanel.add(lbl_db_status);
        topPanel.add(clockPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center Content Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(520);
        splitPane.setContinuousLayout(true);

        // Left Panel: Input & Calculations
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 73, 94), 1),
                "Student Exam Information & Marks Entry",
                0, 0, new Font("Tahoma", Font.BOLD, 14), new Color(41, 128, 185)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Roll & Fetch
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Student Roll:"), gbc);
        gbc.gridx = 1;
        JPanel rollPanel = new JPanel(new BorderLayout(5, 0));
        txt_roll = new JTextField();
        txt_roll.setFont(new Font("Tahoma", Font.PLAIN, 13));
        JButton btn_fetch = new JButton("Fetch Info");
        btn_fetch.setFont(new Font("Tahoma", Font.PLAIN, 11));
        btn_fetch.addActionListener(e -> fetchStudentInfo());
        rollPanel.add(txt_roll, BorderLayout.CENTER);
        rollPanel.add(btn_fetch, BorderLayout.EAST);
        formPanel.add(rollPanel, gbc);

        // Row 1: Student Name
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Student Name:"), gbc);
        gbc.gridx = 1;
        txt_name = new JTextField();
        formPanel.add(txt_name, gbc);

        // Row 2: Class & Term
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Class / Term:"), gbc);
        gbc.gridx = 1;
        JPanel classTermPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        txt_class = new JTextField();
        combo_term = new JComboBox<>(new String[]{"1st Term", "Mid Term", "Final Exam"});
        classTermPanel.add(txt_class);
        classTermPanel.add(combo_term);
        formPanel.add(classTermPanel, gbc);

        // Row 3: Exam Year
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Exam Year:"), gbc);
        gbc.gridx = 1;
        txt_year = new JTextField(new SimpleDateFormat("yyyy").format(new Date()));
        formPanel.add(txt_year, gbc);

        // Row 4: Separator
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JSeparator sep1 = new JSeparator();
        formPanel.add(sep1, gbc);
        gbc.gridwidth = 1;

        // Subject Scores (Rows 5 to 9)
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("English (100):"), gbc);
        gbc.gridx = 1;
        txt_english = new JTextField();
        formPanel.add(txt_english, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Mathematics (100):"), gbc);
        gbc.gridx = 1;
        txt_math = new JTextField();
        formPanel.add(txt_math, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        formPanel.add(new JLabel("General Science (100):"), gbc);
        gbc.gridx = 1;
        txt_science = new JTextField();
        formPanel.add(txt_science, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        formPanel.add(new JLabel("Social Studies (100):"), gbc);
        gbc.gridx = 1;
        txt_social = new JTextField();
        formPanel.add(txt_social, gbc);

        gbc.gridx = 0; gbc.gridy = 9;
        formPanel.add(new JLabel("ICT (100):"), gbc);
        gbc.gridx = 1;
        txt_ict = new JTextField();
        formPanel.add(txt_ict, gbc);

        // Row 10: Separator
        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2;
        JSeparator sep2 = new JSeparator();
        formPanel.add(sep2, gbc);
        gbc.gridwidth = 1;

        // Calculated Results (Rows 11 to 14)
        gbc.gridx = 0; gbc.gridy = 11;
        formPanel.add(new JLabel("Total Marks / %:"), gbc);
        gbc.gridx = 1;
        JPanel totPercPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        txt_total = new JTextField();
        txt_total.setEditable(false);
        txt_total.setBackground(new Color(240, 240, 240));
        txt_percentage = new JTextField();
        txt_percentage.setEditable(false);
        txt_percentage.setBackground(new Color(240, 240, 240));
        totPercPanel.add(txt_total);
        totPercPanel.add(txt_percentage);
        formPanel.add(totPercPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 12;
        formPanel.add(new JLabel("Grade / GPA (5.0):"), gbc);
        gbc.gridx = 1;
        JPanel gradeGpaPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        txt_grade = new JTextField();
        txt_grade.setEditable(false);
        txt_grade.setFont(new Font("Tahoma", Font.BOLD, 13));
        txt_grade.setBackground(new Color(240, 240, 240));
        txt_gpa = new JTextField();
        txt_gpa.setEditable(false);
        txt_gpa.setFont(new Font("Tahoma", Font.BOLD, 13));
        txt_gpa.setBackground(new Color(240, 240, 240));
        gradeGpaPanel.add(txt_grade);
        gradeGpaPanel.add(txt_gpa);
        formPanel.add(gradeGpaPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 13;
        formPanel.add(new JLabel("Academic Remarks:"), gbc);
        gbc.gridx = 1;
        txt_remarks = new JTextField();
        txt_remarks.setEditable(false);
        txt_remarks.setBackground(new Color(240, 240, 240));
        formPanel.add(txt_remarks, gbc);

        leftPanel.add(new JScrollPane(formPanel), BorderLayout.CENTER);

        // Buttons Bar
        JPanel btnPanel = new JPanel(new GridLayout(2, 4, 6, 6));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JButton btn_calc = new JButton("Calculate");
        btn_calc.setBackground(new Color(41, 128, 185));
        btn_calc.setForeground(Color.WHITE);
        btn_calc.addActionListener(e -> calculateGrade(false));

        JButton btn_save = new JButton("Save / Update");
        btn_save.setBackground(new Color(39, 174, 96));
        btn_save.setForeground(Color.WHITE);
        btn_save.addActionListener(e -> saveRecord());

        JButton btn_delete = new JButton("Delete");
        btn_delete.setBackground(new Color(192, 57, 43));
        btn_delete.setForeground(Color.WHITE);
        btn_delete.addActionListener(e -> deleteRecord());

        JButton btn_clear = new JButton("Clear Form");
        btn_clear.addActionListener(e -> clearForm());

        JButton btn_report = new JButton("Report Card");
        btn_report.setBackground(new Color(142, 68, 173));
        btn_report.setForeground(Color.WHITE);
        btn_report.addActionListener(e -> generateReportCard());

        JButton btn_home = new JButton("Main Menu");
        btn_home.addActionListener(e -> {
            new Students_and_Teachers_Management().setVisible(true);
            dispose();
        });

        JButton btn_close = new JButton("Close");
        btn_close.addActionListener(e -> dispose());

        btnPanel.add(btn_calc);
        btnPanel.add(btn_save);
        btnPanel.add(btn_delete);
        btnPanel.add(btn_clear);
        btnPanel.add(btn_report);
        btnPanel.add(btn_home);
        btnPanel.add(btn_close);

        leftPanel.add(btnPanel, BorderLayout.SOUTH);
        splitPane.setLeftComponent(leftPanel);

        // Right Panel: Table Overview and Search
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Examination Records"));
        txt_search = new JTextField();
        txt_search.setToolTipText("Enter Student Roll or Name");
        JButton btn_search = new JButton("Search");
        btn_search.addActionListener(e -> searchRecords());
        JButton btn_refresh = new JButton("Refresh All");
        btn_refresh.addActionListener(e -> loadTableData());

        searchPanel.add(txt_search, BorderLayout.CENTER);
        JPanel searchBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        searchBtns.add(btn_search);
        searchBtns.add(btn_refresh);
        searchPanel.add(searchBtns, BorderLayout.EAST);
        rightPanel.add(searchPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{
                "Roll", "Name", "Class", "Term", "Year", "Total", "Pct%", "Grade", "GPA", "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table_marks = new JTable(tableModel);
        table_marks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table_marks.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    loadSelectedRowToForm();
                }
            }
        });

        rightPanel.add(new JScrollPane(table_marks), BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void fetchStudentInfo() {
        String rollStr = txt_roll.getText().trim();
        if (rollStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Student Roll number.");
            return;
        }

        try {
            int roll = Integer.parseInt(rollStr);
            String sql = "SELECT First_name, Last_name, Class, Year FROM student_info WHERE Roll = ?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, roll);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        txt_name.setText(rs.getString("First_name") + " " + rs.getString("Last_name"));
                        txt_class.setText(rs.getString("Class"));
                        txt_year.setText(rs.getString("Year"));
                    } else {
                        JOptionPane.showMessageDialog(this, "No student found with Roll: " + roll);
                    }
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Roll must be a valid integer.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching student: " + ex.getMessage());
        }
    }

    private GradeCalculator.GradeResult calculateGrade(boolean silent) {
        try {
            double eng = Double.parseDouble(txt_english.getText().trim());
            double math = Double.parseDouble(txt_math.getText().trim());
            double sci = Double.parseDouble(txt_science.getText().trim());
            double soc = Double.parseDouble(txt_social.getText().trim());
            double ict = Double.parseDouble(txt_ict.getText().trim());

            GradeCalculator.GradeResult res = GradeCalculator.calculate(eng, math, sci, soc, ict);

            txt_total.setText(String.format("%.1f", res.totalMarks));
            txt_percentage.setText(String.format("%.2f%%", res.percentage));
            txt_grade.setText(res.grade);
            txt_gpa.setText(String.format("%.2f", res.gpa));
            txt_remarks.setText(res.remarks);

            if (res.passed) {
                txt_grade.setForeground(new Color(39, 174, 96));
            } else {
                txt_grade.setForeground(new Color(192, 57, 43));
            }

            return res;
        } catch (NumberFormatException ex) {
            if (!silent) {
                JOptionPane.showMessageDialog(this, "Please enter valid numeric marks for all 5 subjects.");
            }
            return null;
        } catch (IllegalArgumentException ex) {
            if (!silent) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
            return null;
        }
    }

    private void saveRecord() {
        if (txt_roll.getText().trim().isEmpty() || txt_name.getText().trim().isEmpty() ||
            txt_class.getText().trim().isEmpty() || txt_year.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in Roll, Name, Class, and Year.");
            return;
        }

        GradeCalculator.GradeResult res = calculateGrade(false);
        if (res == null) return;

        try {
            int roll = Integer.parseInt(txt_roll.getText().trim());
            int cls = Integer.parseInt(txt_class.getText().trim());
            int yr = Integer.parseInt(txt_year.getText().trim());
            String term = (String) combo_term.getSelectedItem();

            ExamMarksDAO.ExamMark mark = new ExamMarksDAO.ExamMark(
                    roll, txt_name.getText().trim(), cls, term, yr,
                    Double.parseDouble(txt_english.getText().trim()),
                    Double.parseDouble(txt_math.getText().trim()),
                    Double.parseDouble(txt_science.getText().trim()),
                    Double.parseDouble(txt_social.getText().trim()),
                    Double.parseDouble(txt_ict.getText().trim()),
                    res
            );

            boolean success = ExamMarksDAO.saveOrUpdate(conn, mark);
            if (success) {
                JOptionPane.showMessageDialog(this, "Exam marks saved successfully!");
                loadTableData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save record.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving record: " + ex.getMessage());
        }
    }

    private void deleteRecord() {
        String rollStr = txt_roll.getText().trim();
        String yrStr = txt_year.getText().trim();
        String term = (String) combo_term.getSelectedItem();

        if (rollStr.isEmpty() || yrStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select or enter Roll and Year to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to delete this exam record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            int roll = Integer.parseInt(rollStr);
            int yr = Integer.parseInt(yrStr);
            boolean deleted = ExamMarksDAO.delete(conn, roll, term, yr);
            if (deleted) {
                JOptionPane.showMessageDialog(this, "Record deleted successfully.");
                clearForm();
                loadTableData();
            } else {
                JOptionPane.showMessageDialog(this, "No matching record found to delete.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error deleting record: " + ex.getMessage());
        }
    }

    private void clearForm() {
        txt_roll.setText("");
        txt_name.setText("");
        txt_class.setText("");
        combo_term.setSelectedIndex(0);
        txt_year.setText(new SimpleDateFormat("yyyy").format(new Date()));

        txt_english.setText("");
        txt_math.setText("");
        txt_science.setText("");
        txt_social.setText("");
        txt_ict.setText("");

        txt_total.setText("");
        txt_percentage.setText("");
        txt_grade.setText("");
        txt_gpa.setText("");
        txt_remarks.setText("");
    }

    private void loadTableData() {
        try {
            List<ExamMarksDAO.ExamMark> list = ExamMarksDAO.findAll(conn);
            tableModel.setRowCount(0);
            for (ExamMarksDAO.ExamMark m : list) {
                tableModel.addRow(new Object[]{
                        m.roll, m.studentName, m.className, m.examTerm, m.examYear,
                        m.totalMarks, String.format("%.1f%%", m.percentage),
                        m.grade, String.format("%.2f", m.gpa), m.remarks
                });
            }
        } catch (Exception ex) {
            System.err.println("Error loading marks table: " + ex.getMessage());
        }
    }

    private void searchRecords() {
        String q = txt_search.getText().trim();
        if (q.isEmpty()) {
            loadTableData();
            return;
        }

        try {
            List<ExamMarksDAO.ExamMark> list = ExamMarksDAO.search(conn, q);
            tableModel.setRowCount(0);
            for (ExamMarksDAO.ExamMark m : list) {
                tableModel.addRow(new Object[]{
                        m.roll, m.studentName, m.className, m.examTerm, m.examYear,
                        m.totalMarks, String.format("%.1f%%", m.percentage),
                        m.grade, String.format("%.2f", m.gpa), m.remarks
                });
            }
            if (list.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No records found matching: " + q);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error during search: " + ex.getMessage());
        }
    }

    private void loadSelectedRowToForm() {
        int selectedRow = table_marks.getSelectedRow();
        if (selectedRow < 0) return;

        try {
            int roll = (int) tableModel.getValueAt(selectedRow, 0);
            String term = (String) tableModel.getValueAt(selectedRow, 3);
            int year = (int) tableModel.getValueAt(selectedRow, 4);

            ExamMarksDAO.ExamMark mark = ExamMarksDAO.findByRollTermYear(conn, roll, term, year);
            if (mark != null) {
                txt_roll.setText(String.valueOf(mark.roll));
                txt_name.setText(mark.studentName);
                txt_class.setText(String.valueOf(mark.className));
                combo_term.setSelectedItem(mark.examTerm);
                txt_year.setText(String.valueOf(mark.examYear));

                txt_english.setText(String.valueOf(mark.english));
                txt_math.setText(String.valueOf(mark.math));
                txt_science.setText(String.valueOf(mark.science));
                txt_social.setText(String.valueOf(mark.socialStudies));
                txt_ict.setText(String.valueOf(mark.ict));

                calculateGrade(true);
            }
        } catch (Exception ex) {
            System.err.println("Error loading selected row: " + ex.getMessage());
        }
    }

    private void generateReportCard() {
        if (txt_roll.getText().trim().isEmpty() || txt_total.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select or calculate marks for a student first.");
            return;
        }

        String report = String.format(
            "========================================================================\n" +
            "                    OFFICIAL ACADEMIC REPORT CARD                       \n" +
            "                     SCHOOL MANAGEMENT SYSTEM                          \n" +
            "========================================================================\n" +
            "Student Name : %-25s Roll Number : %-10s\n" +
            "Class        : %-25s Exam Term   : %-15s\n" +
            "Academic Year: %-25s Date Issued : %-15s\n" +
            "------------------------------------------------------------------------\n" +
            " Subject                     Full Marks       Pass Marks    Marks Obtained\n" +
            "------------------------------------------------------------------------\n" +
            " 1. English                     100               33            %6.1f\n" +
            " 2. Mathematics                 100               33            %6.1f\n" +
            " 3. General Science             100               33            %6.1f\n" +
            " 4. Social Studies              100               33            %6.1f\n" +
            " 5. ICT                         100               33            %6.1f\n" +
            "------------------------------------------------------------------------\n" +
            " Total Marks   : %6.1f / 500.0\n" +
            " Percentage    : %6s\n" +
            " Letter Grade  : %-6s               GPA (5.0 Scale): %s\n" +
            " Result Status : %-30s\n" +
            "========================================================================\n" +
            " Headmaster Signature: __________________   Teacher: __________________\n",
            txt_name.getText(), txt_roll.getText(),
            txt_class.getText(), combo_term.getSelectedItem(),
            txt_year.getText(), new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
            Double.parseDouble(txt_english.getText()),
            Double.parseDouble(txt_math.getText()),
            Double.parseDouble(txt_science.getText()),
            Double.parseDouble(txt_social.getText()),
            Double.parseDouble(txt_ict.getText()),
            Double.parseDouble(txt_total.getText()),
            txt_percentage.getText(),
            txt_grade.getText(), txt_gpa.getText(),
            txt_remarks.getText()
        );

        JTextArea textArea = new JTextArea(report);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(680, 480));

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton printBtn = new JButton("Print Report Card");
        printBtn.addActionListener(e -> {
            try {
                textArea.print();
            } catch (Exception printEx) {
                JOptionPane.showMessageDialog(this, "Printer error: " + printEx.getMessage());
            }
        });
        panel.add(printBtn, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(this, panel, "Student Report Card", JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Student_Grades_System().setVisible(true));
    }
}
