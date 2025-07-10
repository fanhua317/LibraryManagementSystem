package com.library.ui;

import com.library.database.DatabaseConnection;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

public class BookEditDialog extends JDialog {
    private JTextField bnoField, bnaField, bdaField, bpuField, bplField, bnuField;
    private boolean succeeded = false;
    private Integer editBno = null;

    public BookEditDialog(Frame owner, Integer bno) {
        super(owner, bno == null ? "新增图书" : "修改图书", true);
        this.editBno = bno;
        setSize(400, 320);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(7, 2, 8, 8));

        bnoField = new JTextField();
        bnaField = new JTextField();
        bdaField = new JTextField(); // 出版日期 yyyy-MM-dd
        bpuField = new JTextField();
        bplField = new JTextField();
        bnuField = new JTextField();

        add(new JLabel("图书编号:")); add(bnoField);
        add(new JLabel("书名:")); add(bnaField);
        add(new JLabel("出版日期(yyyy-MM-dd):")); add(bdaField);
        add(new JLabel("出版社:")); add(bpuField);
        add(new JLabel("存放位置:")); add(bplField);
        add(new JLabel("库存:")); add(bnuField);

        JButton okBtn = new JButton("确定");
        JButton cancelBtn = new JButton("取消");
        add(okBtn); add(cancelBtn);

        if (editBno != null) loadBook(editBno);

        okBtn.addActionListener(e -> saveBook());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void loadBook(int bno) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM 图书 WHERE BNO=?")) {
            ps.setInt(1, bno);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                bnoField.setText(String.valueOf(rs.getInt("BNO")));
                bnoField.setEditable(false);
                bnaField.setText(rs.getString("BNA"));
                bdaField.setText(String.valueOf(rs.getDate("BDA")));
                bpuField.setText(rs.getString("BPU"));
                bplField.setText(rs.getString("BPL"));
                bnuField.setText(String.valueOf(rs.getInt("BNU")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "加载图书信息失败: " + e.getMessage());
        }
    }

    private void saveBook() {
        try {
            int bno = Integer.parseInt(bnoField.getText().trim());
            String bna = bnaField.getText().trim();
            Date bda = Date.valueOf(bdaField.getText().trim());
            String bpu = bpuField.getText().trim();
            String bpl = bplField.getText().trim();
            int bnu = Integer.parseInt(bnuField.getText().trim());
            if (bna.isEmpty() || bpu.isEmpty() || bpl.isEmpty()) {
                JOptionPane.showMessageDialog(this, "所有字段均不能为空！");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (editBno == null) {
                    // 新增
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO 图书 (BNO, BNA, BDA, BPU, BPL, BNU) VALUES (?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, bno);
                        ps.setString(2, bna);
                        ps.setDate(3, bda);
                        ps.setString(4, bpu);
                        ps.setString(5, bpl);
                        ps.setInt(6, bnu);
                        ps.executeUpdate();
                    }
                } else {
                    // 修改
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE 图书 SET BNA=?, BDA=?, BPU=?, BPL=?, BNU=? WHERE BNO=?")) {
                        ps.setString(1, bna);
                        ps.setDate(2, bda);
                        ps.setString(3, bpu);
                        ps.setString(4, bpl);
                        ps.setInt(5, bnu);
                        ps.setInt(6, editBno);
                        ps.executeUpdate();
                    }
                }
                succeeded = true;
                dispose();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage());
        }
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}