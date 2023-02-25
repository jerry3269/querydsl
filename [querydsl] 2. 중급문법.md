# 1. 프로젝션 결과 반환

## 1) 기본 - 대상 하나

```java
List<String> result = queryFactory
    .select(member.username)
    .from(member)
    .fetch();
```

<Br><Br>

## 2) 튜플 - 대상 둘 이상

```java
List<Tuple> result = queryFactory
    .select(member.username, member.age)
    .from(member)
    .fetch();
```

<Br><Br>

## 3) DTO조회 - 순수 JPA

```java
List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age)" +
                        " from Member m", MemberDto.class)
                .getResultList();
```

- 패키지이름이 지저분함

- 생성자 방식만 지원

<br><Br>

## 4) querydsl - 프로퍼티 접근

```java
List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
```

- DTO에 Setter가 존재해야 함.(아니면 null이 들어감)

<br><Br>

## 5) querydsl - 필드 직접 접근

```java
List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
```

- setter가 없어도 해당 필드에 데이터 직접 주입

- private으로 선언되었다 하더라도 주입이 됨.

<br><Br>

## 6) querydsl - 생성자 사용

```java
List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
```

<Br><Br>


## 7) querydsl - @QueryProjection

DTO 생성자에 다음 어노테이션 추가

```java
@QueryProjection
 public MemberDto(String username, int age) {
    this.username = username;
    this.age = age;
 }
```

<br>

이후 `./gradlew compileQuerydsl` 실행  
QMemberDto 생성 확인

<br>

```java
List<MemberDto> result = queryFactory
    .select(new QMemberDto(member.username, member.age))
    .from(member)
    .fetch();
```

- 컴파일러로 타입을 체크할 수 있음 -> 안전한 방법  

- DTO에 Querydsl관련된 어노테이션을 유지해야 하는점 -> DTO는 여러 계층에서 쓰임.  

- DTO까지 Q파일로 생성해야 함.

<br><Br>

## 8) 참고 - 별칭이 다를 때
DB에서 멤버를 DTO로 조회하려고 할때  
멤버변수에서의 필드이름과 DTO의 필드이름이 다르면 어떻게 해야할까?

- member(username , age) -> userDto(name, age)

<Br>

```java
List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(
                                select(member.age.avg())
                                        .from(member), "age")
                )).from(member)
                .fetch();
```

- 생성자 사용을 제외한, 프로퍼티 접근, 필드 접근방법은 생성방식에서 필드 이름이 같아야 한다. 

- 이름이 다를때는 위와 같은 방법을 사용한다.

- `ExpressionUtils.as(source,alias)` : 필드나, 서브 쿼리에 별칭 적용

- `username.as("memberName")` : 필드에 별칭 적용

<Br><Br>


# 2. 동적 쿼리

<br>

## 1) BooleanBuilder

```java
@Test
    public void 동적쿼리_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }
```

<br><Br>

## 2) Where 다중 파라미터

```java
@Test
    public void 동적쿼리_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

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

    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
```

- where 조건에서 null인 값은 자동으로 무시

- 메서드를 다른 쿼리에서도 재활용 할 수 있다.

- allEq처럼 메서드를 조합할 수 있다.(null 체크는 주의해서 처리)

- 가독성이 높아짐

<br><Br>

# 3. 벌크 연산

<br>

## 1) 대량 데이터 수정

```java
long count = queryFactory
    .update(member)
    .set(member.username, "비회원")
    .where(member.age.lt(28))
    .execute();
```

<Br>

## 2) 숫자 더하기

```java
long count = queryFactory
    .update(member)
    .set(member.age, member.age.add(1))
    .execute();
```

<Br>

## 3) 대량 데이터 삭제

```java
long count = queryFactory
    .delete(member)
    .where(member.age.gt(18))
    .execute();
```

<br>

## 4) 주의
> 벌크 연산은 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에 배치 쿼리를 실행하고 나면 영속성 컨텍스트를 초기화(flush, clear)해주는것이 안전.

<Br><Br>

# 4. SQL function 호출

- SQL function은 JPA와 같이 Dialect에 등록된 내용만 사용 가능

<br>

## 1) replace 함수

```java
List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace',{0},{1},{2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();
```

<br><br>

## 2) lower 함수

```java
List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(Expressions.stringTemplate(
                        "function('lower',{0})", member.username)))
                .fetch();
```

<Br><Br>

## 3) ansi 표준 함수

- lower 같은 ansi 표준 함수들은 querydsl이 상당부분 내장하고 있다. 따라서 다음과 같이 처리해도 결과는 같다.

<br>

```java
List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();
```

<Br><Br>




   










