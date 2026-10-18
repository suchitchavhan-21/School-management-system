import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.util.Arrays;
import javax.swing.*;

/**
 * First-Run Administrator Account Setup Dialog.
 * Prompts administrator to configure initial secure credentials when no accounts exist.
 */
public class FirstRunSetupDialog extends JDialog {

    private JTextField txt_username;
    private JPasswordField txt_password;
    private JPasswordField txt_confirm_password;
    private JPasswordField txt_admin_key;
    private JPasswordField txt_confirm_admin_key;

    private boolean setupCompleted = false;
    private final Connection conn;

    public FirstRunSetupDialog(Frame parent, Connection conn) {
        super(parent, "Initial Security Setup - School Management System", true);
        this.conn = conn;
        initComponents();
    }

    private void initComponents() {
        setSize(480, 380);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel titleLabel = new JLabel("First-Run Administrator Setup");
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel subLabel = new JLabel("Create initial secure credentials to initialize the system.");
        subLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        subLabel.setForeground(new Color(236, 240, 241));
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Login Username:"), gbc);
        gbc.gridx = 1;
        txt_username = new JTextField(15);
        txt_username.setText("admin");
        formPanel.add(txt_username, gbc);

        // Login Password
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Login Password:"), gbc);
        gbc.gridx = 1;
        txt_password = new JPasswordField(15);
        formPanel.add(txt_password, gbc);

        // Confirm Login Password
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        txt_confirm_password = new JPasswordField(15);
        formPanel.add(txt_confirm_password, gbc);

        // Admin Security PIN / Master Password
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Admin Delete PIN:"), gbc);
        gbc.gridx = 1;
        txt_admin_key = new JPasswordField(15);
        formPanel.add(txt_admin_key, gbc);

        // Confirm Admin Security PIN
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Confirm Admin PIN:"), gbc);
        gbc.gridx = 1;
        txt_confirm_admin_key = new JPasswordField(15);
        formPanel.add(txt_confirm_admin_key, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btn_submit = new JButton("Save & Initialize");
        btn_submit.setFont(new Font("Tahoma", Font.BOLD, 12));
        btn_submit.setBackground(new Color(39, 174, 96));
        btn_submit.setForeground(Color.WHITE);
        btn_submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSetup();
            }
        });

        JButton btn_cancel = new JButton("Cancel");
        btn_cancel.addActionListener(e -> dispose());

        btnPanel.add(btn_cancel);
        btnPanel.add(btn_submit);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void handleSetup() {
        String username = txt_username.getText().trim();
        char[] pwd = txt_password.getPassword();
        char[] confirmPwd = txt_confirm_password.getPassword();
        char[] adminPin = txt_admin_key.getPassword();
        char[] confirmAdminPin = txt_confirm_admin_key.getPassword();

        try {
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username cannot be blank.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (pwd.length < 4) {
                JOptionPane.showMessageDialog(this, "Password must be at least 4 characters long.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!Arrays.equals(pwd, confirmPwd)) {
                JOptionPane.showMessageDialog(this, "Login passwords do not match.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (adminPin.length < 4) {
                JOptionPane.showMessageDialog(this, "Admin PIN must be at least 4 characters long.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!Arrays.equals(adminPin, confirmAdminPin)) {
                JOptionPane.showMessageDialog(this, "Admin PINs do not match.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Register initial credentials using PBKDF2 salted hash
            PasswordSecurity.registerUser(conn, username, pwd);
            PasswordSecurity.setAdminPassword(conn, adminPin);

            JOptionPane.showMessageDialog(this, "Initial administrator setup completed successfully!\nPlease log in with your new credentials.", "Success", JOptionPane.INFORMATION_MESSAGE);
            setupCompleted = true;
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Setup error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            PasswordSecurity.wipe(pwd);
            PasswordSecurity.wipe(confirmPwd);
            PasswordSecurity.wipe(adminPin);
            PasswordSecurity.wipe(confirmAdminPin);
        }
    }

    public boolean isSetupCompleted() {
        return setupCompleted;
    }
}
