package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import javax.persistence.Id;
import java.util.List;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(QMember.member)
                .from(QMember.member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(QMember.member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .where(
                        usernameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoeEq(condition.getAgeGoe()),
                        ageLoeEq(condition.getAgeLoe())
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();

        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, query ->
                query.selectFrom(QMember.member)
                        .leftJoin(QMember.member.team, QTeam.team)
                        .where(
                                usernameEq(condition.getUserName()),
                                teamNameEq(condition.getTeamName()),
                                ageGoeEq(condition.getAgeGoe()),
                                ageLoeEq(condition.getAgeLoe())
                        )
        );
    }

    public Page<Member> applyPagination2(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, contentQuery ->
                contentQuery.selectFrom(QMember.member)
                    .leftJoin(QMember.member.team, QTeam.team)
                    .where(
                            usernameEq(condition.getUserName()),
                            teamNameEq(condition.getTeamName()),
                            ageGoeEq(condition.getAgeGoe()),
                            ageLoeEq(condition.getAgeLoe())
                    ),
                countQuery ->
                    countQuery.select(QMember.member.id)
                            .leftJoin(QMember.member.team, QTeam.team)
                            .where(
                                usernameEq(condition.getUserName()),
                                teamNameEq(condition.getTeamName()),
                                ageGoeEq(condition.getAgeGoe()),
                                ageLoeEq(condition.getAgeLoe())
                            )
        );
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
