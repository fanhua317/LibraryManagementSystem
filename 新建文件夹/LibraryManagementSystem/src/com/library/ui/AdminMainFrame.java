package com.library.ui;

import com.library.database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;

public class AdminMainFrame extends JFrame {
    // --- 图书管理相关 ---
    private JTable bookTable;
    private DefaultTableModel bookModel;
    private JTextField searchBookField;

    // --- 学生管理相关 ---
    private JTable studentTable;
    private DefaultTableModel studentModel;

    // --- 借阅/欠款/学生借阅管理相关 ---
    private JTable borrowTable;
    private DefaultTableModel borrowModel;

    // --- 管理员账号管理 ---
    private JTable adminTable;
    private DefaultTableModel adminModel;

    public AdminMainFrame() {
        setTitle("图书借阅系统 - 管理员");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeDefaultAdmin();

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("图书管理", createBookPanel());
        tabbedPane.addTab("学生账号管理", createStudentPanel());
        tabbedPane.addTab("借阅/欠款管理", createBorrowManagePanel());
        tabbedPane.addTab("管理员账号管理", createAdminPanel());

        add(tabbedPane);
    }

    // ========== 图书管理 ==========
    private JPanel createBookPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        // 多条件查询区
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

        // 修正：添加双击监听，强制使用MouseAdapter且保证事件被触发
        bookTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                JTable table = (JTable) evt.getSource();
                if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
                    int row = table.rowAtPoint(evt.getPoint());
                    if (row != -1) {
                        int bno = Integer.parseInt(bookModel.getValueAt(row, 0).toString());
                        showBookDetailDialog(bno);
                    }
                }
            }
        });

        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("新增图书");
        JButton editBtn = new JButton("修改选中图书");
        JButton delBtn = new JButton("删除选中图书");
        btnPanel.add(addBtn); btnPanel.add(editBtn); btnPanel.add(delBtn);

        // 多条件查询事件
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

        addBtn.addActionListener(e -> {
            BookEditDialog dialog = new BookEditDialog(this, null);
            dialog.setVisible(true);
            if (dialog.isSucceeded()) loadAllBooks();
        });

        editBtn.addActionListener(e -> {
            int rowIdx = bookTable.getSelectedRow();
            if (rowIdx == -1) {
                JOptionPane.showMessageDialog(this, "请选择要修改的图书！");
                return;
            }
            int bno = Integer.parseInt(bookModel.getValueAt(rowIdx, 0).toString());
            BookEditDialog dialog = new BookEditDialog(this, bno);
            dialog.setVisible(true);
            if (dialog.isSucceeded()) loadAllBooks();
        });
        delBtn.addActionListener(e -> {
            int rowIdx = bookTable.getSelectedRow();
            if (rowIdx == -1) {
                JOptionPane.showMessageDialog(this, "请选择要删除的图书！");
                return;
            }

            int bno = Integer.parseInt(bookModel.getValueAt(rowIdx, 0).toString()); // 图书编号在第1列
            String bookName = bookModel.getValueAt(rowIdx, 1).toString();

            int opt = JOptionPane.showConfirmDialog(this, "确定要删除图书【" + bookName + "】吗？", "确认删除", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    // 检查是否有借阅记录
                    try (PreparedStatement checkPs = conn.prepareStatement("SELECT COUNT(*) FROM 借阅 WHERE BNO=?")) {
                        checkPs.setInt(1, bno);
                        ResultSet rs = checkPs.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            JOptionPane.showMessageDialog(this, "该图书存在借阅记录，无法删除！");
                            return;
                        }
                    }

                    // 删除图书
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM 图书 WHERE BNO=?")) {
                        ps.setInt(1, bno);
                        ps.executeUpdate();
                        JOptionPane.showMessageDialog(this, "删除成功！");
                        loadAllBooks(); // 刷新图书列表
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "删除失败: " + ex.getMessage());
                }
            }
        });
        

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
                // 可扩展更多字段
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
    // ========== 学生账号管理 ==========
    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        studentModel = new DefaultTableModel(new String[]{"姓名", "系别", "专业", "上限", "借书证号"}, 0);
        studentTable = new JTable(studentModel);

        // --- 筛选区 ---
        JPanel filterPanel = new JPanel();
        JTextField nameField = new JTextField(8);
        JTextField deptField = new JTextField(8);
        JTextField majorField = new JTextField(8);
        JTextField snoField = new JTextField(8);
        JButton filterBtn = new JButton("筛选");
        JButton resetBtn = new JButton("重置");
        filterPanel.add(new JLabel("姓名:"));
        filterPanel.add(nameField);
        filterPanel.add(new JLabel("系别:"));
        filterPanel.add(deptField);
        filterPanel.add(new JLabel("专业:"));
        filterPanel.add(majorField);
        filterPanel.add(new JLabel("借书证号:"));
        filterPanel.add(snoField);
        filterPanel.add(filterBtn);
        filterPanel.add(resetBtn);

        filterBtn.addActionListener(e -> loadStudentsWithFilter(
                nameField.getText().trim(),
                deptField.getText().trim(),
                majorField.getText().trim(),
                snoField.getText().trim()
        ));
        resetBtn.addActionListener(e -> {
            nameField.setText("");
            deptField.setText("");
            majorField.setText("");
            snoField.setText("");
            loadAllStudents();
        });

        JButton addBtn = new JButton("新增学生账号");
        JButton delBtn = new JButton("删除选中账号");
        JButton editBtn = new JButton("修改选中账号");
        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(delBtn);

        loadAllStudents();

        // 新增学生
        addBtn.addActionListener(e -> {
            JTextField nameField2 = new JTextField();
            JTextField deptField2 = new JTextField();
            JTextField majorField2 = new JTextField();
            JTextField supField = new JTextField("2");
            JTextField snoField2 = new JTextField();
            JPanel regPanel = new JPanel(new GridLayout(5, 2, 8, 8));
            regPanel.add(new JLabel("姓名:")); regPanel.add(nameField2);
            regPanel.add(new JLabel("系别:")); regPanel.add(deptField2);
            regPanel.add(new JLabel("专业:")); regPanel.add(majorField2);
            regPanel.add(new JLabel("借书上限:")); regPanel.add(supField);
            regPanel.add(new JLabel("借书证号:")); regPanel.add(snoField2);
            int res = JOptionPane.showConfirmDialog(this, regPanel, "新增学生", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String name = nameField2.getText().trim();
                String dept = deptField2.getText().trim();
                String major = majorField2.getText().trim();
                int sup = 2;
                try { sup = Integer.parseInt(supField.getText().trim()); } catch (Exception ignore) {}
                String sno = snoField2.getText().trim();
                if (name.isEmpty() || dept.isEmpty() || major.isEmpty() || sno.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "所有字段均不能为空！");
                    return;
                }
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("INSERT INTO 学生 (SNA, SDE, SSP, SUP, SNO) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setString(1, name); ps.setString(2, dept); ps.setString(3, major);
                    ps.setInt(4, sup); ps.setString(5, sno);
                    ps.executeUpdate();
                    loadAllStudents();
                    JOptionPane.showMessageDialog(this, "新增成功！");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "新增失败: " + ex.getMessage());
                }
            }
        });

        // 修改学生
        editBtn.addActionListener(e -> {
            int row = studentTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "请选择要修改的学生账号！");
                return;
            }
            String snaOld = studentModel.getValueAt(row, 0).toString();
            String sdeOld = studentModel.getValueAt(row, 1).toString();
            String sspOld = studentModel.getValueAt(row, 2).toString();
            String supOld = studentModel.getValueAt(row, 3).toString();
            String snoOld = studentModel.getValueAt(row, 4).toString();

            JTextField nameField2 = new JTextField(snaOld);
            JTextField deptField2 = new JTextField(sdeOld);
            JTextField majorField2 = new JTextField(sspOld);
            JTextField supField = new JTextField(supOld);
            JTextField snoField2 = new JTextField(snoOld);
            snoField2.setEditable(false); // 借书证号不允许修改

            JPanel regPanel = new JPanel(new GridLayout(5, 2, 8, 8));
            regPanel.add(new JLabel("姓名:")); regPanel.add(nameField2);
            regPanel.add(new JLabel("系别:")); regPanel.add(deptField2);
            regPanel.add(new JLabel("专业:")); regPanel.add(majorField2);
            regPanel.add(new JLabel("借书上限:")); regPanel.add(supField);
            regPanel.add(new JLabel("借书证号:")); regPanel.add(snoField2);

            int res = JOptionPane.showConfirmDialog(this, regPanel, "修改学生账号", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String name = nameField2.getText().trim();
                String dept = deptField2.getText().trim();
                String major = majorField2.getText().trim();
                int sup = 2;
                try { sup = Integer.parseInt(supField.getText().trim()); } catch (Exception ignore) {}
                String sno = snoField2.getText().trim();
                if (name.isEmpty() || dept.isEmpty() || major.isEmpty() || sno.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "所有字段均不能为空！");
                    return;
                }
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "UPDATE 学生 SET SNA=?, SDE=?, SSP=?, SUP=? WHERE SNO=?")) {
                    ps.setString(1, name);
                    ps.setString(2, dept);
                    ps.setString(3, major);
                    ps.setInt(4, sup);
                    ps.setString(5, sno);
                    ps.executeUpdate();
                    loadAllStudents();
                    JOptionPane.showMessageDialog(this, "修改成功！");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "修改失败: " + ex.getMessage());
                }
            }
        });

     // 删除学生
        delBtn.addActionListener(e -> {
            int row = studentTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "请选择要删除的学生账号！");
                return;
            }

            String sna = studentModel.getValueAt(row, 0).toString(); // 姓名在第1列（索引0）

            int opt = JOptionPane.showConfirmDialog(this, "确定要删除该学生账号吗?\n注意：该学生的所有借阅记录也将被删除！", "确认", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    // 1. 删除借阅记录（根据姓名）
                    try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM 借阅 WHERE SNA=?")) {
                        ps1.setString(1, sna);
                        ps1.executeUpdate();
                    }

                    // 2. 删除学生（根据姓名）
                    try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM 学生 WHERE SNA=?")) {
                        ps2.setString(1, sna);
                        ps2.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(this, "删除成功！");
                    loadAllStudents(); // 刷新学生列表
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "删除失败: " + ex.getMessage());
                }
            }
        });

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // 加载所有学生
    private void loadAllStudents() {
        studentModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SNA, SDE, SSP, SUP, SNO FROM 学生")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                studentModel.addRow(new Object[]{
                        rs.getString("SNA"),
                        rs.getString("SDE"),
                        rs.getString("SSP"),
                        rs.getInt("SUP"),
                        rs.getString("SNO")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage());
        }
    }

    // 按条件筛选学生
    private void loadStudentsWithFilter(String name, String dept, String major, String sno) {
        studentModel.setRowCount(0);
        StringBuilder sql = new StringBuilder("SELECT SNA, SDE, SSP, SUP, SNO FROM 学生 WHERE 1=1");
        if (!name.isEmpty()) sql.append(" AND SNA LIKE ?");
        if (!dept.isEmpty()) sql.append(" AND SDE LIKE ?");
        if (!major.isEmpty()) sql.append(" AND SSP LIKE ?");
        if (!sno.isEmpty()) sql.append(" AND SNO LIKE ?");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!name.isEmpty()) ps.setString(idx++, "%" + name + "%");
            if (!dept.isEmpty()) ps.setString(idx++, "%" + dept + "%");
            if (!major.isEmpty()) ps.setString(idx++, "%" + major + "%");
            if (!sno.isEmpty()) ps.setString(idx++, "%" + sno + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                studentModel.addRow(new Object[]{
                        rs.getString("SNA"),
                        rs.getString("SDE"),
                        rs.getString("SSP"),
                        rs.getInt("SUP"),
                        rs.getString("SNO")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "筛选失败: " + ex.getMessage());
        }
    }

    // ========== 借阅/欠款/学生借阅管理 ==========
    private JPanel createBorrowManagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        borrowModel = new DefaultTableModel(
            new String[]{"借阅编号", "姓名", "借书证号", "书名", "借书日期", "应还日期", "归还日期", "状态", "罚款"}, 0);
        borrowTable = new JTable(borrowModel);

        // --------- 筛选区 ---------
        JPanel filterPanel = new JPanel();
        JTextField snaField = new JTextField(8);
        JTextField bnaField = new JTextField(8);
        JTextField snoField = new JTextField(8);
        JComboBox<String> stateBox = new JComboBox<>(new String[]{"全部", "在借", "超期未还", "已归还", "已归还(欠款)", "超期未还(欠款)", "欠款"});
        JButton filterBtn = new JButton("筛选");
        JButton resetBtn = new JButton("重置");
        filterPanel.add(new JLabel("姓名:"));
        filterPanel.add(snaField);
        filterPanel.add(new JLabel("书名:"));
        filterPanel.add(bnaField);
        filterPanel.add(new JLabel("借书证号:"));
        filterPanel.add(snoField);
        filterPanel.add(new JLabel("状态:"));
        filterPanel.add(stateBox);
        filterPanel.add(filterBtn);
        filterPanel.add(resetBtn);

        filterBtn.addActionListener(e -> loadBorrowRecordsWithFilter(
                snaField.getText().trim(),
                bnaField.getText().trim(),
                snoField.getText().trim(),
                (String) stateBox.getSelectedItem()
        ));
        resetBtn.addActionListener(e -> {
            snaField.setText("");
            bnaField.setText("");
            snoField.setText("");
            stateBox.setSelectedIndex(0);
            loadBorrowRecords(null, null, null, "全部");
        });

        // 操作按钮
        JButton modifyBtn = new JButton("修改罚款金额");
        JButton clearBtn = new JButton("结清欠款");
        JPanel btnPanel = new JPanel();
        JButton modifyBorrowBtn = new JButton("修改借阅信息");
        btnPanel.add(modifyBorrowBtn);
        btnPanel.add(modifyBtn);
        btnPanel.add(clearBtn);

        // 默认加载全部
        loadBorrowRecords(null, null, null, "全部");
        //修改借阅信息
        modifyBorrowBtn.addActionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "请选择要修改的借阅记录！");
                return;
            }

            int borrowId = Integer.parseInt(borrowModel.getValueAt(row, 0).toString());
            String currentBorrowDate = borrowModel.getValueAt(row, 4).toString();
            String currentDueDate = borrowModel.getValueAt(row, 5).toString();

            JTextField borrowDateField = new JTextField(currentBorrowDate);
            JTextField dueDateField = new JTextField(currentDueDate);

            JPanel editPanel = new JPanel(new GridLayout(2, 2, 8, 8));
            editPanel.add(new JLabel("借书日期 (yyyy-MM-dd):"));
            editPanel.add(borrowDateField);
            editPanel.add(new JLabel("归还日期 (yyyy-MM-dd):"));
            editPanel.add(dueDateField);

            int res = JOptionPane.showConfirmDialog(this, editPanel, "修改借阅信息", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                try {
                    LocalDate borrowDate = LocalDate.parse(borrowDateField.getText().trim());
                    LocalDate dueDate = LocalDate.parse(dueDateField.getText().trim());

                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                             "UPDATE 借阅 SET BT = ?, RT = ? WHERE 借阅编号 = ?")) {
                        ps.setDate(1, Date.valueOf(borrowDate));
                        ps.setDate(2, Date.valueOf(dueDate));
                        ps.setInt(3, borrowId);
                        ps.executeUpdate();
                        loadBorrowRecords(null, null, null, "全部");
                        JOptionPane.showMessageDialog(this, "修改成功！");
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "修改失败: " + ex.getMessage());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "日期格式错误，应为 yyyy-MM-dd！");
                }
            }
        });
        // 修改罚款金额
        modifyBtn.addActionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "请选择要修改罚款的记录！");
                return;
            }
            int borrowId = Integer.parseInt(borrowModel.getValueAt(row, 0).toString());
            String oldMon = borrowModel.getValueAt(row, 8).toString();
            String input = JOptionPane.showInputDialog(this, "请输入新罚款金额：", oldMon);
            if (input == null) return;
            try {
                double mon = Double.parseDouble(input.trim());
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                         "UPDATE 借阅 SET MON=?, JUG=? WHERE 借阅编号=?")) {
                    ps.setDouble(1, mon);
                    ps.setBoolean(2, mon > 0); // 关键：设置欠款状态
                    ps.setInt(3, borrowId);
                    ps.executeUpdate();
                    loadBorrowRecords(null, null, null, "全部");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "金额输入有误！");
            }
        });
        // 结清欠款
        clearBtn.addActionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "请选择要结清的记录！");
                return;
            }
            int borrowId = Integer.parseInt(borrowModel.getValueAt(row, 0).toString());
            int opt = JOptionPane.showConfirmDialog(this, "确定要结清该欠款吗?", "确认", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("UPDATE 借阅 SET JUG=0, MON=0 WHERE 借阅编号=?")) {
                    ps.setInt(1, borrowId);
                    ps.executeUpdate();
                    loadBorrowRecords(null, null, null, "全部");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage());
                }
            }
        });
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(borrowTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadBorrowRecordsWithFilter(String sna, String bna, String sno, String state) {
        loadBorrowRecords(sna, bna, sno, state);
    }

    private void loadBorrowRecords(String sna, String bna, String sno, String state) {
        borrowModel.setRowCount(0);
        String sql =
            "SELECT j.借阅编号, s.SNA, s.SNO, t.BNA, j.BT, j.RT, j.JUG, j.MON " +
            "FROM 借阅 j " +
            "INNER JOIN 学生 s ON j.SNA=s.SNA " +
            "INNER JOIN 图书 t ON j.BNO=t.BNO WHERE 1=1";
        if (sna != null && !sna.isEmpty()) sql += " AND s.SNA LIKE ?";
        if (bna != null && !bna.isEmpty()) sql += " AND t.BNA LIKE ?";
        if (sno != null && !sno.isEmpty()) sql += " AND s.SNO LIKE ?";
        sql += " ORDER BY j.借阅编号 DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (sna != null && !sna.isEmpty()) ps.setString(idx++, "%" + sna + "%");
            if (bna != null && !bna.isEmpty()) ps.setString(idx++, "%" + bna + "%");
            if (sno != null && !sno.isEmpty()) ps.setString(idx++, "%" + sno + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalDate bt = rs.getDate("BT").toLocalDate();
                LocalDate due = bt.plusMonths(2);
                LocalDate rt = (rs.getDate("RT") != null) ? rs.getDate("RT").toLocalDate() : null;
                int jug = rs.getBoolean("JUG") ? 1 : 0;
                double mon = rs.getBigDecimal("MON").doubleValue();
                String rowState;
                if (jug == 1 && mon > 0) {
                    rowState = (rt == null) ? "超期未还(欠款)" : "已归还(欠款)";
                } else if (rt == null) {
                    rowState = due.isBefore(LocalDate.now()) ? "超期未还" : "在借";
                } else {
                    rowState = "已归还";
                }
                // 补充“欠款”筛选
                if ("欠款".equals(state) && !(jug == 1 && mon > 0)) continue;
                if (!"全部".equals(state) && !"欠款".equals(state) && !rowState.equals(state)) continue;

                borrowModel.addRow(new Object[]{
                    rs.getInt("借阅编号"),
                    rs.getString("SNA"),
                    rs.getString("SNO"),
                    rs.getString("BNA"),
                    bt,
                    due,
                    (rt != null ? rt : ""),
                    rowState,
                    rs.getBigDecimal("MON")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "加载借阅记录失败: " + ex.getMessage());
        }
    }

    // ========== 管理员账号管理 ==========
    private void initializeDefaultAdmin() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "IF NOT EXISTS (SELECT 1 FROM 管理员 WHERE ANO = 10001) " +
                 "INSERT INTO 管理员 (ANO, ACO, PWD) VALUES (10001, '10001', '10001')")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        adminModel = new DefaultTableModel(new String[]{"账号", "编号"}, 0);
        adminTable = new JTable(adminModel);

        // 筛选区
        JPanel filterPanel = new JPanel();
        JTextField acoField = new JTextField(8);
        JButton filterBtn = new JButton("筛选");
        JButton resetBtn = new JButton("重置");
        filterPanel.add(new JLabel("账号:"));
        filterPanel.add(acoField);
        filterPanel.add(filterBtn);
        filterPanel.add(resetBtn);

        filterBtn.addActionListener(e -> loadAdminsWithFilter(acoField.getText().trim()));
        resetBtn.addActionListener(e -> {
            acoField.setText("");
            loadAllAdmins();
        });

        JButton addBtn = new JButton("注册新管理员账号");
        JButton delBtn = new JButton("删除选中账号");
        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn);
        btnPanel.add(delBtn);

        loadAllAdmins();

        addBtn.addActionListener(e -> {
            JTextField acoField2 = new JTextField();
            JPasswordField pwdField = new JPasswordField();
            JPanel regPanel = new JPanel(new GridLayout(2, 2, 8, 8));
            regPanel.add(new JLabel("管理员账号:")); regPanel.add(acoField2);
            regPanel.add(new JLabel("密码:")); regPanel.add(pwdField);
            int res = JOptionPane.showConfirmDialog(this, regPanel, "注册管理员", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String aco = acoField2.getText().trim();
                String pwd = new String(pwdField.getPassword()).trim();
                if (aco.isEmpty() || pwd.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "所有字段均不能为空！");
                    return;
                }
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement getMaxAno = conn.prepareStatement("SELECT ISNULL(MAX(ANO),0) FROM 管理员");
                     PreparedStatement ps = conn.prepareStatement("INSERT INTO 管理员 (ANO, ACO, PWD) VALUES (?, ?, ?)")) {
                    ResultSet rs = getMaxAno.executeQuery();
                    int maxAno = 0;
                    if (rs.next()) maxAno = rs.getInt(1);
                    ps.setInt(1, maxAno + 1);
                    ps.setString(2, aco);
                    ps.setString(3, pwd);
                    ps.executeUpdate();
                    loadAllAdmins();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "注册失败: " + ex.getMessage());
                }
            }
        });

        delBtn.addActionListener(e -> {
            int rowIdx = adminTable.getSelectedRow();
            if (rowIdx == -1) {
                JOptionPane.showMessageDialog(this, "请选择要删除的管理员账号！");
                return;
            }
            int ano = Integer.parseInt(adminModel.getValueAt(rowIdx, 1).toString());

            if (ano == 10001) {
                JOptionPane.showMessageDialog(this, "默认管理员账号不可删除！");
                return;
            }

            int opt = JOptionPane.showConfirmDialog(this, "确定要删除该管理员账号吗?", "确认", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM 管理员 WHERE ANO=?")) {
                    ps.setInt(1, ano);
                    ps.executeUpdate();
                    loadAllAdmins();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "删除失败: " + ex.getMessage());
                }
            }
        });

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(adminTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadAllAdmins() {
        adminModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ANO, ACO FROM 管理员")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                adminModel.addRow(new Object[]{rs.getString("ACO"), rs.getInt("ANO")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage());
        }
    }

    private void loadAdminsWithFilter(String aco) {
        adminModel.setRowCount(0);
        String sql = "SELECT ANO, ACO FROM 管理员 WHERE 1=1";
        if (!aco.isEmpty()) sql += " AND ACO LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!aco.isEmpty()) ps.setString(1, "%" + aco + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                adminModel.addRow(new Object[]{rs.getString("ACO"), rs.getInt("ANO")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "筛选失败: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminMainFrame().setVisible(true));
    }
}