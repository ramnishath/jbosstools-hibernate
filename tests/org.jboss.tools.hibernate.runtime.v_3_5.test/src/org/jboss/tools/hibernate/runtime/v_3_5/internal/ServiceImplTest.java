package org.jboss.tools.hibernate.runtime.v_3_5.internal;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.JDBCReaderFactory;
import org.hibernate.cfg.reveng.DatabaseCollector;
import org.hibernate.cfg.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.JDBCReader;
import org.jboss.tools.hibernate.runtime.common.IFacade;
import org.jboss.tools.hibernate.runtime.common.IFacadeFactory;
import org.jboss.tools.hibernate.runtime.spi.IConfiguration;
import org.jboss.tools.hibernate.runtime.spi.IDatabaseCollector;
import org.jboss.tools.hibernate.runtime.spi.IJDBCReader;
import org.jboss.tools.hibernate.runtime.spi.IReverseEngineeringStrategy;
import org.junit.Assert;
import org.junit.Test;

public class ServiceImplTest {
	
	private static final IFacadeFactory FACADE_FACTORY = new FacadeFactoryImpl();
	
	private ServiceImpl service = new ServiceImpl();

	@Test
	public void testNewAnnotationConfiguration() {
		IConfiguration configuration = service.newAnnotationConfiguration();
		Assert.assertNotNull(configuration);
		Object target = ((IFacade)configuration).getTarget();
		Assert.assertNotNull(target);
		Assert.assertTrue(target instanceof AnnotationConfiguration);
	}
	
	@Test
	public void testNewJDBCReader() {
		IConfiguration configuration = 
				FACADE_FACTORY.createConfiguration(
						new Configuration());
		IReverseEngineeringStrategy engineeringStrategy = 
				FACADE_FACTORY.createReverseEngineeringStrategy(
						new DefaultReverseEngineeringStrategy());
		IJDBCReader jdbcReaderFacade = service.newJDBCReader(
				configuration, 
				engineeringStrategy);
		Assert.assertNotNull(jdbcReaderFacade);
		JDBCReader reader = (JDBCReader)((IFacade)jdbcReaderFacade).getTarget();
		Assert.assertNotNull(reader);		
	}
	
	@Test
	public void testNewDatabaseCollector() {
		Configuration cfg = new Configuration();
		IJDBCReader jdbcReader = FACADE_FACTORY.createJDBCReader(
				JDBCReaderFactory.newJDBCReader(
						cfg.getProperties(), 
						cfg.buildSettings(), 
						new DefaultReverseEngineeringStrategy()));
		IDatabaseCollector databaseCollectorFacade = 
				service.newDatabaseCollector(jdbcReader);
		Assert.assertNotNull(databaseCollectorFacade);
		DatabaseCollector databaseCollector = 
				(DatabaseCollector)((IFacade)databaseCollectorFacade).getTarget();
		Assert.assertNotNull(databaseCollector);
	}
	
}
