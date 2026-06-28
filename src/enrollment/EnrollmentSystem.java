package enrollment;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

public class EnrollmentSystem {

    // Functional interface for building a menu card with an icon badge
    @FunctionalInterface
    interface IconCardFactory {
        JButton make(String label, String sub, String iconGlyph, Color iconBg, Color iconFg);
    }

    // ── Rounded UI helpers ──────────────────────────────
    // Swing has no built-in rounded corners; these small helpers paint
    // rounded shapes manually using Graphics2D, used for the login logo
    // box, input field borders, and the Sign in button.

    /** A JPanel with a solid rounded-rectangle background. */
    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A rounded border for text fields, with adjustable corner radius and inner padding. */
    static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int radius;
        private final int padH;
        private final int padV;

        RoundedLineBorder(Color color, int radius, int padV, int padH) {
            this.color = color;
            this.radius = radius;
            this.padV = padV;
            this.padH = padH;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(padV, padH, padV, padH);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(padV, padH, padV, padH);
            return insets;
        }
    }

    /** A JButton painted as a solid rounded rectangle, with hover color support. */
    static class RoundedButton extends JButton {
        private final int radius;
        private final Color base;
        private final Color hover;
        private boolean isHovering = false;

        RoundedButton(String text, int radius, Color base, Color hover) {
            super(text);
            this.radius = radius;
            this.base = base;
            this.hover = hover;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    isHovering = true;
                    repaint();
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    isHovering = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isHovering ? hover : base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Wraps a label in a fixed-width panel so it visually left-aligns within
     *  the centered form block, instead of left-aligning against the window edge. */
    static JComponent alignLeftWithinWidth(JLabel label, int width) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BorderLayout());
        wrap.setMaximumSize(new Dimension(width, 16));
        wrap.setPreferredSize(new Dimension(width, 16));
        wrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        wrap.add(label, BorderLayout.WEST);
        return wrap;
    }

    // ─────────────────────────────────────────
    // STYLED TABLE DIALOG (shared by all list & report views)
    // ─────────────────────────────────────────

    /**
     * Shows a professional-looking modal dialog with a navy header bar,
     * a sortable JTable with zebra striping, and a footer note.
     *
     * @param title       Window title (e.g. "All Students")
     * @param subtitle    Small text under the title (e.g. "40 records")
     * @param columns     Column header names
     * @param rows        Row data (each inner array matches columns.length)
     * @param footerText  Optional footer line below the table (null to omit)
     */
    public static void showTableDialog(String title, String subtitle,
                                        String[] columns, Object[][] rows, String footerText) {
        Color navyDark   = new Color(21, 35, 63);
        Color navyMid    = new Color(30, 58, 95);
        Color bgLight    = new Color(244, 246, 248);
        Color cardBorder = new Color(224, 227, 232);
        Color textDark   = new Color(27, 36, 48);
        Color textMuted  = new Color(110, 118, 130);
        Color stripe     = new Color(248, 249, 251);
        Color headerBg   = new Color(237, 240, 243);

        JDialog dialog = new JDialog((Frame) null, title, true);
        dialog.setSize(720, 520);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        // ── Header bar ───────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(navyDark);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        RoundedPanel logoBox = new RoundedPanel(8, navyMid);
        logoBox.setPreferredSize(new Dimension(30, 30));
        logoBox.setMaximumSize(new Dimension(30, 30));
        logoBox.setLayout(new GridBagLayout());
        JLabel logoIcon = new JLabel("\uD83C\uDF93");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        logoBox.add(logoIcon);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        JLabel subtitleLbl = new JLabel(subtitle);
        subtitleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLbl.setForeground(new Color(139, 163, 199));
        titleBlock.add(titleLbl);
        titleBlock.add(Box.createVerticalStrut(1));
        titleBlock.add(subtitleLbl);

        header.add(logoBox);
        header.add(titleBlock);
        header.add(Box.createHorizontalGlue());

        // ── Table ─────────────────────────────────────────
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(textDark);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(222, 235, 245));
        table.setSelectionForeground(textDark);
        table.setFillsViewportHeight(true);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(headerBg);
        table.getTableHeader().setForeground(textMuted);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, cardBorder));
        table.getTableHeader().setReorderingAllowed(false);

        // Zebra striping renderer
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : stripe);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setBackground(bgLight);
        tableWrap.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(cardBorder, 1, true));
        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableWrap.add(tableCard, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(bgLight);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 16, 14, 16));

        if (footerText != null) {
            JLabel footerLbl = new JLabel(footerText);
            footerLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            footerLbl.setForeground(textMuted);
            footer.add(footerLbl, BorderLayout.WEST);
        }

        RoundedButton closeBtn = new RoundedButton("Close", 8,
                new Color(255, 255, 255), new Color(248, 249, 251));
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setForeground(textDark);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(86, 32));
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel closeWrap = new JPanel();
        closeWrap.setOpaque(false);
        closeWrap.add(closeBtn);
        footer.add(closeWrap, BorderLayout.EAST);

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(tableWrap, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.getContentPane().setBackground(bgLight);
        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────

    public static void main(String[] args) {
        DatabaseConnection.initializeDatabase();
        showLogin();
    }

    // ─────────────────────────────────────────
    // LOGIN MODULE
    // ─────────────────────────────────────────

    public static void showLogin() {
        // ── Color palette (matches main menu) ──────────
        Color navyDark    = new Color(21, 35, 63);
        Color navyMid     = new Color(30, 58, 95);
        Color fieldBg      = new Color(30, 51, 84);
        Color fieldBorder  = new Color(45, 74, 115);
        Color mutedBlue    = new Color(139, 163, 199);
        Color tealAccent   = new Color(46, 139, 139);
        Color tealHover    = new Color(38, 120, 120);

        final int FORM_WIDTH = 300; // fixed width used by every form element so they all line up

        JFrame frame = new JFrame("Enrollment System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(380, 480);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel outer = new JPanel();
        outer.setBackground(navyDark);
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(BorderFactory.createEmptyBorder(42, 0, 36, 0));

        // ── Inner form panel: fixed width, everything inside aligns to this ──
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.setMaximumSize(new Dimension(FORM_WIDTH, Integer.MAX_VALUE));
        form.setPreferredSize(new Dimension(FORM_WIDTH, 400));

        // ── Logo box (rounded) ───────────────────────────
        RoundedPanel logoBox = new RoundedPanel(14, navyMid);
        logoBox.setMaximumSize(new Dimension(56, 56));
        logoBox.setPreferredSize(new Dimension(56, 56));
        logoBox.setLayout(new GridBagLayout());
        logoBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel logoIcon = new JLabel("\uD83C\uDF93"); // 🎓
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        logoBox.add(logoIcon);
        form.add(logoBox);

        form.add(Box.createVerticalStrut(16));

        // ── Titles (centered) ─────────────────────────────
        JLabel title = new JLabel("Enrollment System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(title);

        form.add(Box.createVerticalStrut(3));

        JLabel subtitle = new JLabel("Sign in to continue");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(mutedBlue);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(subtitle);

        form.add(Box.createVerticalStrut(28));

        // ── Username field ────────────────────────────────
        JLabel userLabel = new JLabel("USERNAME");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        userLabel.setForeground(mutedBlue);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userLabel.setMaximumSize(new Dimension(FORM_WIDTH, 16));
        form.add(alignLeftWithinWidth(userLabel, FORM_WIDTH));
        form.add(Box.createVerticalStrut(6));

        JTextField userField = new JTextField();
        userField.setMaximumSize(new Dimension(FORM_WIDTH, 36));
        userField.setPreferredSize(new Dimension(FORM_WIDTH, 36));
        userField.setBackground(fieldBg);
        userField.setForeground(Color.WHITE);
        userField.setCaretColor(Color.WHITE);
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userField.setBorder(new RoundedLineBorder(fieldBorder, 8, 8, 10));
        userField.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(userField);

        form.add(Box.createVerticalStrut(16));

        // ── Password field ────────────────────────────────
        JLabel passLabel = new JLabel("PASSWORD");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        passLabel.setForeground(mutedBlue);
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(alignLeftWithinWidth(passLabel, FORM_WIDTH));
        form.add(Box.createVerticalStrut(6));

        JPasswordField passField = new JPasswordField();
        passField.setMaximumSize(new Dimension(FORM_WIDTH, 36));
        passField.setPreferredSize(new Dimension(FORM_WIDTH, 36));
        passField.setBackground(fieldBg);
        passField.setForeground(Color.WHITE);
        passField.setCaretColor(Color.WHITE);
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passField.setBorder(new RoundedLineBorder(fieldBorder, 8, 8, 10));
        passField.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(passField);

        form.add(Box.createVerticalStrut(8));

        // ── Status / error label ──────────────────────────
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(240, 130, 130));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setMaximumSize(new Dimension(FORM_WIDTH, 16));
        form.add(statusLabel);

        form.add(Box.createVerticalStrut(14));

        // ── Sign in button (rounded) ───────────────────────
        RoundedButton signInBtn = new RoundedButton("Sign in", 8, tealAccent, tealHover);
        signInBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        signInBtn.setForeground(Color.WHITE);
        signInBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signInBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        signInBtn.setMaximumSize(new Dimension(FORM_WIDTH, 40));
        signInBtn.setPreferredSize(new Dimension(FORM_WIDTH, 40));
        form.add(signInBtn);

        form.add(Box.createVerticalStrut(16));

        JLabel hint = new JLabel("Default admin account: admin / admin123");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hint.setForeground(new Color(90, 110, 140));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(hint);

        outer.add(form);

        // ── Track failed attempts ──────────────────────────
        final int[] attempts = {0};

        Runnable attemptLogin = () -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please enter both username and password.");
                return;
            }

            if (validateLogin(username, password)) {
                frame.dispose();
                showMainMenu();
            } else {
                attempts[0]++;
                int remaining = 3 - attempts[0];
                if (remaining <= 0) {
                    JOptionPane.showMessageDialog(frame,
                            "Too many failed attempts. Exiting system.",
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                    DatabaseConnection.closeConnection();
                    System.exit(0);
                } else {
                    statusLabel.setText("Invalid credentials. " + remaining + " attempt(s) remaining.");
                    passField.setText("");
                }
            }
        };

        signInBtn.addActionListener(e -> attemptLogin.run());
        passField.addActionListener(e -> attemptLogin.run());

        frame.add(outer);
        frame.setVisible(true);
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
        // ── Color palette ─────────────────────────────
        Color navyDark   = new Color(21, 35, 63);
        Color navyMid    = new Color(30, 58, 95);
        Color bgLight    = new Color(244, 246, 248);
        Color cardBorder = new Color(224, 227, 232);
        Color textDark   = new Color(27, 36, 48);
        Color textMuted  = new Color(110, 118, 130);
        Color dangerBg   = new Color(253, 240, 240);
        Color dangerText = new Color(163, 45, 45);
        Color dangerBorder = new Color(240, 200, 200);

        JFrame frame = new JFrame("Enrollment System");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(460, 610);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // ── Header ─────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(navyDark);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        RoundedPanel logoBox = new RoundedPanel(10, navyMid);
        logoBox.setPreferredSize(new Dimension(36, 36));
        logoBox.setMaximumSize(new Dimension(36, 36));
        logoBox.setLayout(new GridBagLayout());
        JLabel logoIcon = new JLabel("\uD83C\uDF93"); // 🎓
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 17));
        logoBox.add(logoIcon);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        JLabel title = new JLabel("Enrollment System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Signed in as admin");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(new Color(139, 163, 199));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(subtitle);

        header.add(logoBox);
        header.add(titleBlock);
        header.add(Box.createHorizontalGlue());

        // ── Body ────────────────────────────────────────
        JPanel body = new JPanel();
        body.setBackground(bgLight);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));

        // ── Section label helper ───────────────────────
        java.util.function.Function<String, JLabel> sectionLabel = (text) -> {
            JLabel lbl = new JLabel(text.toUpperCase());
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lbl.setForeground(new Color(139, 148, 163));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));
            return lbl;
        };

        // ── Card button helper with icon badge ─────────
        // iconGlyph: a short unicode symbol; iconBg/iconFg: badge colors
        IconCardFactory makeCard = (label, sub, iconGlyph, iconBg, iconFg) -> {
            JButton btn = new JButton();
            btn.setLayout(new BorderLayout(10, 0));
            btn.setBackground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(cardBorder, 1, true),
                    BorderFactory.createEmptyBorder(9, 10, 9, 10)));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setHorizontalAlignment(SwingConstants.LEFT);

            // Icon badge (small rounded square with a glyph)
            RoundedPanel iconBadge = new RoundedPanel(8, iconBg);
            iconBadge.setPreferredSize(new Dimension(28, 28));
            iconBadge.setMaximumSize(new Dimension(28, 28));
            iconBadge.setMinimumSize(new Dimension(28, 28));
            iconBadge.setLayout(new GridBagLayout());
            JLabel iconLbl = new JLabel(iconGlyph);
            iconLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            iconLbl.setForeground(iconFg);
            iconBadge.add(iconLbl);

            JLabel labelLbl = new JLabel(label);
            labelLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            labelLbl.setForeground(textDark);

            JLabel subLbl = new JLabel(sub);
            subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            subLbl.setForeground(textMuted);

            JPanel textStack = new JPanel();
            textStack.setOpaque(false);
            textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
            labelLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            textStack.add(labelLbl);
            textStack.add(Box.createVerticalStrut(2));
            textStack.add(subLbl);

            JPanel iconRow = new JPanel();
            iconRow.setOpaque(false);
            iconRow.setLayout(new BoxLayout(iconRow, BoxLayout.Y_AXIS));
            iconBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
            iconRow.add(iconBadge);
            iconRow.add(Box.createVerticalStrut(6));
            textStack.setAlignmentX(Component.LEFT_ALIGNMENT);
            iconRow.add(textStack);

            btn.add(iconRow, BorderLayout.CENTER);

            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    btn.setBackground(new Color(248, 249, 251));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    btn.setBackground(Color.WHITE);
                }
            });

            return btn;
        };

        // ── Students section: 3-column grid, no empty cell ────
        body.add(sectionLabel.apply("Students"));
        JPanel grid1 = new JPanel(new GridLayout(2, 3, 7, 7));
        grid1.setOpaque(false);
        grid1.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 156));

        JButton btnAdd    = makeCard.make("Add", "New student", "+",
                new Color(230, 241, 251), new Color(24, 95, 165));
        JButton btnView   = makeCard.make("View all", "All records", "\u2261",
                new Color(234, 243, 222), new Color(59, 109, 17));
        JButton btnSearch = makeCard.make("Search", "Find student", "\u26B2",
                new Color(243, 239, 254), new Color(83, 74, 183));
        JButton btnUpdate = makeCard.make("Update", "Edit record", "\u270E",
                new Color(250, 238, 218), new Color(133, 79, 11));
        JButton btnDelete = makeCard.make("Delete", "Remove record", "\u2715",
                new Color(252, 235, 235), new Color(163, 45, 45));
        grid1.add(btnAdd);
        grid1.add(btnView);
        grid1.add(btnSearch);
        grid1.add(btnUpdate);
        grid1.add(btnDelete);
        // 6th cell: filled with a disabled-look placeholder is unnecessary —
        // instead, stretch Delete's row by leaving grid at 5 items;
        // GridLayout requires equal cells, so we add a spacer label instead.
        JLabel spacerCell = new JLabel();
        grid1.add(spacerCell);
        body.add(grid1);

        body.add(Box.createVerticalStrut(14));

        // ── Subjects & Enrollment section: 2-column grid ──────
        body.add(sectionLabel.apply("Subjects & enrollment"));
        JPanel grid2 = new JPanel(new GridLayout(2, 2, 7, 7));
        grid2.setOpaque(false);
        grid2.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));

        JButton btnSubjects   = makeCard.make("Subjects", "Manage subjects", "\u2317",
                new Color(225, 245, 238), new Color(15, 110, 86));
        JButton btnEnroll     = makeCard.make("Enroll", "Assign subject", "\u2713",
                new Color(251, 234, 240), new Color(153, 53, 86));
        JButton btnEnrollView = makeCard.make("Enrollments", "View all records", "\u2630",
                new Color(230, 241, 251), new Color(24, 95, 165));
        JButton btnReport     = makeCard.make("Reports", "Generate summary", "\u25A4",
                new Color(234, 243, 222), new Color(59, 109, 17));
        grid2.add(btnSubjects);
        grid2.add(btnEnroll);
        grid2.add(btnEnrollView);
        grid2.add(btnReport);
        body.add(grid2);

        body.add(Box.createVerticalStrut(14));

        // ── Exit button ─────────────────────────────────
        JButton btnExit = new JButton("Exit system");
        btnExit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnExit.setBackground(dangerBg);
        btnExit.setForeground(dangerText);
        btnExit.setFocusPainted(false);
        btnExit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(dangerBorder, 1, true),
                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        btnExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExit.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnExit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        body.add(btnExit);

        body.add(Box.createVerticalStrut(10));

        // ── Status bar (pinned with a top divider, fills remaining space) ──
        body.add(Box.createVerticalGlue());

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, cardBorder),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));

        JLabel statusLeft = new JLabel("●  Connected to enrollment.db");
        statusLeft.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statusLeft.setForeground(new Color(60, 150, 110));

        JLabel statusRight = new JLabel("v1.0");
        statusRight.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statusRight.setForeground(textMuted);

        statusBar.add(statusLeft, BorderLayout.WEST);
        statusBar.add(statusRight, BorderLayout.EAST);
        body.add(statusBar);

        // ── Wire up actions (unchanged) ─────────────────
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

        // ── Assemble ─────────────────────────────────────
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

            String[] columns = {"Student ID", "Name", "Course", "Year", "Contact"};
            Object[][] data = new Object[students.size()][5];
            for (int i = 0; i < students.size(); i++) {
                RegularStudent s = students.get(i);
                data[i][0] = s.getStudentId();
                data[i][1] = s.getName();
                data[i][2] = s.getCourse();
                data[i][3] = s.getYearLevel();
                data[i][4] = s.getContact();
            }

            showTableDialog("All students", students.size() + " records", columns, data, null);

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

            String[] columns = {"Student ID", "Name", "Course", "Year", "Contact"};
            Object[][] data = new Object[results.size()][5];
            for (int i = 0; i < results.size(); i++) {
                RegularStudent s = results.get(i);
                data[i][0] = s.getStudentId();
                data[i][1] = s.getName();
                data[i][2] = s.getCourse();
                data[i][3] = s.getYearLevel();
                data[i][4] = s.getContact();
            }

            showTableDialog("Search results", results.size() + " match(es) for \"" + keyword + "\"",
                    columns, data, null);

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

            String[] columns = {"Code", "Subject name", "Units", "Schedule"};
            Object[][] data = new Object[subjects.size()][4];
            for (int i = 0; i < subjects.size(); i++) {
                Subject s = subjects.get(i);
                data[i][0] = s.getSubjectCode();
                data[i][1] = s.getSubjectName();
                data[i][2] = s.getUnits();
                data[i][3] = s.getSchedule();
            }

            showTableDialog("All subjects", subjects.size() + " records", columns, data, null);

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

            ArrayList<Object[]> rowList = new ArrayList<>();
            while (rs.next()) {
                rowList.add(new Object[]{
                        rs.getString("student_id"),
                        rs.getString("name"),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("date_enrolled")
                });
            }

            if (rowList.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No enrollment records found.");
                return;
            }

            String[] columns = {"Student ID", "Name", "Code", "Subject", "Date"};
            Object[][] data = rowList.toArray(new Object[0][]);

            showTableDialog("All enrollments", rowList.size() + " records", columns, data, null);

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

            ArrayList<Object[]> rowList = new ArrayList<>();
            int total = 0;
            while (rs.next()) {
                int count = rs.getInt("total");
                rowList.add(new Object[]{
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        count
                });
                total += count;
            }

            String[] columns = {"Code", "Subject name", "Enrolled"};
            Object[][] data = rowList.toArray(new Object[0][]);

            showTableDialog("Enrollment summary by subject", rowList.size() + " subjects",
                    columns, data, "Total enrollments: " + total);

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

            ArrayList<Object[]> rowList = new ArrayList<>();
            int grandTotal = 0;
            while (rs.next()) {
                int count = rs.getInt("total");
                rowList.add(new Object[]{
                        rs.getString("course"),
                        count
                });
                grandTotal += count;
            }

            if (rowList.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No student records found.");
                return;
            }

            String[] columns = {"Course", "Total students"};
            Object[][] data = rowList.toArray(new Object[0][]);

            showTableDialog("Students per course", rowList.size() + " courses",
                    columns, data, "Grand total: " + grandTotal + " students");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error generating report: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}