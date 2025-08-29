package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    //? EntityManger는 "트랜잭션 단위"로 다른데에 바인딩 되도록 라우팅해주기 때문에 멀티 스레드 환경에서의 동시성 문제는 없다!
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    //* Application 클래스에서 @Bean으로 수동 주입했기 때문에 바로 주입 가능
    /*public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
        this.em = em;
        this.queryFactory = queryFactory;
    }*/

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAllWithQuerydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsernameWithQuerydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition cond) {

        BooleanBuilder builder = new BooleanBuilder();

        //* null과 ""를 동시에 해결해주는 StringUtils
        if (hasText(cond.getUsername())) {
            builder.and(member.username.eq(cond.getUsername()));
        }

        if (hasText(cond.getTeamName())) {
            builder.and(team.name.eq(cond.getTeamName()));
        }

        if (cond.getAgeGoe() != null) {
            builder.and(member.age.goe(cond.getAgeGoe()));
        }

        if (cond.getAgeLoe() != null) {
            builder.and(member.age.loe(cond.getAgeLoe()));
        }


        return queryFactory
                // dto로 성능 최적화
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")

                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder) // 동적 쿼리
                .fetch();

    }

    public List<MemberTeamDto> search(MemberSearchCondition cond) {

        return queryFactory
                // dto로 성능 최적화
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")

                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageGoe(cond.getAgeGoe()),
                        ageLoe(cond.getAgeLoe())

                ) // 동적 쿼리
                .fetch();

    }

    private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
        return ageLoe(ageLoe).and(ageGoe(ageGoe));
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
