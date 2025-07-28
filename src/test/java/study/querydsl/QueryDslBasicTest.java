package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;


import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
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
        // especially those involving multiple GROUP BY clauses or HAVING clauses, where a proper count query cannot be reliably generated through standard JPA/JPQL mechanisms.
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

}
