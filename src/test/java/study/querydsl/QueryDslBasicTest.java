package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;


import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
@Slf4j
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        // given
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    @DisplayName("JPQL 테스트")
    void startJPQL() {
        // when
        String qlString = "select m from Member m where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class).setParameter("username", "member1")
                .getSingleResult();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    @DisplayName("querydsl 테스트")
    void startQuerydsl() {
        // given: on-demand static으로 하면 코드가 깔끔해짐


        // when
        Member findMember = queryFactory.select(member)
                .from(member)
                // eq로 파라미터 바인딩을 대신 해줌
                //* sql injection 공격 방어도 가능
                .where(member.username.eq("member1"))
                .fetchOne();

        // then
        assertNotNull(findMember);
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("inputYourTestName")
    void search() {
        // given

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        // then
        assertNotNull(findMember);
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    @DisplayName("AND 테스트")
    void searchAndParam() {
        // given

        // when
        //* where문 안의 and는 콤마로 대체하는게 가장 베스트
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        (member.age.eq(10)))
                .fetchOne();

        // then
        assertNotNull(findMember);
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    @DisplayName("inputYourTestName")
    void resultFetch() {
        // given

        // when
        /*List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();*/

        //! fetchResults()는 dialect에 따라 count 쿼리가 생성되지 않을 수도 있기 때문에 fetch()를 대신 사용할 것!
        // Especially those involving multiple GROUP BY clauses or HAVING clauses, where a proper count query cannot be reliably generated through standard JPA/JPQL mechanisms.
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();


        // then

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    @DisplayName("inputYourTestName")
    void sort() {
        // given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.getFirst();
        Member member6 = result.get(1);
        Member memberNull = result.getLast();

        // then
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    @DisplayName("페이징 테스트")
    void paging1() {
        //! contents 쿼리와 count 쿼리는 "명시적으로" 분리해서 사용해야 한다.

        // 콘텐츠 조회 쿼리 (페이징 적용)
        List<Member> contents = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        // then
        assertThat(contents.size()).isEqualTo(2);

    }

    @Test
    @DisplayName("count 쿼리 테스트")
    void count() {
        // given

        // 전체 카운트 조회 쿼리 (페이징 미적용)
        //! 집계함수에는 selectFrom()을 사용할 수 없음!
        Long total = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        // then
        assertThat(total).isEqualTo(4);

    }

    @Test
    @DisplayName("집계함수 테스트")
    void aggregation() {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        // then
        Tuple tuple = result.getFirst();
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    @DisplayName("group by 테스트")
    void groupBy() {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        // then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원 조회
     */
    @Test
    @DisplayName("join 테스트")
    void join() {
        // given

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        // then
        assertThat(result)
                // 검증하고 싶은 값을 뽑아냄
                .extracting("username")
                // 그 값들이 예상과 정확히 일치하는지 "순서"까지 확인
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    @DisplayName("세타 조인 테스트")
    void theta_join() {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        // when
        List<Member> result = queryFactory
                .select(member)
                // FROM 절에 두 테이블을 나열하여 세타 조인을 수행했는데, 마침 WHERE 절의 조건이 '=' 연산자로 필터링하는 동일 조인으로 변환된 것
                //! Cross join은 카티션 프로덕트(ㆍ)라고 불리며, 특정 조건없이 모든 row의 조합 결과를 조회
                //? 아래의 FROM절은 SELECT * FROM member m CROSS JOIN team t WHERE m.username = t.name;과 동일한 작업을 수행
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    @DisplayName("연관관계가 있는 join..on 테스트")
    void join_on_filtering() {
        // given

        // when
        //* 외부(left, right, outer) 조인은 join...on절로, 그냥 내부(inner) 조인이면 where절을 쓰자.
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member) // 주 테이블
                // target: 주 테이블과 어떤 관계를 통해 연결된 필드인지 지정
                // alias: where절 또는 on절에서 편하게 사용하기 위해 지정
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    @DisplayName("연관관계가 없는 엔티티 조인 테스트")
    void join_on_no_relation() {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("페치 조인 없을 때")
    void fetchJoinNo() {
        // given
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        // then
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    @DisplayName("페치 조인 있을 때")
    void fetchJoinUse() {
        // given
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        // then
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    @DisplayName("서브 쿼리 테스트")
    void subQueryEq() {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        // then
        assertThat(result).
                extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    @DisplayName("서브 쿼리 테스트")
    void subQueryGoe() {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        // then
        assertThat(result).
                extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    @DisplayName("서브 쿼리 테스트")
    void subQueryIn() {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        // then
        assertThat(result).
                extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    @DisplayName("inputYourTestName")
    void selectSubQuery() {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();


        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    //! DB에서는 웬만해서는 case문을 사용하지 말 것!
    @Test
    @DisplayName("케이스 테스트")
    void basicCase() {
        // given

        // when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    @DisplayName("inputYourTestName")
    void complexCase() {
        // given

        // when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")

                )
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("orderby case 테스트")
    void orderByCase() {
        // given
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(3)
                .otherwise(1);

        // when
        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.asc())
                .fetch();

        // then
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            log.info("username: {}, age: {}, rank: {}", username, age, rank);
        }
    }

    @Test
    @DisplayName("상수 테스트")
    void constant() {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("D"))
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    @DisplayName("상수, 문자 더하기 테스트")
    void concatWithString() {
        // given

        // when
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("inputYourTestName")
    void simpleProjection() {
        // given

        // when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();


        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //! tuple은 querydsl에 종속적인 객체이기 때문에 repository 계층에서만 사용하고, 서비스 계층 이상에서는 Dto로 변환해서 사용할 것!
    @Test
    void tupleProjection() {
        // when
        //? 튜플은 리스트라고 생각하면 됨
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            log.info("username: {}, age: {}", username, age);
        }
    }

    @Test
    @DisplayName("inputYourTestName")
    void findDtoByJPQL() {
        // given

        // when
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("Setter로 Dto 조회")
    void findDtoBySetter() {
        // given

        // when
        //! Dto로 setter를 설정할 때 QueryDSL의 Projections 팩토리가 리플렉션 기술을 사용하기 때문에 Dto에 기본 생성자를 만들어야 한다.
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("Setter로 Dto 조회")
    void findDtoByField() {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }


    }

    @Test
    @DisplayName("생성자로 Dto 조회")
    void findDtoByConstructor() {
        // given

        // when
        //! 생성자 방식은 인자에 필드 순서를 맞춰야 한다.
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    @DisplayName("필드로 Dto 조회")
    void findUserDto() {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        //! 필드 이름이 맞아야 하기 때문에 as로 alias를 사용
        //? subQuery + as는 ExpressionUtils를 사용할 것
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        // then
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    @DisplayName("생성자로 UserDto 조회")
        //! Projects.constructor는 런타임 시에 에러를 발견함!
    void findUserDtoByConstructor() {

        // when
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("BooleanBuilder 테스트")
    void dynamicQuery_BooleanBuilder() {
        // given
        String usernameParam = "member1";
        Integer ageParam = null;

        // when
        List<Member> result = searchMember1(usernameParam, ageParam);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        //* BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));로 초기값을 설정할 수도 있음
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }


    //* 장점: 메서드를 다른 쿼리에서도 재활용할 수 있고 캡슐화를 할 수 있어서 코드 가독성 향상
    @Test
    @DisplayName("where 다중 파라미터 테스트")
    void dynamicQuery_WhereParam() {
        // given
        String usernameParam = "member1";
        Integer ageParam = 10;

        // when
        List<Member> result = searchMember2(usernameParam, ageParam);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    //* where 절은 아래와 같이 콤마로 여러 개의 메서드를 파라미터로 넘겨주는 것이 Best Practice
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    @Test
    @Commit
    @DisplayName("벌크 연산 테스트")
    void bulkUpdate() {
        // given


        // when
        //! bulk 연산(UPDATE, DELETE)은 영속성 컨텍스트를 무시하고 바로 DB로 쿼리를 때려버리는데, 영속성 컨텍스트(1차 캐시)와 DB의 상태가 다르면 1차 캐시를 먼저 조회하기 때문에 데이터 무결성이 깨짐!
        //? 위의 과정을 repeatable read라고 한다.
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //* 초기화로 해결 가능
        //? 아래의 과정은 안전장치로서 세트로 사용하자.
        em.flush(); // 혹시 모를 영속성 컨텍스트의 쓰기 지연 SQL을 DB에 반영
        em.clear(); // 영속성 컨텍스트를 비워서 1차 캐시를 깨끗하게 만듦

        // then
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        //* 1차 캐시를 비웠기 때문에 반영된 DB로부터 조회해서 데이터 정합성이 일치됨
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    @DisplayName("bulk 덧셈 연산 테스트")
    void bulkAdd() {
        // given

        // when
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();

        // then
    }

    @Test
    @Commit
    @DisplayName("bulk 제거 테스트")
    void bulkDelete() {
        // given

        // when
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        //* DELETE도 마찬가지로 bulk 연산에 해당하기 때문에 아래의 과정 필수
        em.flush();
        em.clear();

        // then
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        //* 1차 캐시를 비웠기 때문에 반영된 DB로부터 조회해서 데이터 정합성이 일치됨
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    @DisplayName("inputYourTestName")
    void sqlFunction() {
        // given

        // when
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("inputYourTestName")
    void sqlFunction2() {
        // given

        // when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();

        // then
        for (String s : result) {
            log.info("name: {}", s);
        }
    }

}
