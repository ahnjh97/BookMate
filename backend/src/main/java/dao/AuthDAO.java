package dao;

import dto.MemberDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthDAO {
    public boolean existsByLoginId(Connection connection, String loginId) throws SQLException {
        String sql = "SELECT 1 FROM MEMBER WHERE login_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, loginId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public MemberDTO selectByLoginId(Connection connection, String loginId) throws SQLException {
        String sql = """
                SELECT member_id, login_id, password, nickname, email, role
                FROM MEMBER
                WHERE login_id = ? AND is_locked = 'N'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, loginId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                MemberDTO member = new MemberDTO();
                member.setMemberId(resultSet.getLong("member_id"));
                member.setLoginId(resultSet.getString("login_id"));
                member.setPassword(resultSet.getString("password"));
                member.setNickname(resultSet.getString("nickname"));
                member.setEmail(resultSet.getString("email"));
                member.setRole(resultSet.getString("role"));
                return member;
            }
        }
    }

    public long insert(Connection connection, MemberDTO member) throws SQLException {
        String sql = """
                INSERT INTO MEMBER (member_id, login_id, password, nickname, email, role)
                VALUES (SEQ_MEMBER.NEXTVAL, ?, ?, ?, ?, 'USER')
                """;
        String[] generatedColumns = {"MEMBER_ID"};
        try (PreparedStatement statement = connection.prepareStatement(sql, generatedColumns)) {
            statement.setString(1, member.getLoginId());
            statement.setString(2, member.getPassword());
            statement.setString(3, member.getNickname());
            statement.setString(4, member.getEmail());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("회원 번호를 가져오지 못했습니다.");
    }
}
