package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.cache.spi.support.QueryResultsRegionTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

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

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(QMember.member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getUserName())) {
            builder.and(QMember.member.username.eq(condition.getUserName()));
        }
        if (StringUtils.hasText(condition.getTeamName())) {
            builder.and(QTeam.team.name.eq(condition.getTeamName()));
        }
        if(condition.getAgeGoe() != null) {
            builder.and(QMember.member.age.goe(condition.getAgeGoe()));
        }
        if(condition.getAgeLoe() != null) {
            builder.and(QMember.member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(
                    new QMemberTeamDto(
                        QMember.member.id.as("memberId"),
                        QMember.member.username,
                        QMember.member.age,
                        QTeam.team.id.as("teamId"),
                        QTeam.team.name.as("teamName")
                    )
                )
                .from(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(
                        new QMemberTeamDto(
                                QMember.member.id.as("memberId"),
                                QMember.member.username,
                                QMember.member.age,
                                QTeam.team.id.as("teamId"),
                                QTeam.team.name.as("teamName")
                        )
                )
                .from(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .where(
                    usernameEq(condition.getUserName()),
                    teamNameEq(condition.getTeamName()),
                    ageGoeEq(condition.getAgeGoe()),
                    ageLoeEq(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String userName) {
        return StringUtils.hasText(userName) ? QMember.member.username.eq(userName) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? QTeam.team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoeEq(Integer ageGoe) {
        return ageGoe != null ? QMember.member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoeEq(Integer ageLoe) {
        return ageLoe != null ? QMember.member.age.loe(ageLoe) : null;
    }

}
