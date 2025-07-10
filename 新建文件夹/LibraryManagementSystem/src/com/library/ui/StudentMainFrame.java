package com.library.ui;

import com.library.database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StudentMainFrame extends JFrame {
    private String studentSNA;   // 学生姓名
    private final String studentSNO;   // 借书证号

    // 学生个人信息缓存
    private String stuDept = ""; // 系别
    private String stuMajor = ""; // 专业

    private JTable bookTable;
    private DefaultTableModel bookModel;
    private JTable borrowTable;
    private DefaultTableModel borrowModel;
    private JLabel fineSumLabel;  // 罚金合计标签

    public StudentMainFrame(String sna, String sno) {
        this.studentSNA = sna;
        this.studentSNO = sno;
        setTitle("图书借阅系统 - 学生 [" + sna + "]");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("图书查询与借阅", createBookPanel());
        tabbedPane.addTab("我的借阅/还书/罚款", createBorrowPanel());
        tabbedPane.addTab("个人中心", createPersonalPanel());

        add(tabbedPane);

        loadStudentInfo();
    }

    // ========== 图书查询与借阅 ==========
    private JPanel createBookPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        // 多条件查询输入区
        JPanel searchPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField tfName = new JTextField(8);
        JTextField tfNo = new JTextField(8);
        JTextField tfDate = new JTextField(8);
        JTextField tfPub = new JTextField(8);
        JTextField tfLoc = new JTextField(8);
        JComboBox<String> sortFieldBox = new JComboBox<>(new String[]{"图书编号", "出版日期"});
        JComboBox<String> sortTypeBox = new JComboBox<>(new String[]{"升序", "降序"});

        // 布局每一行
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; searchPanel.add(new JLabel("书名:"), gbc);
        gbc.gridx = 1; searchPanel.add(tfName, gbc);
        gbc.gridx = 2; searchPanel.add(new JLabel("编号:"), gbc);
        gbc.gridx = 3; searchPanel.add(tfNo, gbc);
        gbc.gridx = 4; searchPanel.add(new JLabel("出版日期(yyyy-MM-dd):"), gbc);
        gbc.gridx = 5; searchPanel.add(tfDate, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; searchPanel.add(new JLabel("出版社:"), gbc);
        gbc.gridx = 1; searchPanel.add(tfPub, gbc);
        gbc.gridx = 2; searchPanel.add(new JLabel("存放位置:"), gbc);
        gbc.gridx = 3; searchPanel.add(tfLoc, gbc);
        gbc.gridx = 4; searchPanel.add(new JLabel("排序字段:"), gbc);
        gbc.gridx = 5; searchPanel.add(sortFieldBox, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; searchPanel.add(new JLabel("排序类型:"), gbc);
        gbc.gridx = 1; searchPanel.add(sortTypeBox, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JButton searchBtn = new JButton("查找图书");
        searchPanel.add(searchBtn, gbc);
        gbc.gridx = 2; gbc.gridwidth = 2;
        JButton refreshBtn = new JButton("刷新");
        searchPanel.add(refreshBtn, gbc);

        String[] cols = {"图书编号", "书名", "出版日期", "出版社", "存放位置", "库存"};
        bookModel = new DefaultTableModel(cols, 0);
        bookTable = new JTable(bookModel);

        JScrollPane scrollPane = new JScrollPane(bookTable);

        // 新增：双击表格行弹出详细信息
        bookTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && evt.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    int row = bookTable.rowAtPoint(evt.getPoint());
                    if (row != -1) {
                        int bno = Integer.parseInt(bookModel.getValueAt(row, 0).toString());
                        showBookDetailDialog(bno);
                    }
                }
            }
        });

        JButton borrowBtn = new JButton("借阅选中图书");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(borrowBtn);

        // 查询按钮事件
        searchBtn.addActionListener(e -> searchBooksMulti(
                tfName.getText().trim(),
                tfNo.getText().trim(),
                tfDate.getText().trim(),
                tfPub.getText().trim(),
                tfLoc.getText().trim(),
                (String) sortFieldBox.getSelectedItem(),
                (String) sortTypeBox.getSelectedItem()
        ));
        refreshBtn.addActionListener(e -> {
            tfName.setText(""); tfNo.setText(""); tfDate.setText(""); tfPub.setText(""); tfLoc.setText("");
            sortFieldBox.setSelectedIndex(0);
            sortTypeBox.setSelectedIndex(0);
            loadAllBooks();
        });

        borrowBtn.addActionListener(e -> borrowSelectedBook());

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadAllBooks();
        return panel;
    }

    // 新增：弹出详细信息对话框
    private void showBookDetailDialog(int bno) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM 图书 WHERE BNO=?")) {
            ps.setInt(1, bno);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                StringBuilder detail = new StringBuilder();
                detail.append("图书编号: ").append(rs.getInt("BNO")).append("\n");
                detail.append("书名: ").append(rs.getString("BNA")).append("\n");
                detail.append("出版日期: ").append(rs.getDate("BDA")).append("\n");
                detail.append("出版社: ").append(rs.getString("BPU")).append("\n");
                detail.append("存放位置: ").append(rs.getString("BPL")).append("\n");
                detail.append("库存: ").append(rs.getInt("BNU")).append("\n");
                JOptionPane.showMessageDialog(this, detail.toString(), "图书详细信息", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "未找到该图书的详细信息！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "查询失败: " + ex.getMessage());
        }
    }

    // 多条件组合查询+排序
    private void searchBooksMulti(
            String name, String no, String date, String pub, String loc,
            String sortField, String sortType
    ) {
        bookModel.setRowCount(0);
        StringBuilder sql = new StringBuilder("SELECT * FROM 图书 WHERE 1=1");
        if (!name.isEmpty()) sql.append(" AND BNA LIKE ?");
        if (!no.isEmpty()) sql.append(" AND BNO LIKE ?");
        if (!date.isEmpty()) sql.append(" AND BDA = ?");
        if (!pub.isEmpty()) sql.append(" AND BPU LIKE ?");
        if (!loc.isEmpty()) sql.append(" AND BPL LIKE ?");

        // 排序
        String field = "BNO";
        if ("图书编号".equals(sortField)) field = "BNO";
        else if ("出版日期".equals(sortField)) field = "BDA";
        sql.append(" ORDER BY ").append(field).append(" ");
        sql.append("升序".equals(sortType) ? "ASC" : "DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!name.isEmpty()) ps.setString(idx++, "%" + name + "%");
            if (!no.isEmpty()) ps.setString(idx++, "%" + no + "%");
            if (!date.isEmpty()) ps.setDate(idx++, java.sql.Date.valueOf(date));
            if (!pub.isEmpty()) ps.setString(idx++, "%" + pub + "%");
            if (!loc.isEmpty()) ps.setString(idx++, "%" + loc + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bookModel.addRow(new Object[]{
                        rs.getInt("BNO"),
                        rs.getString("BNA"),
                        rs.getDate("BDA"),
                        rs.getString("BPU"),
                        rs.getString("BPL"),
                        rs.getInt("BNU")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "多条件查询失败: " + ex.getMessage());
        }
    }

    private void loadAllBooks() {
        bookModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM 图书")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bookModel.addRow(new Object[]{
                        rs.getInt("BNO"),
                        rs.getString("BNA"),
                        rs.getDate("BDA"),
                        rs.getString("BPU"),
                        rs.getString("BPL"),
                        rs.getInt("BNU")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage());
        }
    }

    // 借书逻辑
    private void borrowSelectedBook() {
        int row = bookTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请选择要借阅的图书！");
            return;
        }
        int bno = Integer.parseInt(bookModel.getValueAt(row, 0).toString());

        // 检查是否有未结清的罚款
        if (hasUnpaidFine()) {
            JOptionPane.showMessageDialog(this, "您有超期罚款未缴清，不能借书！");
            return;
        }
        // 检查是否有未归还该书
        if (isBookAlreadyBorrowed(bno)) {
            JOptionPane.showMessageDialog(this, "您已借阅该图书且未归还！");
            return;
        }
        // 检查库存
        int stock = Integer.parseInt(bookModel.getValueAt(row, 5).toString());
        if (stock < 1) {
            JOptionPane.showMessageDialog(this, "该图书已无库存！");
            return;
        }
        // 检查借阅数量上限
        int borrowed = getBorrowedCount();
        int limit = getStudentLimit();
        if (borrowed >= limit) {
            JOptionPane.showMessageDialog(this, "已达借阅上限（" + limit + "）本！");
            return;
        }
        // 插入借阅记录
        try (Connection conn = DatabaseConnection.getConnection()) {
            int borrowId = 1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT ISNULL(MAX(借阅编号),0) FROM 借阅")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) borrowId = rs.getInt(1) + 1;
            }
            LocalDate today = LocalDate.now();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO 借阅 (借阅编号, BNO, SNA, BT, JUG, MON) VALUES (?, ?, ?, ?, 0, 0)");
            ps.setInt(1, borrowId);
            ps.setInt(2, bno);
            ps.setString(3, studentSNA);
            ps.setDate(4, Date.valueOf(today));
            ps.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("UPDATE 图书 SET BNU = BNU - 1 WHERE BNO=?");
            ps2.setInt(1, bno);
            ps2.executeUpdate();

            JOptionPane.showMessageDialog(this, "借阅成功！到期日：" + today.plusMonths(2));
            loadAllBooks();
            loadAllBorrows();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "借阅失败: " + ex.getMessage());
        }
    }

    private boolean hasUnpaidFine() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM 借阅 WHERE SNA=? AND JUG=1 AND MON>0")) {
            ps.setString(1, studentSNA);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) {
            return true;
        }
    }

    private int getStudentLimit() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUP FROM 学生 WHERE SNA=?")) {
            ps.setString(1, studentSNA);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return 2;
    }

    private int getBorrowedCount() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM 借阅 WHERE SNA=? AND RT IS NULL")) {
            ps.setString(1, studentSNA);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return 0;
    }

    private boolean isBookAlreadyBorrowed(int bno) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM 借阅 WHERE SNA=? AND BNO=? AND RT IS NULL")) {
            ps.setString(1, studentSNA);
            ps.setInt(2, bno);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) { return true; }
    }

// ========== 我的借阅/还书/罚款 ==========

    private JPanel createBorrowPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        // 多条件查询区
        JPanel searchPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField tfBookName = new JTextField(8);
        JTextField tfBookNo = new JTextField(8);
        JTextField tfBorrowDate = new JTextField(8);
        JTextField tfReturnDate = new JTextField(8);
        JComboBox<String> sortFieldBox = new JComboBox<>(new String[]{"还书日期", "借书日期", "图书编号", "图书名称"});
        JComboBox<String> sortTypeBox = new JComboBox<>(new String[]{"升序", "降序"});
        JComboBox<String> stateBox = new JComboBox<>(new String[]{"全部", "已归还", "未归还", "未缴费"});

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; searchPanel.add(new JLabel("图书名称:"), gbc);
        gbc.gridx = 1; searchPanel.add(tfBookName, gbc);
        gbc.gridx = 2; searchPanel.add(new JLabel("图书编号:"), gbc);
        gbc.gridx = 3; searchPanel.add(tfBookNo, gbc);
        gbc.gridx = 4; searchPanel.add(new JLabel("借书日期(yyyy-MM-dd):"), gbc);
        gbc.gridx = 5; searchPanel.add(tfBorrowDate, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; searchPanel.add(new JLabel("还书日期(yyyy-MM-dd):"), gbc);
        gbc.gridx = 1; searchPanel.add(tfReturnDate, gbc);
        gbc.gridx = 2; searchPanel.add(new JLabel("排序字段:"), gbc);
        gbc.gridx = 3; searchPanel.add(sortFieldBox, gbc);
        gbc.gridx = 4; searchPanel.add(new JLabel("排序类型:"), gbc);
        gbc.gridx = 5; searchPanel.add(sortTypeBox, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; searchPanel.add(new JLabel("状态筛选:"), gbc);
        gbc.gridx = 1; searchPanel.add(stateBox, gbc);
        gbc.gridx = 2; gbc.gridwidth = 2;
        JButton searchBtn = new JButton("查找记录");
        searchPanel.add(searchBtn, gbc);
        gbc.gridx = 4; gbc.gridwidth = 2;
        JButton refreshBtn = new JButton("刷新");
        searchPanel.add(refreshBtn, gbc);

        borrowModel = new DefaultTableModel(new String[] {
                "借阅编号", "图书编号", "书名", "借书日期", "应还日期", "归还日期", "状态", "罚款"
        }, 0);
        borrowTable = new JTable(borrowModel);

        // 罚金统计
        fineSumLabel = new JLabel("当前所欠罚金合计：0 元");

        JButton returnBtn = new JButton("归还选中图书");
        JButton renewBtn = new JButton("续借选中图书");
        JButton payBtn = new JButton("缴纳罚金");
        JPanel btnPanel = new JPanel();
        btnPanel.add(returnBtn);
        btnPanel.add(renewBtn);
        btnPanel.add(payBtn);

        // 查询按钮事件
        searchBtn.addActionListener(e -> searchBorrowsMulti(
                tfBookName.getText().trim(),
                tfBookNo.getText().trim(),
                tfBorrowDate.getText().trim(),
                tfReturnDate.getText().trim(),
                (String) sortFieldBox.getSelectedItem(),
                (String) sortTypeBox.getSelectedItem(),
                (String) stateBox.getSelectedItem()
        ));
        refreshBtn.addActionListener(e -> {
            tfBookName.setText(""); tfBookNo.setText(""); tfBorrowDate.setText(""); tfReturnDate.setText("");
            sortFieldBox.setSelectedIndex(0); sortTypeBox.setSelectedIndex(0); stateBox.setSelectedIndex(0);
            loadAllBorrows();
        });

        returnBtn.addActionListener(e -> returnSelectedBook());
        renewBtn.addActionListener(e -> renewSelectedBook());
        payBtn.addActionListener(e -> payFine());

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(borrowTable), BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(fineSumLabel, BorderLayout.WEST);
        southPanel.add(btnPanel, BorderLayout.EAST);
        panel.add(southPanel, BorderLayout.SOUTH);

        loadAllBorrows();
        return panel;
    }

    // 支持多条件筛选/排序/状态统计
    private void searchBorrowsMulti(
            String bna, String bno, String bdate, String rdate,
            String sortField, String sortType, String state
    ) {
        borrowModel.setRowCount(0);
        double fineSum = 0;
        StringBuilder sql = new StringBuilder(
                "SELECT j.借阅编号, j.BNO, t.BNA, j.BT, j.RT, j.JUG, j.MON " +
                "FROM 借阅 j INNER JOIN 图书 t ON j.BNO = t.BNO WHERE j.SNA=?"
        );
        if (!bna.isEmpty()) sql.append(" AND t.BNA LIKE ?");
        if (!bno.isEmpty()) sql.append(" AND j.BNO LIKE ?");
        if (!bdate.isEmpty()) sql.append(" AND j.BT = ?");
        if (!rdate.isEmpty()) sql.append(" AND j.RT = ?");

        String field = "j.RT";
        if ("还书日期".equals(sortField)) field = "j.RT";
        else if ("借书日期".equals(sortField)) field = "j.BT";
        else if ("图书编号".equals(sortField)) field = "j.BNO";
        else if ("图书名称".equals(sortField)) field = "t.BNA";
        sql.append(" ORDER BY ").append(field).append(" ");
        sql.append("升序".equals(sortType) ? "ASC" : "DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, studentSNA);
            if (!bna.isEmpty()) ps.setString(idx++, "%" + bna + "%");
            if (!bno.isEmpty()) ps.setString(idx++, "%" + bno + "%");
            if (!bdate.isEmpty()) ps.setDate(idx++, java.sql.Date.valueOf(bdate));
            if (!rdate.isEmpty()) ps.setDate(idx++, java.sql.Date.valueOf(rdate));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int jug = rs.getBoolean("JUG") ? 1 : 0;
                String rowState;
                String fine = "";
                LocalDate bt = rs.getDate("BT").toLocalDate();
                LocalDate due = bt.plusMonths(2);
                LocalDate rt = (rs.getDate("RT") != null) ? rs.getDate("RT").toLocalDate() : null;
                double mon = rs.getDouble("MON");

                // 状态识别 - 修复罚金状态判断
                if (jug == 1 && mon > 0) {
                    if (rt == null) {
                        rowState = "未归还";
                    } else {
                        rowState = "已归还";
                    }
                    fine = String.valueOf(mon);
                    fineSum += mon;
                } else if (rt == null) {
                    rowState = "未归还";
                } else {
                    rowState = "已归还";
                }
                
                // 按状态筛选
                if ("已归还".equals(state) && !rowState.equals("已归还")) continue;
                if ("未归还".equals(state) && !rowState.equals("未归还")) continue;
                if ("未缴费".equals(state) && !(jug == 1 && mon > 0)) continue;

                borrowModel.addRow(new Object[] {
                        rs.getInt("借阅编号"),
                        rs.getInt("BNO"),
                        rs.getString("BNA"),
                        bt,
                        due,
                        (rt != null ? rt : ""),
                        rowState,
                        fine
                });
            }
            fineSumLabel.setText("当前所欠罚金合计：" + fineSum + " 元");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "查询失败: " + ex.getMessage());
        }
    }

    private void loadAllBorrows() {
        // 全部，默认排序
        searchBorrowsMulti("", "", "", "", "还书日期", "降序", "全部");
    }

    private void returnSelectedBook() {
        int row = borrowTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请选择要归还的图书！");
            return;
        }
        int borrowId = Integer.parseInt(borrowModel.getValueAt(row, 0).toString());
        String state = (String) borrowModel.getValueAt(row, 6); // 状态在第7列

        if (!state.equals("未归还")) {
            JOptionPane.showMessageDialog(this, "该记录不可归还！");
            return;
        }

        LocalDate due = LocalDate.parse(borrowModel.getValueAt(row, 4).toString());
        LocalDate today = LocalDate.now();
        double fine = 0;
        boolean overdue = false;

        if (today.isAfter(due)) {
            long days = ChronoUnit.DAYS.between(due, today);
            fine = days * 0.2; // 调整为0.2元/天
            overdue = true;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE 借阅 SET RT=?, JUG=?, MON=? WHERE 借阅编号=?");
            ps.setDate(1, Date.valueOf(today));
            if (overdue) {
                ps.setBoolean(2, true);
                ps.setDouble(3, fine);
            } else {
                ps.setBoolean(2, false);
                ps.setDouble(3, 0);
            }
            ps.setInt(4, borrowId);
            ps.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("UPDATE 图书 SET BNU = BNU + 1 WHERE BNO=(SELECT BNO FROM 借阅 WHERE 借阅编号=?)");
            ps2.setInt(1, borrowId);
            ps2.executeUpdate();

            if (overdue) {
                JOptionPane.showMessageDialog(this, "归还成功！超期罚金：" + fine + "元，请在『我的借阅/还书/罚款』里缴纳罚金！");
            } else {
                JOptionPane.showMessageDialog(this, "归还成功！");
            }
            loadAllBorrows();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "归还失败: " + ex.getMessage());
        }
    }

    private void renewSelectedBook() {
        int row = borrowTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请选择要续借的图书！");
            return;
        }
        int borrowId = Integer.parseInt(borrowModel.getValueAt(row, 0).toString());
        String state = (String) borrowModel.getValueAt(row, 6); // 状态在第7列
        if (!state.equals("未归还")) {
            JOptionPane.showMessageDialog(this, "不可续借！");
            return;
        }
        returnSelectedBook();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT BNO FROM 借阅 WHERE 借阅编号=?")) {
            ps.setInt(1, borrowId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int bno = rs.getInt(1);
                SwingUtilities.invokeLater(() -> {
                    borrowSelectedBookByBno(bno);
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "续借失败: " + ex.getMessage());
        }
    }

    private void borrowSelectedBookByBno(int bno) {
        if (hasUnpaidFine()) {
            JOptionPane.showMessageDialog(this, "您有超期罚款未缴清，不能借书！");
            return;
        }
        int stock = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT BNU FROM 图书 WHERE BNO=?")) {
            ps.setInt(1, bno);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) stock = rs.getInt(1);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "续借失败（查询库存失败）: " + ex.getMessage());
            return;
        }
        if (stock < 1) {
            JOptionPane.showMessageDialog(this, "续借失败（无库存）！");
            return;
        }
        int borrowed = getBorrowedCount();
        int limit = getStudentLimit();
        if (borrowed >= limit) {
            JOptionPane.showMessageDialog(this, "已达借阅上限（" + limit + "）本，续借失败！");
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            int borrowId = 1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT ISNULL(MAX(借阅编号),0) FROM 借阅")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) borrowId = rs.getInt(1) + 1;
            }
            LocalDate today = LocalDate.now();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO 借阅 (借阅编号, BNO, SNA, BT, JUG, MON) VALUES (?, ?, ?, ?, 0, 0)");
            ps.setInt(1, borrowId);
            ps.setInt(2, bno);
            ps.setString(3, studentSNA);
            ps.setDate(4, Date.valueOf(today));
            ps.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("UPDATE 图书 SET BNU = BNU - 1 WHERE BNO=?");
            ps2.setInt(1, bno);
            ps2.executeUpdate();

            JOptionPane.showMessageDialog(this, "续借成功！新到期日：" + today.plusMonths(2));
            loadAllBooks();
            loadAllBorrows();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "续借失败: " + ex.getMessage());
        }
    }

    // 修复罚金缴纳功能
    private void payFine() {
        int row = borrowTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请选择要缴纳罚金的记录！");
            return;
        }
        
        // 获取罚款金额字符串
        String fineStr = borrowModel.getValueAt(row, 7).toString();
        
        // 如果罚款字段为空，说明没有罚款
        if (fineStr == null || fineStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "该记录没有欠款！");
            return;
        }
        
        // 尝试解析罚款金额
        double fineAmount;
        try {
            fineAmount = Double.parseDouble(fineStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "罚金数据格式错误！");
            return;
        }
        
        // 检查金额是否大于0
        if (fineAmount <= 0) {
            JOptionPane.showMessageDialog(this, "该记录没有欠款！");
            return;
        }
        
        int borrowId = Integer.parseInt(borrowModel.getValueAt(row, 0).toString());
        
        int opt = JOptionPane.showConfirmDialog(this, "确认缴纳罚金 " + fineAmount + " 元？", "确认", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE 借阅 SET JUG=0, MON=0 WHERE 借阅编号=?")) {
            ps.setInt(1, borrowId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "罚金缴纳成功！");
            loadAllBorrows();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage());
        }
    }

    // ========== 个人中心 ==========
    private JPanel createPersonalPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 8, 12, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel snoLabel = new JLabel("借书证号: ");
        JLabel snoValue = new JLabel(studentSNO);

        JLabel nameLabel = new JLabel("姓名: ");
        JTextField nameField = new JTextField(studentSNA, 14);

        JLabel deptLabel = new JLabel("系别: ");
        JTextField deptField = new JTextField(stuDept, 14);

        JLabel majorLabel = new JLabel("专业: ");
        JTextField majorField = new JTextField(stuMajor, 14);

        JButton saveBtn = new JButton("保存修改");
        JLabel infoLbl = new JLabel(" ");

        JButton refreshBtn = new JButton("刷新信息");
        refreshBtn.addActionListener(e -> {
            loadStudentInfo();
            nameField.setText(studentSNA);
            deptField.setText(stuDept);
            majorField.setText(stuMajor);
            infoLbl.setText("已刷新。");
        });

        saveBtn.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newDept = deptField.getText().trim();
            String newMajor = majorField.getText().trim();

            // 输入合法性检测
            if (newName.isEmpty() || newDept.isEmpty() || newMajor.isEmpty()) {
                infoLbl.setText("所有项不能为空！");
                return;
            }

            if (newName.length() < 2) {
                infoLbl.setText("学生姓名不得少于两个字！");
                return;
            }

            // 正则表达式：只允许中文、字母、空格
            String nameRegex = "^[\\u4e00-\\u9fa5a-zA-Z\\s]+$";
            String textRegex = "^[\\u4e00-\\u9fa5a-zA-Z\\s]+$";

            if (!newName.matches(nameRegex)) {
                infoLbl.setText("学生姓名不能包含数字或特殊字符！");
                return;
            }

            if (!newDept.matches(textRegex)) {
                infoLbl.setText("系别不能包含数字或特殊字符！");
                return;
            }

            if (!newMajor.matches(textRegex)) {
                infoLbl.setText("专业不能包含数字或特殊字符！");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE 学生 SET SNA=?, SDE=?, SSP=? WHERE SNO=?");
                 PreparedStatement ps2 = conn.prepareStatement(
                         "UPDATE 借阅 SET SNA=? WHERE SNA=?")) {

                // 更新学生表
                ps.setString(1, newName);
                ps.setString(2, newDept);
                ps.setString(3, newMajor);
                ps.setString(4, studentSNO);
                ps.executeUpdate();

                // 同步更新借阅表中的 SNA
                ps2.setString(1, newName);
                ps2.setString(2, studentSNA);
                ps2.executeUpdate();

                // 更新缓存
                studentSNA = newName;
                stuDept = newDept;
                stuMajor = newMajor;

                infoLbl.setText("修改成功！");
                this.setTitle("图书借阅系统 - 学生 [" + newName + "]");
            } catch (SQLException ex) {
                infoLbl.setText("修改失败: " + ex.getMessage());
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; panel.add(snoLabel, gbc);
        gbc.gridx = 1; panel.add(snoValue, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(nameLabel, gbc);
        gbc.gridx = 1; panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(deptLabel, gbc);
        gbc.gridx = 1; panel.add(deptField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(majorLabel, gbc);
        gbc.gridx = 1; panel.add(majorField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; panel.add(saveBtn, gbc);
        gbc.gridx = 1; panel.add(refreshBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; panel.add(infoLbl, gbc);

        return panel;
    }

    private void loadStudentInfo() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT SNA, SDE, SSP FROM 学生 WHERE SNO=?")) {
            ps.setString(1, studentSNO);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                studentSNA = rs.getString("SNA");
                stuDept = rs.getString("SDE");
                stuMajor = rs.getString("SSP");
            }
        } catch (SQLException ex) {
            // 可在必要时弹窗提示
        }
    }

    public static void main(String[] args) {
        // 示例：new StudentMainFrame("张三", "20250001").setVisible(true);
        // 实际请从登录后传入学生姓名和借书证号
    }
}