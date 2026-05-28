package enrollment;

import java.awt.*;
import javax.swing.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.swing.JOptionPane;

public class EnrollmentSystem {

    // ─────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────

    public static void main(String[] args) {
        DatabaseConnection.initializeDatabase();

        if (showLogin()) {
            showMainMenu();
        } else {
            JOptionPane.showMessageDialog(null, "Too many failed attempts. Exiting system.");
        }

        DatabaseConnection.closeConnection();
    }

    // ─────────────────────────────────────────
    // LOGIN MODULE
    // ─────────────────────────────────────────

    public static boolean showLogin() {
        int attempts = 0;

        while (attempts < 3) {
            String username = JOptionPane.showInputDialog(null,
                    "Username:", "Enrollment System - Login",
                    JOptionPane.PLAIN_MESSAGE);

            if (username == null) {
                System.exit(0); // User closed the dialog
            }

            String password = JOptionPane.showInputDialog(null,
                    "Password:", "Enrollment System - Login",
                    JOptionPane.PLAIN_MESSAGE);

            if (password == null) {
                System.exit(0);
            }

            if (validateLogin(username.trim(), password.trim())) {
                JOptionPane.showMessageDialog(null,
                        "Welcome, " + username + "!",
                        "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                attempts++;
                JOptionPane.showMessageDialog(null,
                        "Invalid username or password.\nAttempts remaining: " + (3 - attempts),
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    public static boolean validateLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Login error: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────
    // MAIN MENU MODULE
    // ─────────────────────────────────────────

    public static void showMainMenu() {
        JFrame frame = new JFrame("Enrollment System");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(420, 580);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // ── Header panel ──────────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(26, 58, 92));

        JLabel icon = new JLabel("🎓");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Enrollment System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Logged in as admin");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(new Color(180, 200, 220));
        titleBlock.add(title);
        titleBlock.add(subtitle);

        header.add(icon);
        header.add(titleBlock);

        // ── Body panel ────────────────────────────────
        JPanel body = new JPanel();
        body.setBackground(new Color(245, 246, 247));
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // ── Helper: section label ─────────────────────
        java.util.function.Supplier<JLabel> makeSection = null; // declared below

        // ── Helper: menu button ───────────────────────
        java.util.function.BiFunction<String, String, JButton> makeBtn = (label, sub) -> {
            JButton btn = new JButton("<html><b>" + label + "</b><br>"
                    + "<span style='color:#777;font-size:10px'>" + sub + "</span></html>");
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            btn.setBackground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            return btn;
        };

        // ── Student Management section ────────────────
        JLabel sec1 = new JLabel("STUDENT MANAGEMENT");
        sec1.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sec1.setForeground(new Color(140, 140, 140));
        sec1.setAlignmentX(Component.LEFT_ALIGNMENT);
        sec1.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        body.add(sec1);

        JPanel grid1 = new JPanel(new GridLayout(3, 2, 8, 8));
        grid1.setOpaque(false);
        grid1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        JButton btnAdd    = makeBtn.apply("Add student",    "Register new");
        JButton btnView   = makeBtn.apply("View students",  "All records");
        JButton btnSearch = makeBtn.apply("Search",         "Find a student");
        JButton btnUpdate = makeBtn.apply("Update",         "Edit record");
        JButton btnDelete = makeBtn.apply("Delete student", "Remove record");
        grid1.add(btnAdd);
        grid1.add(btnView);
        grid1.add(btnSearch);
        grid1.add(btnUpdate);
        grid1.add(btnDelete);
        grid1.add(new JLabel()); // empty cell
        body.add(grid1);

        body.add(Box.createVerticalStrut(14));

        // ── Subjects & Enrollment section ─────────────
        JLabel sec2 = new JLabel("SUBJECTS & ENROLLMENT");
        sec2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sec2.setForeground(new Color(140, 140, 140));
        sec2.setAlignmentX(Component.LEFT_ALIGNMENT);
        sec2.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        body.add(sec2);

        JPanel grid2 = new JPanel(new GridLayout(2, 2, 8, 8));
        grid2.setOpaque(false);
        grid2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        JButton btnSubjects   = makeBtn.apply("Subjects",    "Manage subjects");
        JButton btnEnroll     = makeBtn.apply("Enroll",      "Assign subject");
        JButton btnEnrollView = makeBtn.apply("Enrollments", "View all");
        JButton btnReport     = makeBtn.apply("Reports",     "Generate summary");
        grid2.add(btnSubjects);
        grid2.add(btnEnroll);
        grid2.add(btnEnrollView);
        grid2.add(btnReport);
        body.add(grid2);

        body.add(Box.createVerticalStrut(14));

        // ── Exit button ────────────────────────────────
        JButton btnExit = new JButton("Exit system");
        btnExit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnExit.setBackground(new Color(255, 240, 240));
        btnExit.setForeground(new Color(163, 45, 45));
        btnExit.setFocusPainted(false);
        btnExit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(240, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        btnExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExit.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnExit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        body.add(btnExit);

        body.add(Box.createVerticalStrut(10));

        // ── Status bar ─────────────────────────────────
        JLabel status = new JLabel("● Connected to enrollment.db  |  Enrollment System v1.0");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        status.setForeground(new Color(160, 160, 160));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(status);

        // ── Wire up button actions ─────────────────────
        btnAdd.addActionListener(e -> addStudent());
        btnView.addActionListener(e -> viewStudents());
        btnSearch.addActionListener(e -> searchStudent());
        btnUpdate.addActionListener(e -> updateStudent());
        btnDelete.addActionListener(e -> deleteStudent());
        btnSubjects.addActionListener(e -> manageSubjects());
        btnEnroll.addActionListener(e -> enrollStudentToSubject());
        btnEnrollView.addActionListener(e -> viewEnrollments());
        btnReport.addActionListener(e -> generateReport());
        btnExit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to exit?",
                    "Exit", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                frame.dispose();
            }
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to exit?",
                        "Exit", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    frame.dispose();
                }
            }
        });

        // ── Assemble and show ──────────────────────────
        frame.setLayout(new BorderLayout());
        frame.add(header, BorderLayout.NORTH);
        frame.add(body, BorderLayout.CENTER);
        frame.setVisible(true);
    }
    // ─────────────────────────────────────────
    // ADD STUDENT MODULE
    // ─────────────────────────────────────────

    public static void addStudent() {
        try {
            String studentId = JOptionPane.showInputDialog(null,
                    "Enter Student ID (e.g. 2024-0001):", "Add Student",
                    JOptionPane.PLAIN_MESSAGE);
            if (studentId == null || studentId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Student ID cannot be empty.");
                return;
            }

            String name = JOptionPane.showInputDialog(null,
                    "Enter Full Name:", "Add Student", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Name cannot be empty.");
                return;
            }

            String course = JOptionPane.showInputDialog(null,
                    "Enter Course (e.g. BSIT, BSCS):", "Add Student", JOptionPane.PLAIN_MESSAGE);
            if (course == null || course.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Course cannot be empty.");
                return;
            }

            String yearStr = JOptionPane.showInputDialog(null,
                    "Enter Year Level (1-4):", "Add Student", JOptionPane.PLAIN_MESSAGE);
            if (yearStr == null || yearStr.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Year level cannot be empty.");
                return;
            }

            int yearLevel;
            try {
                yearLevel = Integer.parseInt(yearStr.trim());
                if (yearLevel < 1 || yearLevel > 4) {
                    JOptionPane.showMessageDialog(null, "Year level must be between 1 and 4.");
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid year level. Please enter a number.");
                return;
            }

            String contact = JOptionPane.showInputDialog(null,
                    "Enter Contact Number:", "Add Student", JOptionPane.PLAIN_MESSAGE);
            if (contact == null) contact = "";

            // Use RegularStudent (Inheritance + Polymorphism)
            RegularStudent student = new RegularStudent(
                    studentId.trim(), name.trim(), course.trim(), yearLevel, contact.trim());

            String sql = "INSERT INTO students (student_id, name, course, year_level, contact) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql)) {
                pst.setString(1, student.getStudentId());
                pst.setString(2, student.getName());
                pst.setString(3, student.getCourse());
                pst.setInt(4, student.getYearLevel());
                pst.setString(5, student.getContact());
                pst.executeUpdate();
                JOptionPane.showMessageDialog(null,
                        "Student added successfully!\n" + student.getDisplayInfo(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(null,
                        "Student ID already exists. Please use a different ID.",
                        "Duplicate Entry", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Error adding student: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ─────────────────────────────────────────
    // VIEW STUDENTS MODULE
    // ─────────────────────────────────────────

    public static void viewStudents() {
        String sql = "SELECT * FROM students ORDER BY student_id";
        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ArrayList<RegularStudent> students = new ArrayList<>();
            while (rs.next()) {
                students.add(new RegularStudent(
                        rs.getString("student_id"),
                        rs.getString("name"),
                        rs.getString("course"),
                        rs.getInt("year_level"),
                        rs.getString("contact")));
            }

            if (students.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No students found in the database.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-15s %-25s %-10s %-6s %-15s%n",
                    "Student ID", "Name", "Course", "Year", "Contact"));
            sb.append("─".repeat(75)).append("\n");

            for (RegularStudent s : students) {
                sb.append(String.format("%-15s %-25s %-10s %-6d %-15s%n",
                        s.getStudentId(), s.getName(), s.getCourse(),
                        s.getYearLevel(), s.getContact()));
            }

            JOptionPane.showMessageDialog(null, sb.toString(),
                    "All Students (" + students.size() + " records)",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving students: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────
    // SEARCH STUDENT MODULE
    // ─────────────────────────────────────────

    public static void searchStudent() {
        String keyword = JOptionPane.showInputDialog(null,
                "Enter Student ID or Name to search:", "Search Student",
                JOptionPane.PLAIN_MESSAGE);

        if (keyword == null || keyword.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Search keyword cannot be empty.");
            return;
        }

        String sql = "SELECT * FROM students WHERE student_id LIKE ? OR name LIKE ?";
        try (PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pst.setString(1, "%" + keyword.trim() + "%");
            pst.setString(2, "%" + keyword.trim() + "%");
            ResultSet rs = pst.executeQuery();

            ArrayList<RegularStudent> results = new ArrayList<>();
            while (rs.next()) {
                results.add(new RegularStudent(
                        rs.getString("student_id"),
                        rs.getString("name"),
                        rs.getString("course"),
                        rs.getInt("year_level"),
                        rs.getString("contact")));
            }

            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "No student found matching: \"" + keyword + "\"",
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Search results for: \"").append(keyword).append("\"\n");
            sb.append("─".repeat(75)).append("\n");
            for (RegularStudent s : results) {
                sb.append(s.getDisplayInfo()).append("\n");
            }

            JOptionPane.showMessageDialog(null, sb.toString(),
                    "Search Results (" + results.size() + " found)",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error searching: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────
    // UPDATE STUDENT MODULE
    // ─────────────────────────────────────────

    public static void updateStudent() {
        String studentId = JOptionPane.showInputDialog(null,
                "Enter Student ID to update:", "Update Student",
                JOptionPane.PLAIN_MESSAGE);

        if (studentId == null || studentId.trim().isEmpty()) return;

        String checkSql = "SELECT * FROM students WHERE student_id = ?";
        try (PreparedStatement checkPst = DatabaseConnection.getConnection().prepareStatement(checkSql)) {
            checkPst.setString(1, studentId.trim());
            ResultSet rs = checkPst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null,
                        "Student ID \"" + studentId + "\" not found.",
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Show current values
            String currentName = rs.getString("name");
            String currentCourse = rs.getString("course");
            int currentYear = rs.getInt("year_level");
            String currentContact = rs.getString("contact");

            JOptionPane.showMessageDialog(null,
                    "Current record:\n" +
                    "Name: " + currentName + "\n" +
                    "Course: " + currentCourse + "\n" +
                    "Year: " + currentYear + "\n" +
                    "Contact: " + currentContact + "\n\n" +
                    "Press OK to continue updating.",
                    "Current Record", JOptionPane.INFORMATION_MESSAGE);

            String newName = JOptionPane.showInputDialog(null,
                    "Enter new Name (current: " + currentName + "):",
                    "Update Student", JOptionPane.PLAIN_MESSAGE);
            if (newName == null || newName.trim().isEmpty()) newName = currentName;

            String newCourse = JOptionPane.showInputDialog(null,
                    "Enter new Course (current: " + currentCourse + "):",
                    "Update Student", JOptionPane.PLAIN_MESSAGE);
            if (newCourse == null || newCourse.trim().isEmpty()) newCourse = currentCourse;

            String yearStr = JOptionPane.showInputDialog(null,
                    "Enter new Year Level (current: " + currentYear + "):",
                    "Update Student", JOptionPane.PLAIN_MESSAGE);
            int newYear = currentYear;
            if (yearStr != null && !yearStr.trim().isEmpty()) {
                try {
                    newYear = Integer.parseInt(yearStr.trim());
                    if (newYear < 1 || newYear > 4) {
                        JOptionPane.showMessageDialog(null, "Invalid year. Keeping current value.");
                        newYear = currentYear;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Invalid input. Keeping current year.");
                }
            }

            String newContact = JOptionPane.showInputDialog(null,
                    "Enter new Contact (current: " + currentContact + "):",
                    "Update Student", JOptionPane.PLAIN_MESSAGE);
            if (newContact == null || newContact.trim().isEmpty()) newContact = currentContact;

            String updateSql = "UPDATE students SET name=?, course=?, year_level=?, contact=? WHERE student_id=?";
            try (PreparedStatement updatePst = DatabaseConnection.getConnection().prepareStatement(updateSql)) {
                updatePst.setString(1, newName.trim());
                updatePst.setString(2, newCourse.trim());
                updatePst.setInt(3, newYear);
                updatePst.setString(4, newContact.trim());
                updatePst.setString(5, studentId.trim());
                updatePst.executeUpdate();
                JOptionPane.showMessageDialog(null,
                        "Student record updated successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error updating student: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────
    // DELETE STUDENT MODULE
    // ─────────────────────────────────────────

    public static void deleteStudent() {
        String studentId = JOptionPane.showInputDialog(null,
                "Enter Student ID to delete:", "Delete Student",
                JOptionPane.PLAIN_MESSAGE);

        if (studentId == null || studentId.trim().isEmpty()) return;

        String checkSql = "SELECT * FROM students WHERE student_id = ?";
        try (PreparedStatement checkPst = DatabaseConnection.getConnection().prepareStatement(checkSql)) {
            checkPst.setString(1, studentId.trim());
            ResultSet rs = checkPst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null,
                        "Student ID \"" + studentId + "\" not found.",
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String name = rs.getString("name");
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to delete:\n" +
                    "ID: " + studentId + "\nName: " + name + "\n\n" +
                    "This will also remove all enrollment records for this student.",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) return;

            // Delete enrollments first (foreign key)
            String deleteEnrollments = "DELETE FROM enrollments WHERE student_id = ?";
            try (PreparedStatement delEnroll = DatabaseConnection.getConnection().prepareStatement(deleteEnrollments)) {
                delEnroll.setString(1, studentId.trim());
                delEnroll.executeUpdate();
            }

            String deleteSql = "DELETE FROM students WHERE student_id = ?";
            try (PreparedStatement deletePst = DatabaseConnection.getConnection().prepareStatement(deleteSql)) {
                deletePst.setString(1, studentId.trim());
                deletePst.executeUpdate();
                JOptionPane.showMessageDialog(null,
                        "Student \"" + name + "\" deleted successfully.",
                        "Deleted", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error deleting student: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────
    // MANAGE SUBJECTS MODULE
    // ─────────────────────────────────────────

    public static void manageSubjects() {
        String[] options = {"Add Subject", "View All Subjects", "Delete Subject", "Back"};
        int choice = JOptionPane.showOptionDialog(null,
                "Subject Management:", "Manage Subjects",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        switch (choice) {
            case 0 -> addSubject();
            case 1 -> viewSubjects();
            case 2 -> deleteSubject();
            default -> {}
        }
    }

    public static void addSubject() {
        try {
            String code = JOptionPane.showInputDialog(null,
                    "Enter Subject Code (e.g. CS101):", "Add Subject", JOptionPane.PLAIN_MESSAGE);
            if (code == null || code.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Subject code cannot be empty.");
                return;
            }

            String subjectName = JOptionPane.showInputDialog(null,
                    "Enter Subject Name:", "Add Subject", JOptionPane.PLAIN_MESSAGE);
            if (subjectName == null || subjectName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Subject name cannot be empty.");
                return;
            }

            String unitsStr = JOptionPane.showInputDialog(null,
                    "Enter Units:", "Add Subject", JOptionPane.PLAIN_MESSAGE);
            if (unitsStr == null || unitsStr.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Units cannot be empty.");
                return;
            }

            int units;
            try {
                units = Integer.parseInt(unitsStr.trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid units. Please enter a number.");
                return;
            }

            String schedule = JOptionPane.showInputDialog(null,
                    "Enter Schedule (e.g. MWF 8:00-9:00 AM):", "Add Subject", JOptionPane.PLAIN_MESSAGE);
            if (schedule == null) schedule = "";

            Subject subject = new Subject(0, code.trim(), subjectName.trim(), units, schedule.trim());

            String sql = "INSERT INTO subjects (subject_code, subject_name, units, schedule) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql)) {
                pst.setString(1, subject.getSubjectCode());
                pst.setString(2, subject.getSubjectName());
                pst.setInt(3, subject.getUnits());
                pst.setString(4, subject.getSchedule());
                pst.executeUpdate();
                JOptionPane.showMessageDialog(null,
                        "Subject added successfully!\n" + subject.toString(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(null, "Subject code already exists.",
                        "Duplicate Entry", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Error adding subject: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void viewSubjects() {
        String sql = "SELECT * FROM subjects ORDER BY subject_code";
        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ArrayList<Subject> subjects = new ArrayList<>();
            while (rs.next()) {
                subjects.add(new Subject(
                        rs.getInt("id"),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getInt("units"),
                        rs.getString("schedule")));
            }

            if (subjects.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No subjects found.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-10s %-30s %-6s %-20s%n",
                    "Code", "Subject Name", "Units", "Schedule"));
            sb.append("─".repeat(70)).append("\n");
            for (Subject s : subjects) {
                sb.append(String.format("%-10s %-30s %-6d %-20s%n",
                        s.getSubjectCode(), s.getSubjectName(),
                        s.getUnits(), s.getSchedule()));
            }

            JOptionPane.showMessageDialog(null, sb.toString(),
                    "All Subjects (" + subjects.size() + " records)",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving subjects: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void deleteSubject() {
        String code = JOptionPane.showInputDialog(null,
                "Enter Subject Code to delete:", "Delete Subject", JOptionPane.PLAIN_MESSAGE);
        if (code == null || code.trim().isEmpty()) return;

        String checkSql = "SELECT * FROM subjects WHERE subject_code = ?";
        try (PreparedStatement checkPst = DatabaseConnection.getConnection().prepareStatement(checkSql)) {
            checkPst.setString(1, code.trim());
            ResultSet rs = checkPst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Subject code \"" + code + "\" not found.",
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String subjectName = rs.getString("subject_name");
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Delete subject: " + code + " - " + subjectName + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            // Remove enrollments for this subject first
            String delEnroll = "DELETE FROM enrollments WHERE subject_code = ?";
            try (PreparedStatement delPst = DatabaseConnection.getConnection().prepareStatement(delEnroll)) {
                delPst.setString(1, code.trim());
                delPst.executeUpdate();
            }

            String deleteSql = "DELETE FROM subjects WHERE subject_code = ?";
            try (PreparedStatement deletePst = DatabaseConnection.getConnection().prepareStatement(deleteSql)) {
                deletePst.setString(1, code.trim());
                deletePst.executeUpdate();
                JOptionPane.showMessageDialog(null, "Subject deleted successfully.",
                        "Deleted", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error deleting subject: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────
    // ENROLL STUDENT TO SUBJECT MODULE
    // ─────────────────────────────────────────

    public static void enrollStudentToSubject() {
        String studentId = JOptionPane.showInputDialog(null,
                "Enter Student ID to enroll:", "Enroll Student",
                JOptionPane.PLAIN_MESSAGE);
        if (studentId == null || studentId.trim().isEmpty()) return;

        // Verify student exists
        String checkStudent = "SELECT name FROM students WHERE student_id = ?";
        try (PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(checkStudent)) {
            pst.setString(1, studentId.trim());
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Student ID not found.",
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String studentName = rs.getString("name");

            String subjectCode = JOptionPane.showInputDialog(null,
                    "Enrolling: " + studentName + "\nEnter Subject Code:",
                    "Enroll Student", JOptionPane.PLAIN_MESSAGE);
            if (subjectCode == null || subjectCode.trim().isEmpty()) return;

            // Verify subject exists
            String checkSubject = "SELECT subject_name FROM subjects WHERE subject_code = ?";
            try (PreparedStatement pst2 = DatabaseConnection.getConnection().prepareStatement(checkSubject)) {
                pst2.setString(1, subjectCode.trim());
                ResultSet rs2 = pst2.executeQuery();
                if (!rs2.next()) {
                    JOptionPane.showMessageDialog(null, "Subject code not found.",
                            "Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Check for duplicate enrollment
                String checkDupe = "SELECT id FROM enrollments WHERE student_id = ? AND subject_code = ?";
                try (PreparedStatement pst3 = DatabaseConnection.getConnection().prepareStatement(checkDupe)) {
                    pst3.setString(1, studentId.trim());
                    pst3.setString(2, subjectCode.trim());
                    ResultSet rs3 = pst3.executeQuery();
                    if (rs3.next()) {
                        JOptionPane.showMessageDialog(null,
                                "Student is already enrolled in this subject.",
                                "Duplicate", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                String date = LocalDate.now().toString();
                Enrollment enrollment = new Enrollment(0, studentId.trim(), subjectCode.trim(), date);

                String insertSql = "INSERT INTO enrollments (student_id, subject_code, date_enrolled) VALUES (?, ?, ?)";
                try (PreparedStatement insertPst = DatabaseConnection.getConnection().prepareStatement(insertSql)) {
                    insertPst.setString(1, enrollment.getStudentId());
                    insertPst.setString(2, enrollment.getSubjectCode());
                    insertPst.setString(3, enrollment.getDateEnrolled());
                    insertPst.executeUpdate();
                    JOptionPane.showMessageDialog(null,
                            "Enrollment successful!\n" + enrollment.toString(),
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error enrolling student: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────
    // VIEW ENROLLMENTS MODULE
    // ─────────────────────────────────────────

    public static void viewEnrollments() {
        String sql = """
                SELECT e.student_id, s.name, e.subject_code, sub.subject_name, e.date_enrolled
                FROM enrollments e
                JOIN students s ON e.student_id = s.student_id
                JOIN subjects sub ON e.subject_code = sub.subject_code
                ORDER BY e.student_id
                """;

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-15s %-20s %-10s %-25s %-12s%n",
                    "Student ID", "Name", "Code", "Subject", "Date"));
            sb.append("─".repeat(85)).append("\n");

            int count = 0;
            while (rs.next()) {
                sb.append(String.format("%-15s %-20s %-10s %-25s %-12s%n",
                        rs.getString("student_id"),
                        rs.getString("name"),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("date_enrolled")));
                count++;
            }

            if (count == 0) {
                JOptionPane.showMessageDialog(null, "No enrollment records found.");
                return;
            }

            JOptionPane.showMessageDialog(null, sb.toString(),
                    "All Enrollments (" + count + " records)",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving enrollments: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────
    // REPORT GENERATION MODULE
    // ─────────────────────────────────────────

    public static void generateReport() {
        String[] options = {"Enrollment Summary by Subject", "Students per Course", "Back"};
        int choice = JOptionPane.showOptionDialog(null,
                "Select Report Type:", "Report Generation",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        switch (choice) {
            case 0 -> reportEnrollmentBySubject();
            case 1 -> reportStudentsByCourse();
            default -> {}
        }
    }

    public static void reportEnrollmentBySubject() {
        String sql = """
                SELECT sub.subject_code, sub.subject_name, COUNT(e.id) as total
                FROM subjects sub
                LEFT JOIN enrollments e ON sub.subject_code = e.subject_code
                GROUP BY sub.subject_code, sub.subject_name
                ORDER BY total DESC
                """;

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            StringBuilder sb = new StringBuilder();
            sb.append("=== ENROLLMENT SUMMARY BY SUBJECT ===\n");
            sb.append(String.format("%-12s %-30s %-10s%n", "Code", "Subject Name", "Enrolled"));
            sb.append("─".repeat(55)).append("\n");

            int total = 0;
            while (rs.next()) {
                int count = rs.getInt("total");
                sb.append(String.format("%-12s %-30s %-10d%n",
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        count));
                total += count;
            }

            sb.append("─".repeat(55)).append("\n");
            sb.append("Total Enrollments: ").append(total);

            JOptionPane.showMessageDialog(null, sb.toString(),
                    "Report: Enrollment by Subject", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error generating report: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void reportStudentsByCourse() {
        String sql = """
                SELECT course, COUNT(*) as total
                FROM students
                GROUP BY course
                ORDER BY total DESC
                """;

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            StringBuilder sb = new StringBuilder();
            sb.append("=== STUDENTS PER COURSE ===\n");
            sb.append(String.format("%-15s %-10s%n", "Course", "Total Students"));
            sb.append("─".repeat(28)).append("\n");

            int grandTotal = 0;
            while (rs.next()) {
                int count = rs.getInt("total");
                sb.append(String.format("%-15s %-10d%n",
                        rs.getString("course"), count));
                grandTotal += count;
            }

            sb.append("─".repeat(28)).append("\n");
            sb.append("Grand Total: ").append(grandTotal).append(" students");

            JOptionPane.showMessageDialog(null, sb.toString(),
                    "Report: Students per Course", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error generating report: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}