// ============================================
// 파일: dao/AuthDAO.java
//
// [목적]
//   MEMBER, AUTHOR_ACCOUNT 테이블에 대한 SQL 실행 전담. 회원가입/로그인/
//   프로필조회(기존)에 이어, 회원정보 수정 화면(신규)에 필요한 중복확인·
//   갱신·작가정보 조회 메서드를 추가함.
//
// [설계 원칙 — Connection을 매개변수로 받는 이유]
//   MemberDao(초기 버전)와 달리 이 DAO는 DBUtil.getConnection()을 내부에서
//   직접 호출하지 않고, 호출하는 쪽(Service)이 Connection을 만들어 넘겨줌.
//   이렇게 하면 Service 메서드 하나 안에서 여러 DAO 메서드를 같은 Connection으로
//   묶어 부를 수 있어, 트랜잭션(commit/rollback) 제어를 Service 계층에서
//   일괄적으로 할 수 있음.
//
// [본인 제외(Except) 패턴 — existsByNicknameExcept, existsByEmailExcept]
//   회원가입 때 쓰던 existsByLoginId 같은 단순 중복확인과 다르게, 수정 화면에서는
//   "아무것도 안 바꾸고 그대로 저장"해도 본인 값과 중복이라며 막히면 안 됨
//   그래서 WHERE 절에 member_id != ? 조건을 추가해 본인은 검사 대상에서 제외
//
// [PreparedStatement만 사용 — SQL Injection 방지]

//   모든 메서드가 ? 파라미터 바인딩만 쓰고 문자열 조합으로 SQL을 만들지 않음.
//
// [메서드 목록]
//   기존(회원가입/로그인 관련, 변경 없음)
//     existsByLoginId(conn, loginId)                : 아이디 중복확인
//     selectByLoginId(conn, loginId)                 : 로그인용 조회(is_locked='N' 조건 포함)
//     selectById(conn, memberId)                     : 회원번호로 단건 조회
//     insert(conn, member)                           : 회원가입 INSERT, 생성된 PK 반환
//
//   신규(회원정보 수정 화면용)
//     updatePassword(connection, memberId, hashedPassword) : 비밀번호 갱신 — 이미 해시된 값을 받아서 그대로 저장(해시는 Service가 처리)
//     existsByNicknameExcept(conn, nickname, memberId) : 닉네임 중복확인(본인 제외)
//     existsByEmailExcept(conn, email, memberId)       : 이메일 중복확인(본인 제외)
//     updateProfile(conn, memberId, nickname, email)   : 닉네임/이메일 갱신
//     findSelfIntro(conn, memberId)                    : AUTHOR_ACCOUNT.self_intro 조회,
//                                                         작가 인증 안 됐으면 null 반환
//     isAuthorAccount(conn, memberId)                  : 작가 인증 여부만 true/false로 확인
//     updateSelfIntro(conn, memberId, selfIntro)        : 작가 소개글 갱신
//                                                         (AUTHOR_ACCOUNT 행은 이미 있다고 전제,
//                                                          작가 인증 절차에서 별도 생성됨)
// ============================================
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import dto.MemberDTO;

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

    public MemberDTO selectById(Connection connection, long memberId) throws SQLException {
        String sql = "SELECT member_id, login_id, password, nickname, email, role FROM MEMBER WHERE member_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
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
    // 닉네임 중복확인(본인 제외) — 회원가입 때 만든 existsByLoginId와 목적은 같지만,
    // "자기 자신과는 안 겹친 걸로 친다"는 조건(member_id != ?)이 추가로 필요해서 별도 메서드로 분리
    public boolean existsByNicknameExcept(Connection connection, String nickname, long memberId) throws SQLException {
        String sql = "SELECT 1 FROM MEMBER WHERE nickname = ? AND member_id != ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nickname);
            statement.setLong(2, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // 이메일 중복확인(본인 제외) — 위와 완전히 같은 이유, 대상 컬럼만 다름
    public boolean existsByEmailExcept(Connection connection, String email, long memberId) throws SQLException {
        String sql = "SELECT 1 FROM MEMBER WHERE email = ? AND member_id != ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setLong(2, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // 닉네임/이메일 저장 — 회원가입(insert)의 수정판, 비밀번호/아이디는 안 건드림
    public void updateProfile(Connection connection, long memberId, String nickname, String email) throws SQLException {
        String sql = "UPDATE MEMBER SET nickname = ?, email = ? WHERE member_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nickname);
            statement.setString(2, email);
            statement.setLong(3, memberId);
            statement.executeUpdate();
        }
    }

    // 작가 인증 정보 조회 — AUTHOR_ACCOUNT 테이블에 이 회원의 행이 있는지 확인.
    // 있으면 self_intro까지 같이 가져오고, 없으면 null(=작가 아님)을 그대로 반환
    public String findSelfIntro(Connection connection, long memberId) throws SQLException {
        String sql = "SELECT self_intro FROM AUTHOR_ACCOUNT WHERE member_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("self_intro") : null;
            }
        }
    }

    // 작가 인증 여부만 필요할 때(true/false만) — findSelfIntro와 SQL은 같지만
    // "행이 있냐 없냐"만 궁금한 호출부(예: 세션에 isAuthor 저장할 때)를 위해 별도 제공
    public boolean isAuthorAccount(Connection connection, long memberId) throws SQLException {
        String sql = "SELECT 1 FROM AUTHOR_ACCOUNT WHERE member_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // self_intro 저장/수정 — AUTHOR_ACCOUNT 테이블은 회원가입 때 자동 생성 안 되므로
    // (작가 인증은 별도 절차) 행이 이미 존재한다는 전제 하에 UPDATE
    public void updateSelfIntro(Connection connection, long memberId, String selfIntro) throws SQLException {
        String sql = "UPDATE AUTHOR_ACCOUNT SET self_intro = ? WHERE member_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, selfIntro);
            statement.setLong(2, memberId);
            statement.executeUpdate();
        }
    }

    // 비밀번호 갱신 — 이미 해시된 값을 받아서 그대로 저장(해시는 Service가 처리)
    public void updatePassword(Connection connection, long memberId, String hashedPassword) throws SQLException {
        String sql = "UPDATE MEMBER SET password = ? WHERE member_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hashedPassword);
            statement.setLong(2, memberId);
            statement.executeUpdate();
        }
    }
}
