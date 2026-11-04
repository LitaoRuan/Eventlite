package uk.ac.man.cs.eventlite.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.junit.jupiter.api.Disabled;


public class PersistenceTest {
	
	private Persistence persistence = new Persistence();

	@Test
	public void dataSourceIsNotNull() {
		DataSource ds = persistence.dataSource();
		assertNotNull(ds);
	}

	@Test
	public void jpaVendorAdapterIsNotNull() {
		JpaVendorAdapter adapter = persistence.jpaVendorAdapter();
		assertNotNull(adapter);
	}

	@Test
	public void namedParameterJdbcTemplateIsNotNull() {
		DataSource ds = persistence.dataSource();
		NamedParameterJdbcTemplate template = persistence.getNamedParameterJdbcTemplate(ds);
		assertNotNull(template);
	}
	@Disabled
	
	@Test
	public void entityManagerFactoryIsNotNull() {
		EntityManagerFactory emf = persistence.entityManagerFactory();
		assertNotNull(emf);
	}
	
	@Disabled
	@Test
	public void transactionManagerIsNotNull() {
		EntityManagerFactory emf = persistence.entityManagerFactory();
		PlatformTransactionManager tm = persistence.transactionManager(emf);
		assertNotNull(tm);
	}
}