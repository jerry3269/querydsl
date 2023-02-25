querydsl 을 사용하기위해  
JPAQueryFactory를 다음과 같이 스프링 빈으로 등록하였다.

<br>

```java
@Bean
JPAQueryFactory jpaQueryFactory(EntityManager em) {
    return new JPAQueryFactory(em);
}
```

> 참고  
스프링이 주입해주는 엔티티 매니저는 실제 동작 시점에 진짜 엔티티 매니저를 찾아주는 프록시용 가짜 엔티티 매니저이다. 이 가짜 엔티티 매니저는 실제 사용 시점에 트랜잭션 단위로 시ㄹ제 엔티티 매니저를 할당해준다. 따라서 동시성문제는 발생하지 않는다.

<br><Br>

# 1. 기본 설정

## 1) 조회 최적화 용 DTO를 생성

```java
@Data
public class MemberTeamDto {

    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
```

- 생성자를 사용하여 DTO를 조회하기 위해 @QueryProjection어노테이션 추가.

- QMemberTeamDto를 생성하기 위해 `./gradlew compileQuerydsl`실행

<br><Br>

## 2) 검색조건 클래스 생성

```java
@Data
public class MemberSearchCondition {

    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;

}
```

<Br><Br>

# 2. 동적 쿼리 - Builder

```java
public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .where(builder)
                .fetch();
    }
```

- 생성자를 사용했으므로, 필드 이름이 달라도 상관 X

- BooleanBuilder를 사용하여 동적 쿼리 생성

<br><Br>

# 3. 동적 쿼리 -  Where절 파라미터

```java
public List<MemberTeamDto> searchByWhere(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    private BooleanExpression ageBetween(Integer ageGoe, Integer ageLoe) {
        return ageGoe(ageGoe).and(ageLoe(ageLoe));
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
```

- `where절` 파라미터 방식은 조건 재사용 가능

- 반환 타입을 `BooleanExpression`로 설정하면 조건 조합도 가능

<br><Br>

# 4. 샘플 데이터 추가

편리한 데이터 확인을 위해 샘플 데이터 추가 

## 1) src/main/resources/application.yml

```yml
spring:
    profiles:
        active: local
```

<br><Br>

## 2) src/test/resources/application.yml

```yml
spring:
    profiles:
        active: test
```

- 해당 설정을 하지 않으면, test동작시에도 샘플 데이터가 들어감.

- 이렇게 분리하게되면 main소스코드와 테스트 소스코드 실행시 프로파일을 분리 가능.

<br><Br>

## 3) 샘플 데이터

```java
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {

        @PersistenceContext
        EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }
    }
}
```

- `@Profile("local")`: 위에서 설정한 local 프로파일 일때만 실행(테스트에서 실행시 실행 안됨.)

- `@PostConstruct`에 바로 코드를 넣지 않은 이유는 `@PostConstruct`과 `@Transactional`을 같이 사용할수 없기 때문

- 위와 같이 `@PostConstruct`와 `@Transactional`을 분리해야 함.

<br><Br>

# 5. 조회 API Controller

```java
@GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.searchByWhere(condition);
    }
```

- `/v1/members` 요청시 condition값이 없으면 모든 엔티티가 전부 나옴.

- 데이터가 너무 많을때는 매우 오랜시간이 걸릴 수 있으므로, 초기 condition값을 설정하는 것이 필수!

- 요청 파라미터가 `@ModelAttribute`에 의해 condition에 매핑됨.

- `http://localhost:8080/v1/members?teamName=teamB&ageGoe=31&ageLoe=35` 을 실행해보면 다음과 같은 결과를 얻을수 있음

```json
[
    {"memberId":34,"username":"member31","age":31,"teamId":2,"teamName":"teamB"},
    {"memberId":36,"username":"member33","age":33,"teamId":2,"teamName":"teamB"},
    {"memberId":38,"username":"member35","age":35,"teamId":2,"teamName":"teamB"}

]
```

<br>







