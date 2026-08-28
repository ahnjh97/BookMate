package service;

import dao.AuthDAO;
import dto.MemberDTO;
import exception.AuthenticationException;
import exception.DuplicateUserException;
import util.DBUtil;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class AuthService {
    private final AuthDAO authDAO = new AuthDAO();

    public MemberDTO login(String loginId, String password) {
        requireLoginCredentials(loginId, password);
        try (Connection connection = DBUtil.getConnection()) {
            MemberDTO member = authDAO.selectByLoginId(connection, loginId.trim());
            if (member == null || !PasswordUtil.matches(password, member.getPassword())) {
                throw new AuthenticationException("아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            member.setPassword(null);
            return member;
        } catch (SQLException exception) {
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다.", exception);
        }
    }

    public MemberDTO findMember(long memberId) {
        try (Connection connection = DBUtil.getConnection()) {
            MemberDTO member = authDAO.selectById(connection, memberId);
            if (member != null) member.setPassword(null);
            return member;
        } catch (SQLException exception) {
            throw new RuntimeException("회원 정보를 불러오지 못했습니다.", exception);
        }
    }

    public long signup(MemberDTO member) {
        validateSignup(member);
        member.setLoginId(member.getLoginId().trim());
        member.setNickname(member.getNickname().trim());
        member.setEmail(member.getEmail().trim().toLowerCase());
        member.setPassword(PasswordUtil.hash(member.getPassword()));

        try (Connection connection = DBUtil.getConnection()) {
            if (authDAO.existsByLoginId(connection, member.getLoginId())) {
                throw new DuplicateUserException("이미 사용 중인 아이디입니다.");
            }
            return authDAO.insert(connection, member);
        } catch (DuplicateUserException exception) {
            throw exception;
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1) {
                throw new DuplicateUserException("아이디, 닉네임 또는 이메일이 이미 사용 중입니다.");
            }
            throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다.", exception);
        }
    }

    public boolean isLoginIdAvailable(String loginId) {
        if (loginId == null || !loginId.trim().matches("[A-Za-z0-9_]{4,50}")) {
            throw new IllegalArgumentException("아이디는 영문, 숫자, 밑줄 4~50자로 입력해 주세요.");
        }
        try (Connection connection = DBUtil.getConnection()) {
            return !authDAO.existsByLoginId(connection, loginId.trim());
        } catch (SQLException exception) {
            throw new RuntimeException("아이디 확인 중 오류가 발생했습니다.", exception);
        }
    }

    private void requireLoginCredentials(String loginId, String password) {
        if (loginId == null || loginId.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("아이디와 비밀번호를 입력해 주세요.");
        }
    }

    private void validateSignup(MemberDTO member) {
        if (member == null) throw new IllegalArgumentException("회원 정보를 입력해 주세요.");
        if (member.getLoginId() == null || !member.getLoginId().trim().matches("[A-Za-z0-9_]{4,50}")) {
            throw new IllegalArgumentException("아이디는 영문, 숫자, 밑줄 4~50자로 입력해 주세요.");
        }
        if (member.getPassword() == null || member.getPassword().length() < 8 || member.getPassword().length() > 100) {
            throw new IllegalArgumentException("비밀번호는 8~100자로 입력해 주세요.");
        }
        if (member.getNickname() == null || member.getNickname().isBlank() || member.getNickname().trim().length() > 30) {
            throw new IllegalArgumentException("닉네임은 1~30자로 입력해 주세요.");
        }
        if (member.getEmail() == null || !member.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || member.getEmail().trim().length() > 100) {
            throw new IllegalArgumentException("올바른 이메일을 입력해 주세요.");
        }
    }
}
