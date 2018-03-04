package com.zc.cris.test.hibernateUtils;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateUtils {

	private HibernateUtils() {};
	private static HibernateUtils instance = new HibernateUtils();

	public static HibernateUtils getUibernateUtils() {
		return instance;
	}

	private SessionFactory factory;

	public SessionFactory getSessionFactory() {
		if (factory == null) {
			final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
					.configure("/hibernate.cfg.xml").build();
			factory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
		}
		return factory;
	}

	public Session getSession() {
		//获取和当前线程绑定的session
		return getSessionFactory().getCurrentSession();
	}
	
}
