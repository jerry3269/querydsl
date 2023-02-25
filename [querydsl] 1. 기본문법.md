# 1. JPAQueryFactory

- EntityManager로 JPAQueryFactory를 생성한다.

- Querydsl은 JPQL 빌더이다.

- JPQL: 문자(실행시점 오류), Querydsl:코드(컴파일 시점 오류)

- JPQL: 파라미터 바인딩 직접, Querydsl: 파라미터 바인딩 자동 처리

<br><Br>

querydsl은 JPQL을 작성해주는 도구이다.

EntityManager로 JPAQueryFactory를 생성하고, JPAQueryFactory를 

이용하여 Querydsl로 JPQL을 생성한다.

> JPAQueryFactory를 필드로 제공하면 동시성 문제는 어떻게 될까? 

> 스프링 프레임워크는 여러 쓰레드에서 동시에 같은 EntityManager에 접근해도, 트랜잭션 마다 벼도의 영속성 컨텍스트를 제공하기 때문에, 동시성 문제는 자동으로 처리해준다.

<br><br>

# 2. Q-Type

Q클래스 인스턴스를 생성하는 방법은, intellij에서

오른쪽 Gradle클릭 > querydsl > Tasks > other > compileQuerydsl 클릭

을 하게되면 엔티티로 등록된 클래스에 대해 각각 Q-type을 생성해준다.

<br><Br>

querydsl에서 테이블을 조회할때 Q-type으로 조회하기 때문에

Q클래스 인스턴스를 사용하여 querydsl을 작성해야 한다.

## 1) Q클래스 인스턴스를 사용하는 2가지 방법

```java
QMember qMember = new QMember("m"); //별칭 직접 지정
QMember qMember = QMember.member; //기본 인스턴스 사용
```

<Br><Br>

> 참고: 같은 테이블을 조인해야 하는 경우가 아니라면 기본으로 생성된 인스턴스를 사용하자.

<br><Br>

# 3. 검색 조건 쿼리

```java
member.username.eq("member1") // username = 'member1'
member.username.ne("member1") //username != 'member1'
member.username.eq("member1").not() // username != 'member1'
member.username.isNotNull() //이름이 is not null
member.age.in(10, 20) // age in (10,20)
member.age.notIn(10, 20) // age not in (10, 20)
member.age.between(10,30) //between 10, 30
member.age.goe(30) // age >= 30
member.age.gt(30) // age > 30
member.age.loe(30) // age <= 30
member.age.lt(30) // age < 30
member.username.like("member%") //like 검색
member.username.contains("member") // like ‘%member%’ 검색
member.username.startsWith("member") //like ‘member%’ 검색
...
```

<Br><Br>

# 4. 결과조회

- fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
- fetchOne() : 단 건 조회
    - 결과가 없으면 : null
    - 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
- fetchFirst() : limit(1).fetchOne()
- fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
- fetchCount() : count 쿼리로 변경해서 count 수 조회

<Br><Br>

> 참고

> 페이징쿼리를 작성할 때, 데이터를 조회하는 쿼리를 여러 테이블을조인해야 하지만  
count 쿼리는 조인일 필요 없는 경우가 있다.  
count쿼리에 조인이 필요없는 성능 최적화를 하려면, count 전용 쿼리를 별도로 작성해야 한다.

<br><Br>

# 5. 조인

- `join()`, `innerJoin()`: 내부조인

- `leftJoin()`, `rightJoin()`: 외부 조인


## 1) querydsl과 on절

querydsl에서 join을 사용할때, 다음과 같이 사용한다.

```java
select(member)
.from(member)
.join(member.team, team)
.fetch()
```

일반적으로 join은 기준이 되는 on절이 있어야 한다.  
위의 join에서 두 파라미터를 넣게되면, 자동으로 두 파라미터의 id로 on절을 만든다.  

다음 코드를 보자

```java
selectFrom(member)
.join(team).on(member.username.eq(team.name))
.fetch()
```
위의 코드는 join에서 team을 조인하지만 기준이 없기때문에 on절을 달아주어야 한다.  
만약 on절이 없다면 컴파일 시점에 오류가 발생한다.


<br><Br>

## 2) 세타 조인

연관관계가 없는 필드로 조인을 하는 세타조인에 대해서 알아보겠다.

```java
List<Member> result = queryFactory
    .select(member)
    .from(member, team)
    .where(member.username.eq(team.name))
    .fetch();
```

다음과 같이 from절에 여러 엔티티를 선택해야 한다.

두 엔티티에서 `member.team`과 `team` 필드는 `id`로 연관관계가 맺어져 있다.  
연관관계가 없는 `member.username`와 `team.name` 필드로 조인을 하기 위해서는 from절에서 두 테이블의 모든 데이터를 가져와, where절에 조건을 넣어주어야 한다.  

하지만 세타조인은 외부조인이 불가능하다. 이러한 문제는 on절을 이용하여 해결할 수 있다.

<br><Br>

## 3) on절

on절은 join하기전 대상을 필터링 할 때, 연관관계가 없는 엔티티를 외부조인할때 사용한다.

<br><Br>

### a) 조인대상 필터링

```java
@Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftjoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
```

위와같이 작성하게 되면, team의 이름이 `teamA`인 팀만을 join하게 된다.  
leftjoin이기 때문에 member엔티티를 전부가져온다.  

나오는 데이터를 살펴보면, 멤버 엔티티가 모두 나오지만, `teamA`인 멤버 엔티티만 team필드가 있고, 나머지 멤버는 팀 필드에 null이 들어가게 된다.

<br><Br>

### b) 연관관계가 없는 엔티티 외부조인

```java
@Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
```

위의 코드를 살펴보면 연관관계가 없는 필드로 조인하기 때문에,  
leftJoin()파라미터로 team만이 들어가게 되고, on절을 추가하여 조인하는 필드 조건을 추가해 주었다.

<br><Br>

# 6. 서브 쿼리

```java
List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                    JPAExpressions
                        .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
```

위와 같이 사용할 수 있다.  
static method로 추가해서 더 간편하게 사용할 수 있다.  

<br>
<Br>

## 1) 한계

from 절에 서브쿼리를 지원하지 않는다. JPA에서 where절의 서브쿼리만을 지원한다.  
하이버네이트를 사용하면 select절의 서브쿼리를 지원해준다.  
하지만 from절의 서브쿼리는 아직 지원하지 않는다는 한계가 있다.

<br><Br>

## 2) 극복
1. 서브쿼리를 join으로 변경(가능한 상황도 있고, 불가능한 상황도 있음)
2. 애플리케이션에서 쿼리를 2번 분리해서 실행
3. nativeSQL을 사용

<br><Br>

# 7. Case
- select, where, orderBy에서 사용 가능

<br><Br>

## 1) 단순한 조건

```java
List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
```

<br><Br>

## 2) 복잡한 조건

```java
List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
```

<br><Br>

















