package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;

public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    /*public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }*/

    /**
     * Creates a new {@link QuerydslRepositorySupport} instance for the given domain type.
     *
     * @param domainClass must not be {@literal null}.
     */
    public MemberRepositoryImpl(EntityManager em) {
        super(Member.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
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

    //fetchResults()를 사용하여 total count와 content 쿼리 동시 호출(total count 최적화 불가능)
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content,pageable, total);
    }

    //total count 쿼리 최적화 가능
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        //별도 쿼리 작성을 통한 total count 쿼리 최적화
        long total = queryFactory
                .select(QMember.member)
                .from(QMember.member)
                .where(
                        usernameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoeEq(condition.getAgeGoe()),
                        ageLoeEq(condition.getAgeLoe())
                )
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    public Page<MemberTeamDto> countQueryOptimization(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Member> countQuery = queryFactory
                .select(QMember.member)
                .from(QMember.member)
                .where(
                        usernameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoeEq(condition.getAgeGoe()),
                        ageLoeEq(condition.getAgeLoe())
                );

        //total count 쿼리가 필요한 경우만 countQuery::fetchCount(() -> countQuery.fetchCount())를 호출하여 사용
        //필요하지 않은경우 total count 쿼리를 사용하지 않음
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
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
