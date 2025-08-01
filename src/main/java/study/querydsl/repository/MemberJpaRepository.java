package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.*;

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
}
