package dao;

import dto.AdminMemberDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    public List<AdminMemberDTO> selectMemberList(
            Connection conn
    ) throws SQLException {

        String sql = """
                SELECT
                    member_id,
                    login_id,
                    nickname,
                    email,
                    role,
                    fail_count,
                    is_locked,
                    last_login_at,
                    created_at
                FROM MEMBER
                ORDER BY member_id DESC
                """;

        List<AdminMemberDTO> memberList =
                new ArrayList<>();

        try (
                PreparedStatement pstmt =
                        conn.prepareStatement(sql);

                ResultSet rs =
                        pstmt.executeQuery()
        ) {

            while (rs.next()) {

                AdminMemberDTO member =
                        new AdminMemberDTO();

                member.setMemberId(
                        rs.getLong("member_id")
                );

                member.setLoginId(
                        rs.getString("login_id")
                );

                member.setNickname(
                        rs.getString("nickname")
                );

                member.setEmail(
                        rs.getString("email")
                );

                member.setRole(
                        rs.getString("role")
                );

                member.setFailCount(
                        rs.getInt("fail_count")
                );

                member.setIsLocked(
                        rs.getString("is_locked")
                );

                member.setLastLoginAt(
                        rs.getTimestamp("last_login_at")
                );

                member.setCreatedAt(
                        rs.getTimestamp("created_at")
                );

                memberList.add(member);
            }
        }

        return memberList;
    }
    public String selectMemberRole(
            Connection conn,
            long memberId
    ) throws SQLException {

        String sql = """
                SELECT role
                FROM MEMBER
                WHERE member_id = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        }

        return null;
    }

    public int updateMemberLock(
            Connection conn,
            long memberId,
            boolean locked
    ) throws SQLException {

        String sql = """
                UPDATE MEMBER
                SET
                    is_locked = ?,
                    fail_count =
                        CASE
                            WHEN ? = 'N'
                            THEN 0
                            ELSE fail_count
                        END
                WHERE member_id = ?
                """;

        String lockValue =
                locked ? "Y" : "N";

        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql)) {

            pstmt.setString(1, lockValue);
            pstmt.setString(2, lockValue);
            pstmt.setLong(3, memberId);

            return pstmt.executeUpdate();
        }
    }


    public int updatePostPin(
            Connection conn,
            long postId,
            boolean pinned
    ) throws SQLException {

        String sql = """
                UPDATE POST
                SET
                    is_pinned = ?,
                    updated_at = SYSDATE
                WHERE post_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql)) {

            pstmt.setString(
                    1,
                    pinned ? "Y" : "N"
            );

            pstmt.setLong(
                    2,
                    postId
            );

            return pstmt.executeUpdate();
        }
    }
}