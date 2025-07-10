package com.library.ui;

import com.library.database.DatabaseConnection;
import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LibraryUI {
    private JFrame loginFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField studentSnoField;
    private JTextField studentNameField;
    private JComboBox<String> identityBox;

    public LibraryUI() {
        createLoginWindow();
    }

    private void createLoginWindow() {
        loginFrame = new JFrame("图书借阅系统登录");
        loginFrame.setSize(440, 360);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(245, 248, 250));
        mainPanel.setLayout(new GridBagLayout());

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true),
                BorderFactory.createEmptyBorder(24, 34, 24, 34)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 4, 10, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("图书借阅系统登录", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleLabel.setForeground(new Color(45, 101, 185));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        cardPanel.add(titleLabel, gbc);

        // 身份选择
        JLabel identityLabel = new JLabel("登录身份:");
        identityLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        identityBox = new JComboBox<>(new String[]{"管理员", "学生"});
        identityBox.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        gbc.gridy = 1; gbc.gridwidth = 1; gbc.gridx = 0;
        cardPanel.add(identityLabel, gbc);
        gbc.gridx = 1;
        cardPanel.add(identityBox, gbc);

        // 管理员输入区
        gbc.gridy = 2; gbc.gridx = 0;
        JLabel usernameLabel = new JLabel("管理员账号:");
        usernameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        cardPanel.add(usernameLabel, gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(16);
        usernameField.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        cardPanel.add(usernameField, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        JLabel passwordLabel = new JLabel("密码:");
        passwordLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        cardPanel.add(passwordLabel, gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(16);
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        cardPanel.add(passwordField, gbc);

        // 学生输入区
        gbc.gridy = 2; gbc.gridx = 0;
        JLabel snoLabel = new JLabel("借书证号:");
        snoLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        cardPanel.add(snoLabel, gbc);
        gbc.gridx = 1;
        studentSnoField = new JTextField(16);
        studentSnoField.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        cardPanel.add(studentSnoField, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        JLabel snameLabel = new JLabel("学生姓名:");
        snameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        cardPanel.add(snameLabel, gbc);
        gbc.gridx = 1;
        studentNameField = new JTextField(16);
        studentNameField.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        cardPanel.add(studentNameField, gbc);

        // 初始只显示管理员组件，隐藏学生组件
        snoLabel.setVisible(false);
        studentSnoField.setVisible(false);
        snameLabel.setVisible(false);
        studentNameField.setVisible(false);

        // 身份切换监听
        identityBox.addItemListener(e -> {
            boolean isAdmin = identityBox.getSelectedItem().equals("管理员");
            usernameLabel.setVisible(isAdmin);
            usernameField.setVisible(isAdmin);
            passwordLabel.setVisible(isAdmin);
            passwordField.setVisible(isAdmin);

            snoLabel.setVisible(!isAdmin);
            studentSnoField.setVisible(!isAdmin);
            snameLabel.setVisible(!isAdmin);
            studentNameField.setVisible(!isAdmin);
        });

        // 按钮面板
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);

        JButton loginBtn = new JButton("登录");
        loginBtn.setBackground(new Color(45, 101, 185));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setFont(new Font("微软雅黑", Font.BOLD, 15));
        loginBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.addActionListener(e -> login());
        btnPanel.add(loginBtn);

        JButton regBtn = new JButton("学生注册");
        regBtn.setBackground(new Color(32, 155, 49));
        regBtn.setForeground(Color.WHITE);
        regBtn.setFocusPainted(false);
        regBtn.setFont(new Font("微软雅黑", Font.BOLD, 15));
        regBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        regBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        regBtn.addActionListener(e -> new StudentRegisterFrame().setVisible(true));
        identityBox.addItemListener(event -> regBtn.setEnabled(identityBox.getSelectedItem().equals("学生")));
        regBtn.setEnabled(identityBox.getSelectedItem().equals("学生"));
        btnPanel.add(regBtn);

        cardPanel.add(btnPanel, gbc);

        mainPanel.add(cardPanel);

        loginFrame.setContentPane(mainPanel);
        loginFrame.setVisible(true);
    }

    private void login() {
        String identity = (String) identityBox.getSelectedItem();
        if ("管理员".equals(identity)) {
            String username = usernameField.getText().trim();
            String password = String.valueOf(passwordField.getPassword()).trim();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "账号和密码不能为空", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            adminLogin(username, password);
        } else {
            String sno = studentSnoField.getText().trim();
            String sname = studentNameField.getText().trim();
            if (sno.isEmpty() || sname.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "借书证号和学生姓名不能为空", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            studentLogin(sno, sname);
        }
    }

    // 管理员登录
    private void adminLogin(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM 管理员 WHERE ACO=? AND PWD=?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(loginFrame, "管理员登录成功！");
                loginFrame.dispose();
                // 跳转到管理员主界面
                new AdminMainFrame().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(loginFrame, "账号或密码错误", "登录失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(loginFrame, "数据库操作失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // 学生登录
    private void studentLogin(String sno, String sname) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM 学生 WHERE SNO=? AND SNA=?")) {
            ps.setString(1, sno);
            ps.setString(2, sname);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(loginFrame, "学生登录成功！");
                loginFrame.dispose();
                // 跳转到学生主界面，注意这里要传入学生姓名和借书证号
                new StudentMainFrame(rs.getString("SNA"), rs.getString("SNO")).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(loginFrame, "借书证号或学生姓名错误", "登录失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(loginFrame, "数据库操作失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 设置更现代的界面风格
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(LibraryUI::new);
    }
}