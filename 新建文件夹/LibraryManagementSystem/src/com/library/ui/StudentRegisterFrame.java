package com.library.ui;

import com.library.database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class StudentRegisterFrame extends JFrame {
    private JTextField nameField;
    private JTextField deptField;
    private JTextField majorField;

    public StudentRegisterFrame() {
        setTitle("学生注册");
        setSize(370, 220);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 12));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true),
                BorderFactory.createEmptyBorder(22, 32, 22, 32)
        ));

        JLabel nameLabel = new JLabel("学生姓名:");
        nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        nameField = new JTextField();
        nameField.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        panel.add(nameLabel); panel.add(nameField);

        JLabel deptLabel = new JLabel("学生系别:");
        deptLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        deptField = new JTextField();
        deptField.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        panel.add(deptLabel); panel.add(deptField);

        JLabel majorLabel = new JLabel("所学专业:");
        majorLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        majorField = new JTextField();
        majorField.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        panel.add(majorLabel); panel.add(majorField);

        JButton registerBtn = new JButton("注册");
        registerBtn.setFont(new Font("微软雅黑", Font.BOLD, 15));
        registerBtn.setBackground(new Color(32, 155, 49));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.addActionListener(this::handleRegister);
        panel.add(new JLabel());
        panel.add(registerBtn);

        add(panel);
    }

    private void handleRegister(ActionEvent e) {
        String name = nameField.getText().trim();
        String dept = deptField.getText().trim();
        String major = majorField.getText().trim();
        int sup = 2; // 借书上限固定为2

        if (name.isEmpty() || dept.isEmpty() || major.isEmpty()) {
            JOptionPane.showMessageDialog(this, "所有字段均不能为空！");
            return;
        }

        String sno = generateUniqueSNO();
        if (sno == null) {
            JOptionPane.showMessageDialog(this, "借书证号生成失败！");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO 学生 (SNA, SDE, SSP, SUP, SNO) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, dept);
                ps.setString(3, major);
                ps.setInt(4, sup);
                ps.setString(5, sno);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "注册成功！您的借书证号为：" + sno, "注册成功", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "注册失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // 自动生成唯一借书证号，健壮处理非数字情况
    private String generateUniqueSNO() {
        String initial = "20250001";
        long maxNum = 20250000L;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SNO FROM 学生")) {
            while (rs.next()) {
                String sno = rs.getString(1);
                if (sno != null && sno.matches("\\d+")) {
                    long num = Long.parseLong(sno);
                    if (num > maxNum) maxNum = num;
                }
            }
            return String.format("%08d", maxNum + 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentRegisterFrame().setVisible(true));
    }
}