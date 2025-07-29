package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

//! DTO에서만 사용할 것
//? shortcut for @ToString, @EqualsAndHashCode, @Getter, @Setter,@RequiredArgsConstructor
@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    // Dto 클래스에 Q도메인을 생성해줌
    //! querydsl 디펜던시가 있기 때문에 DB 변경 시에 문제가 생김
    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
