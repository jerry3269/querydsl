package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import java.util.List;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired private EntityManager em;

	@Test
	void contextLoads() {
		Hello m1 = new Hello();
		m1.setName("m1");
		Hello m2 = new Hello();
		m2.setName("m2");
		em.persist(m1);
		em.persist(m2);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QHello qHello = new QHello("h");

		List<Hello> hello = query
				.selectFrom(qHello)
				.fetch();

		for (Hello result : hello) {
			System.out.println("result.getId() = " + result.getName());
		}


	}

}
