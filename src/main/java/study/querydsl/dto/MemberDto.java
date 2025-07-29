package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

//! DTO에서만 사용할 것
//? shortcut for @ToString, @EqualsAndHashCode, @Getter, @Setter,@RequiredArgsConstructor
@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
