// BirthdayDAO.java
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BirthdayDAO {

    public void add(Birthday b) throws SQLException {
        String sql = "INSERT INTO birthdays (name, dob, notes) VALUES (?, ?, ?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getName());
            ps.setDate(2, Date.valueOf(b.getDob()));
            ps.setString(3, b.getNotes());
            ps.executeUpdate();
        }
    }

    public void update(Birthday b) throws SQLException {
        String sql = "UPDATE birthdays SET name = ?, dob = ?, notes = ? WHERE id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getName());
            ps.setDate(2, Date.valueOf(b.getDob()));
            ps.setString(3, b.getNotes());
            ps.setInt(4, b.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM birthdays WHERE id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Birthday> findAll() throws SQLException {
        String sql = "SELECT id, name, dob, notes FROM birthdays";
        List<Birthday> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Birthday> searchByName(String nameQuery) throws SQLException {
        String sql = "SELECT id, name, dob, notes FROM birthdays WHERE name LIKE ?";
        List<Birthday> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + nameQuery + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Birthday> searchByMonth(int month) throws SQLException {
        String sql = "SELECT id, name, dob, notes FROM birthdays WHERE MONTH(dob) = ?";
        List<Birthday> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Birthday mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        LocalDate dob = rs.getDate("dob").toLocalDate();
        String notes = rs.getString("notes");
        return new Birthday(id, name, dob, notes);
    }
}
