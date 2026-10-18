import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.util.Arrays;
import javax.swing.*;

/**
 * First-Run Administrator Account Setup Dialog.
 * Prompts administrator to configure initial secure credentials when no accounts exist.
 * Enforces strong password and master key policies, wipes sensitive memory on all exit paths,
 * and delegates to transactional credential initialization.
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
        setSize(520, 420);
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

        JLabel subLabel = new JLabel("Initialize master administrator and authorization credentials. Strong credentials are required.");
        subLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        subLabel.setForeground(new Color(236, 240, 241));
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Administrator Username:"), gbc);
        gbc.gridx = 1;
        txt_username = new JTextField(16);
        txt_username.setText("admin");
        formPanel.add(txt_username, gbc);

        // Login Password
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Login Password (min 8 chars):"), gbc);
        gbc.gridx = 1;
        txt_password = new JPasswordField(16);
        formPanel.add(txt_password, gbc);

        // Confirm Login Password
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Confirm Login Password:"), gbc);
        gbc.gridx = 1;
        txt_confirm_password = new JPasswordField(16);
        formPanel.add(txt_confirm_password, gbc);

        // Admin Security Key / Master Authorization Password
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Admin Authorization Key (min 8 chars):"), gbc);
        gbc.gridx = 1;
        txt_admin_key = new JPasswordField(16);
        formPanel.add(txt_admin_key, gbc);

        // Confirm Admin Security Key
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Confirm Authorization Key:"), gbc);
        gbc.gridx = 1;
        txt_confirm_admin_key = new JPasswordField(16);
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
        char[] adminKey = txt_admin_key.getPassword();
        char[] confirmAdminKey = txt_confirm_admin_key.getPassword();

        try {
            // Guard: strictly ensure setup is only allowed on uninitialized installations
            PasswordSecurity.SetupState state = PasswordSecurity.getSetupState(conn);
            if (state != PasswordSecurity.SetupState.UNINITIALIZED) {
                JOptionPane.showMessageDialog(this,
                        "Initial setup is not permitted: The database is not in an uninitialized state (" + state + ").\n" +
                        "Existing accounts and credentials are strictly protected and cannot be overwritten.",
                        "Security Policy Violation", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Administrator username cannot be blank.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Strong password policy check
            PasswordSecurity.ValidationResult pwdVal = PasswordSecurity.validatePasswordStrength(pwd);
            if (!pwdVal.valid) {
                JOptionPane.showMessageDialog(this, pwdVal.message, "Password Security Policy", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!Arrays.equals(pwd, confirmPwd)) {
                JOptionPane.showMessageDialog(this, "Login passwords do not match. Please re-enter.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Strong admin authorization key policy check
            PasswordSecurity.ValidationResult keyVal = PasswordSecurity.validateAdminKeyStrength(adminKey);
            if (!keyVal.valid) {
                JOptionPane.showMessageDialog(this, keyVal.message, "Authorization Key Policy", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!Arrays.equals(adminKey, confirmAdminKey)) {
                JOptionPane.showMessageDialog(this, "Administrator authorization keys do not match. Please re-enter.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Atomic transactional setup of both accounts
            boolean success = PasswordSecurity.initializeFirstRunCredentials(conn, username, pwd, adminKey);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Initial administrator setup completed successfully!\nPlease log in using your newly configured credentials.",
                        "Setup Success", JOptionPane.INFORMATION_MESSAGE);
                setupCompleted = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Setup failed to save credentials. No partial changes were committed.",
                        "Database Transaction Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "An error occurred while saving administrator setup: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Memory hygiene: Wipe all sensitive password arrays on every exit path
            PasswordSecurity.wipe(pwd);
            PasswordSecurity.wipe(confirmPwd);
            PasswordSecurity.wipe(adminKey);
            PasswordSecurity.wipe(confirmAdminKey);
        }
    }

    public boolean isSetupCompleted() {
        return setupCompleted;
    }
}
