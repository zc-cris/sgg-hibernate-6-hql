package com.zc.cris.test.departmentDao;

import org.hibernate.Session;
import org.hibernate.engine.transaction.jta.platform.internal.SynchronizationRegistryBasedSynchronizationStrategy;

import com.zc.cris.test.enties.Department;
import com.zc.cris.test.hibernateUtils.HibernateUtils;

public class DepartmentDao {
	
	public void save(Department dept) {
		//1. 内部获取session对象
		//2. 获取和当前线程绑定的session对象
		//3. 不需要从外部传入session对象（外部不需要和session有任何关系）
		//4. 多个dao方法也可以使用一个事务
		Session session = HibernateUtils.getUibernateUtils().getSession();
		System.out.println(session.hashCode());
	}
	
	
	/*
	 * 不推荐使用这种方式进行保存：
	 * 1. 需要从service层传入一个session对象
	 * 2. service层需要和hibernate的 API 紧密的结合在一起了
	 */
	public void save(Session session, Department dept) {
		session.save(dept);
	}
}
