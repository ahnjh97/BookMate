// ============================================
// 파일: service/AuthService.java
//
// [이 클래스가 하는 일]
//   MEMBER, AUTHOR_ACCOUNT 관련 비즈니스 로직 전담.
//   controller는 이 클래스의 메서드만 호출하고, SQL이나 세션 조작은 모름.
//
// [메서드 목록 — 용도별로 묶어서 정리]
//   ── 로그인/조회 ──
//     login(loginId, password)      : 로그인 인증, 성공 시 비밀번호 지운 MemberDTO 반환
//     findMember(memberId)          : 회원번호로 단건 조회(세션 자가치유용)
//
//   ── 회원가입 ──
//     signup(member)                : 검증 → 해시 → 중복확인 → INSERT
//     isLoginIdAvailable(loginId)   : 회원가입 화면의 아이디 중복확인 버튼용
//
//   ── 회원정보 수정(신규) ──
//     isNicknameAvailable(nickname, memberId) : 수정 화면의 닉네임 중복확인(본인 제외)
//     isEmailAvailable(email, memberId)       : 수정 화면의 이메일 중복확인(본인 제외)
//     updateProfile(memberId, nickname, email) : 닉네임/이메일 저장, 저장 직전 서버 재검증 포함
//     changePassword(memberId, currentPassword, newPassword)) : 비밀번호 변경, 재인증 포함
//
//   ── 작가 인증(AUTHOR_ACCOUNT, 신규) ──
//     isAuthorAccount(memberId)         : 작가 인증 여부(true/false) — 로그인 시 세션에 저장할 때 사용
//     getSelfIntro(memberId)            : 작가 소개글 조회
//     updateSelfIntro(memberId, intro)  : 작가 소개글 저장
//
// [공통 규칙 — 모든 메서드에 반복 적용됨, 아래에 다시 안 적음]
//   · 메서드 하나당 DBUtil.getConnection()을 한 번만 열고 try-with-resources로 자동 close
//   · 입력값 검증(길이/형식)은 DB 접근 전에 먼저 함 — 잘못된 값으로 커넥션 낭비 안 하도록
//   · SQLException은 그대로 던지지 않고 RuntimeException으로 감싸서 던짐
//     (controller가 SQLException을 매번 처리 안 해도 되게, GlobalExceptionFilter가
//      RuntimeException 하나만 잡으면 되도록)
//   · 예외 메시지는 사용자에게 그대로 보여줘도 되는 문장으로 작성(내부 오류 노출 안 함)
// ============================================

package service;

import java.sql.Connection;
import java.sql.SQLException;

import dao.AuthDAO;
import dto.MemberDTO;
import exception.AuthenticationException;
import exception.DuplicateUserException;
import util.DBUtil;
import util.PasswordUtil;

public class AuthService {
    private final AuthDAO authDAO = new AuthDAO();

    // ---------- 로그인/조회 ----------

    public MemberDTO login(String loginId, String password) {
        requireLoginCredentials(loginId, password);
        try (Connection connection = DBUtil.getConnection()) {
            MemberDTO member = authDAO.selectByLoginId(connection, loginId.trim());
            if (member == null || !PasswordUtil.matches(password, member.getPassword())) {
                // 아이디가 없는 건지 비밀번호가 틀린 건지 구분해서 알려주지 않음
                // (구분해서 알려주면 "이 아이디는 존재한다"는 정보가 공격자에게 새어나감)
                throw new AuthenticationException("아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            member.setPassword(null); // 응답으로 나갈 객체이므로 해시값도 지워서 반환(방어적 조치)
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

    // ---------- 회원가입 ----------

    public long signup(MemberDTO member) {
        validateSignup(member);
        member.setLoginId(member.getLoginId().trim());
        member.setNickname(member.getNickname().trim());
        member.setEmail(member.getEmail().trim().toLowerCase());
        member.setPassword(PasswordUtil.hash(member.getPassword()));

        try (Connection connection = DBUtil.getConnection()) {
            // 애플리케이션 레벨에서 먼저 중복 검사(사용자에게 더 빠르고 정확한 메시지를 주기 위함)
            if (authDAO.existsByLoginId(connection, member.getLoginId())) {
                throw new DuplicateUserException("이미 사용 중인 아이디입니다.");
            }
            return authDAO.insert(connection, member);
        } catch (DuplicateUserException exception) {
            throw exception;
        } catch (SQLException exception) {
            // 애플리케이션 레벨 검사와 실제 INSERT 사이의 찰나에 다른 요청이 끼어들어
            // 중복이 생기는 경우(동시성 문제)까지 대비 — DB 자체의 UNIQUE 제약(에러코드 1)이
            // 걸리면 이것도 "중복" 문제로 다시 분류해서 사용자에게 보여줌
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

    // ---------- 회원정보 수정 (신규) ----------
    // 이 구간의 메서드들은 모두 "본인 제외" 중복확인을 씀 —
    // 이미 자신이 쓰고 있는 닉네임/이메일을 그대로 다시 저장해도 "중복"으로 막히면 안 되기 때문

    public boolean isNicknameAvailable(String nickname, long memberId) {
        if (nickname == null || nickname.trim().isBlank() || nickname.trim().length() > 30) {
            throw new IllegalArgumentException("닉네임은 1~30자로 입력해 주세요.");
        }
        try (Connection connection = DBUtil.getConnection()) {
            return !authDAO.existsByNicknameExcept(connection, nickname.trim(), memberId);
        } catch (SQLException exception) {
            throw new RuntimeException("닉네임 확인 중 오류가 발생했습니다.", exception);
        }
    }

    public boolean isEmailAvailable(String email, long memberId) {
        if (email == null || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.trim().length() > 100) {
            throw new IllegalArgumentException("올바른 이메일을 입력해 주세요.");
        }
        try (Connection connection = DBUtil.getConnection()) {
            return !authDAO.existsByEmailExcept(connection, email.trim().toLowerCase(), memberId);
        } catch (SQLException exception) {
            throw new RuntimeException("이메일 확인 중 오류가 발생했습니다.", exception);
        }
    }

    public void updateProfile(long memberId, String nickname, String email) {
        if (nickname == null || nickname.trim().isBlank() || nickname.trim().length() > 30) {
            throw new IllegalArgumentException("닉네임은 1~30자로 입력해 주세요.");
        }
        if (email == null || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.trim().length() > 100) {
            throw new IllegalArgumentException("올바른 이메일을 입력해 주세요.");
        }

        String trimmedNickname = nickname.trim();
        String normalizedEmail = email.trim().toLowerCase();

        try (Connection connection = DBUtil.getConnection()) {
            // 프론트에서 "중복확인" 버튼을 누르고 통과했더라도, 그 확인과 저장 버튼을
            // 누르는 시점 사이에 다른 사람이 같은 값을 선점했을 수 있으므로
            // 저장 직전에 서버가 한 번 더 확인함(프론트 검증만 믿지 않음)
            if (authDAO.existsByNicknameExcept(connection, trimmedNickname, memberId)) {
                throw new DuplicateUserException("이미 사용 중인 닉네임입니다.");
            }
            if (authDAO.existsByEmailExcept(connection, normalizedEmail, memberId)) {
                throw new DuplicateUserException("이미 사용 중인 이메일입니다.");
            }
            authDAO.updateProfile(connection, memberId, trimmedNickname, normalizedEmail);
        } catch (DuplicateUserException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new RuntimeException("회원정보 수정 중 오류가 발생했습니다.", exception);
        }
    }

    // 비밀번호 변경 — 현재 비밀번호가 맞는지 먼저 검증(재인증), 통과해야 새 비밀번호 저장
    public void changePassword(long memberId, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해 주세요.");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 100) {
            throw new IllegalArgumentException("새 비밀번호는 8~100자로 입력해 주세요.");
        }

        try (Connection connection = DBUtil.getConnection()) {
            MemberDTO member = authDAO.selectById(connection, memberId);
            if (member == null || !PasswordUtil.matches(currentPassword, member.getPassword())) {
                // 로그인 실패와 같은 이유로 구분 메시지 없이 통일
                throw new AuthenticationException("현재 비밀번호가 일치하지 않습니다.");
            }
            authDAO.updatePassword(connection, memberId, PasswordUtil.hash(newPassword));
        } catch (SQLException exception) {
            throw new RuntimeException("비밀번호 변경 중 오류가 발생했습니다.", exception);
        }
    }

    // ---------- 작가 인증(AUTHOR_ACCOUNT) (신규) ----------
    // AUTHOR_ACCOUNT는 회원가입 시 자동 생성되지 않음(작가 인증은 별도 심사 절차) —
    // 그래서 이 구간 메서드들은 전부 "이미 인증받은 회원"을 대상으로만 의미가 있음

    public boolean isAuthorAccount(long memberId) {
        try (Connection connection = DBUtil.getConnection()) {
            return authDAO.isAuthorAccount(connection, memberId);
        } catch (SQLException exception) {
            throw new RuntimeException("작가 인증 정보를 확인하지 못했습니다.", exception);
        }
    }

    public String getSelfIntro(long memberId) {
        try (Connection connection = DBUtil.getConnection()) {
            return authDAO.findSelfIntro(connection, memberId);
        } catch (SQLException exception) {
            throw new RuntimeException("작가 소개를 불러오지 못했습니다.", exception);
        }
    }

    public void updateSelfIntro(long memberId, String selfIntro) {
        // 컬럼 정의(AUTHOR_ACCOUNT.self_intro VARCHAR2(1000))에 맞춘 길이 제한
        if (selfIntro != null && selfIntro.length() > 1000) {
            throw new IllegalArgumentException("작가 소개는 1000자 이하로 입력해 주세요.");
        }
        try (Connection connection = DBUtil.getConnection()) {
            authDAO.updateSelfIntro(connection, memberId, selfIntro);
        } catch (SQLException exception) {
            throw new RuntimeException("작가 소개 저장 중 오류가 발생했습니다.", exception);
        }
    }

    // ---------- 내부 전용 검증(기존, 변경 없음) ----------

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