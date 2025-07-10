package com.library.ui;

import com.library.database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class BorrowRecordEditDialog extends JDialog {
    private JTextField rtField, jugField, monField;
    private final int borrowId;
    private boolean succeeded = false;

    public BorrowRecordEditDialog(Frame owner, int borrowId) {
        super(owner, "修改借阅记录", true);
        this.borrowId = borrowId;
        setSize(340, 220);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(4, 2, 8, 8));

        add(new JLabel("归还日期(yyyy-MM-dd):")); rtField = new JTextField(); add(rtField);
        add(new JLabel("欠款状态(0正常/1欠款):")); jugField = new JTextField(); add(jugField);
        add(new JLabel("罚款金额:")); monField = new JTextField(); add(monField);

        JButton okBtn = new JButton("确定");
        JButton cancelBtn = new JButton("取消");
        add(okBtn); add(cancelBtn);

        loadRecord();

        okBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void loadRecord() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT RT, JUG, MON FROM 借阅 WHERE 借阅编号=?")) {
            ps.setInt(1, borrowId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                rtField.setText(rs.getDate("RT") != null ? rs.getDate("RT").toString() : "");
                jugField.setText(rs.getBoolean("JUG") ? "1" : "0");
                monField.setText(rs.getBigDecimal("MON").toString());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage());
        }
    }

    private void save() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE 借阅 SET RT=?, JUG=?, MON=? WHERE 借阅编号=?")) {
            String rt = rtField.getText().trim();
            if (rt.isEmpty()) ps.setNull(1, java.sql.Types.DATE);
            else ps.setDate(1, java.sql.Date.valueOf(rt));
            ps.setBoolean(2, "1".equals(jugField.getText().trim()));
            ps.setBigDecimal(3, new java.math.BigDecimal(monField.getText().trim()));
            ps.setInt(4, borrowId);
            ps.executeUpdate();
            succeeded = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage());
        }
    }

    public boolean isSucceeded() { return succeeded; }
}