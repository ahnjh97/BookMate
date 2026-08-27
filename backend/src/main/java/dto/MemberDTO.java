package dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDTO {
    private Long memberId;
    private String loginId;
    private String password;
    private String nickname;
    private String email;
    private String role;
}
