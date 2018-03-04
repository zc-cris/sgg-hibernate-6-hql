package com.zc.cris.test;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zc.cris.test.departmentDao.DepartmentDao;
import com.zc.cris.test.enties.Department;
import com.zc.cris.test.enties.Employee;
import com.zc.cris.test.hibernateUtils.HibernateUtils;

class TestHibernate2 {

	private SessionFactory sessionFactory = null;
	private Session session = null;
	private Transaction transaction = null;

	/*
	 * 批处理，只建议使用jdbc原生的批处理方式，效率最高
	 */
	@Test
	void testBatch() {
		session.doWork(new Work() {
			
			@Override
			public void execute(Connection connection) throws SQLException {
				//批处理方式1(好处自己看)
//				String sql = "update GG_DEPARTMENT set name='福利部' where id = 10";
//				String sql2 = "insert into GG_DEPARTMENT(id,name) values (110,'前台部')";
//				
//				Statement st = connection.createStatement();
//				st.addBatch(sql);
//				st.addBatch(sql2);
//				st.executeBatch();
//				st.clearBatch();
				
				
				//批处理方式2（好处自己看代码）
				String sql = "update GG_DEPARTMENT SET name='什么部' where id = ?";
				PreparedStatement ps = connection.prepareStatement(sql);
				for(int i = 10;i<30;i+=10) {
					ps.setInt(1, i);
					ps.addBatch();
				}
				ps.executeBatch();
			}
		});
		
	}
	
	
	// junit 不支持多线程，先用主方法来进行设置
	public static void main(String[] args) {

		// 获取session
		// 开启事务
		Session session2 = HibernateUtils.getUibernateUtils().getSession();
		System.out.println("-------" + session2.hashCode());
		Transaction transaction = session2.beginTransaction();
		
		DepartmentDao departmentDao = new DepartmentDao();

		departmentDao.save(null);
		departmentDao.save(null);
		departmentDao.save(null);

		//若session是由thread来管理的，那么当事务提交或者回滚的时候，session就已经关闭了
		transaction.commit();
		System.out.println(session2.isOpen());
	}

	/*
	 * 测试时间戳缓存区域
	 */
	@Test
	void testTimeCache() {
		String hql = "from Employee";

		// 时间戳t1
		Query query = this.session.createQuery(hql);
		query.setCacheable(true);

		System.out.println(query.list().size());

		// 时间戳t2
		Employee employee = this.session.get(Employee.class, 1);
		employee.setSalary(333F);

		// 时间戳t3（将会比较与t1和t2之间的差值，以此判断缓存数据是否过期，需要重新查询）
		System.out.println(query.list().size());
	}

	/*
	 * 测试hql语句的查询缓存
	 */
	@Test
	void testQueryCache() {
		
		

//		String hql = "from Employee";
//
//		Query query = this.session.createQuery(hql);
//		query.setCacheable(true);
//
//		System.out.println(query.list().size());
//
//		System.out.println(query.list().size());
	}

	/*
	 * 实现集合数据的二级缓存
	 */
	@Test
	void testCollectionSecondLevelCache() {
		Department dept = this.session.get(Department.class, 10);
		System.out.println(dept.getName());
		System.out.println(dept.getEmps().size());

		this.transaction.commit();
		this.session.close();

		this.session = this.sessionFactory.openSession();
		this.transaction = this.session.beginTransaction();

		Department dept2 = this.session.get(Department.class, 10);
		System.out.println(dept2.getName());
		System.out.println(dept2.getEmps().size());

	}

	/*
	 * 类级别的二级缓存测试
	 */
	@Test
	void testSecondLevelCatche() {

		Employee employee = this.session.get(Employee.class, 1);
		System.out.println(employee);

		this.transaction.commit();
		this.session.close();

		this.session = this.sessionFactory.openSession();
		this.transaction = this.session.beginTransaction();
		Employee employee2 = this.session.get(Employee.class, 1);
		System.out.println(employee2);
	}

	/*
	 * 测试hql的更新和删除
	 */
	@Test
	void testHQLUpdate() {

		// String hql = "update Department d set d.name = :name where d.id = :id";
		// Query query = this.session.createQuery(hql);
		// query.setParameter("name", "搞基部").setParameter("id", 123);
		// query.executeUpdate();
		//
		String hql = "delete from Department d where d.id = :id";
		this.session.createQuery(hql).setParameter("id", 123).executeUpdate();
	}

	/*
	 * hql 主要是运用在查询和更新以及删除，其不支持增加 QBC 主要是运用简单的查询（方便，快捷） 而 本地sql 可以实现所有的功能或者使用session.save()方法增加数据 
	 */
	@Test
	void testNativeSql() {

		String sql = "insert into GG_DEPARTMENT values (?,?)";
		NativeQuery query = this.session.createSQLQuery(sql);

		query.setInteger(0, 123).setString(1, "开发部").executeUpdate();
	}

	/*
	 * 使用QBC进行排序和分页查询
	 */
	@Test
	void testQBC4() {

		Criteria criteria = this.session.createCriteria(Employee.class);

		// 1. 添加排序
		criteria.addOrder(Order.asc("salary"));
		criteria.addOrder(Order.desc("id"));

		// 2. 添加分页
		int pageNo = 1;
		int pageSize = 2;
		criteria.setFirstResult((pageNo - 1) * pageSize).setMaxResults(pageSize);
		criteria.list();
	}

	/*
	 * 使用QBC进行统计查询
	 */
	@Test
	void testQBC3() {

		Criteria criteria = this.session.createCriteria(Employee.class);

		// 统计查询：使用Projection 来表示，可以由Projections 的静态方法获取
		criteria.setProjection(Projections.max("salary"));

		System.out.println(criteria.uniqueResult());
	}

	/*
	 * 通过QBC实现and以及or查询
	 */
	@Test
	void testQBC2() {

		Criteria criteria = this.session.createCriteria(Employee.class);

		// 1. AND 查询
		Conjunction conjunction = Restrictions.conjunction();
		conjunction.add(Restrictions.like("name", "ja", MatchMode.ANYWHERE));
		Department dept = new Department();
		dept.setId(1);
		conjunction.add(Restrictions.eq("department", dept));
		// System.out.println(conjunction);

		// 2. OR 查询
		Disjunction disjunction = Restrictions.disjunction();
		disjunction.add(Restrictions.ge("salary", 1000F));
		disjunction.add(Restrictions.isNull("email"));

		criteria.add(disjunction);
		criteria.add(conjunction);

		criteria.list();
	}

	/*
	 * 第一个QBC查询（完全是基于对象的查询操作，更加方便）
	 */
	@Test
	void testQBC() {
		// 1. 创建criteria对象
		Criteria criteria = this.session.createCriteria(Employee.class);

		// 2. 添加查询条件：在QBC中查询条件使用criterion来表示，criterion可以通过Restrictions的静态方法获取
		criteria.add(Restrictions.eq("email", "23@qq.com"));
		criteria.add(Restrictions.gt("salary", 100F));

		// 执行查询(两种方法都可以)
		// Object result = criteria.uniqueResult();
		// System.out.println(result);

		List<Employee> result = criteria.list();
		System.out.println(result.get(0));
	}

	/*
	 * 从员工端查询部门信息（迫切内连接，如果使用左外连接可能会出错，因为有的员工可能没有对应部门）
	 */
	@Test
	void testInnerJoinFetch2() {

		String hql = "from Employee e inner join fetch e.department";
		List<Employee> list = this.session.createQuery(hql).list();
		System.out.println(list.size());

		for (Employee emp : list) {
			System.out.println(emp.getId() + "------" + emp.getDepartment().getName());
		}
	}

	/*
	 * 迫切内连接（只查询一方满足条件的数据，如果一方没有另外一方对应的数据，那么就不进行强制查询）
	 */
	@Test
	void testInnerJoinFetch() {

		// 不返回不满足查询条件的数据
		String hql = "from Department d inner join fetch d.emps";
		List<Department> list = this.session.createQuery(hql).list();
		list = new ArrayList<>(new LinkedHashSet<>(list));
		System.out.println(list.size());

	}

	/*
	 * 左外连接(查询出一方的同时也查询出另外一方，但是不会初始化另外一方的数据，即使一方没有另外一方的对应数据也要强制查询出来），一般不建议使用左外连接
	 */
	@Test
	void testLeftJoin() {

		// 还是使用distinct关键字去重,指定查询department
		String hql = "select distinct d from Department d left join d.emps";
		List<Department> list = this.session.createQuery(hql).list();
		System.out.println(list.size());

		for (Department dept : list) {
			System.out.println(dept.getName() + "--------" + dept.getEmps().size());
		}

		// 这里的泛型不能使用Department，因为返回的不是已经装配好的Department对象，而是department的实体和emps集合的代理一起的数组
		// String hql = "from Department d left join d.emps";
		// List<Object []> list = this.session.createQuery(hql).list();
		// System.out.println(list.size());
		//
		// for(Object[] objs : list) {
		// System.out.println(Arrays.asList(objs));
		// }
	}

	/*
	 * 测试迫切左外连接(查询一方的时候，就已经将另外一端的数据初始化好了，即使一方没有另外一方的对应数据也要强制查询出来）
	 */
	@Test
	void testLeftJoinFetch() {

		// 查询部门id等于员工id的所有记录的条数（有重复）
		// String hql = "from Department d left join fetch d.emps ";

		// 方式一去重：通过关键字distinct去重（有几个部门就初始化几个Department对象即可，但是在数据库中显示所有员工的记录数+没有员工的部门记录记录数）
		// String hql = "select distinct d from Department d left join fetch d.emps ";

		// 方式二去重：通过linkedHashSet集合属性
		String hql = "from Department d left join fetch d.emps";
		List<Department> list = this.session.createQuery(hql).list();
		list = new ArrayList<>(new LinkedHashSet<>(list));
		System.out.println(list.size());

		for (Department dept : list) {
			System.out.println(dept.getName() + "---------" + dept.getEmps().size());
		}
	}

	@Test
	void testGroupBy() {

		// 查询员工最低工资大于1200的部门里的员工最高薪资和最低薪资
		String hql = "select min(e.salary), max(e.salary) from Employee e " + "group by e.department "
				+ "having min(e.salary) > :minSalary";
		List<Object[]> list = this.session.createQuery(hql).setParameter("minSalary", 1200F).list();

		for (Object[] objs : list) {
			System.out.println(Arrays.asList(objs));
		}
	}

	/*
	 * 升级版投影查询
	 */
	@Test
	void testFieldQuery2() {

		// Employee必须要有对应的带参构造器（形参顺序必须一一对应）
		String hql = "select new Employee(e.id, e.name, e.email, e.department) " + "from Employee e "
				+ "where e.department = :dept";

		Department dept = new Department();
		dept.setId(20);
		List<Employee> list = this.session.createQuery(hql).setParameter("dept", dept).list();

		for (Employee emp : list) {
			System.out.println(
					emp.getId() + "---" + emp.getName() + "----" + emp.getEmail() + "----" + emp.getDepartment());
		}

	}

	/*
	 * 投影查询（默认返回Object数组的list）
	 */
	@Test
	void testFieldQuery() {

		String hql = "select e.email, e.name, e.id from Employee e where e.department = :dept";
		Department dept = new Department();
		dept.setId(20);
		List<Object[]> list = this.session.createQuery(hql).setParameter("dept", dept).list();

		for (Object[] objs : list) {
			System.out.println(Arrays.asList(objs));
		}

	}

	/*
	 * 命名查询
	 */
	@Test
	void testNamedQuery() {

		// 命名查询：将hql语句设置到对应的映射文件中的query节点
		Query query = this.session.getNamedQuery("empsBySalary");
		List list = query.setParameter("minSalary", 1000F).setParameter("maxSalary", 1300F).list();
		System.out.println(list.size());

	}

	/*
	 * 分页查询
	 */
	@Test
	void testPageQuery() {
		String hql = "from Employee";
		Query query = this.session.createQuery(hql);

		int pageNo = 2;
		int pageSize = 2;

		// hibernate特殊的分页公式（不需要关心底层数据库是mysql还是oracle）
		List<Employee> list = query.setFirstResult((pageNo - 1) * pageSize).setMaxResults(pageSize).list();

		System.out.println(list.get(0).getName());
	}

	/*
	 * 第一个hql查询语句
	 */
	@Test
	void testHQL() {

		// 1. 创建 query 对象的两种方式
		// 1.1 基于占位符设置查询参数（支持代码链）
		// String hql = "from Employee e where e.salary >?0 and e.email like ?1";
		// Query query = this.session.createQuery(hql).setParameter("0",
		// 1200F).setParameter("1", "%qq%");

		// 1.2 基于命名参数设置查询参数（支持代码链）
		String hql = "from Employee e where e.salary > :sal and e.email like :email" + " and e.department = :dept"
				+ " order by e.salary";
		Department dept = new Department();
		dept.setId(20);
		Query query = this.session.createQuery(hql).setParameter("sal", 1200F).setParameter("email", "%qq%")
				.setParameter("dept", dept); // 参数类型还可以是实体类型

		// 2. 绑定参数

		// 3. 执行查询
		List<Employee> list = query.list();
		System.out.println(list.size());
	}

	@Test
	void test() {

	}

	/**
	 * 
	 * @MethodName: init
	 * @Description: TODO (执行每次@Test 方法前需要执行的方法)
	 * @Return Type: void
	 * @Author: zc-cris
	 */
	@BeforeEach
	public void init() {

		// 在5.1.0版本汇总，hibernate则采用如下新方式获取：
		// 1. 配置类型安全的准服务注册类，这是当前应用的单例对象，不作修改，所以声明为final
		// 在configure("cfg/hibernate.cfg.xml")方法中，如果不指定资源路径，默认在类路径下寻找名为hibernate.cfg.xml的文件
		final StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure("/hibernate.cfg.xml")
				.build();
		// 2. 根据服务注册类创建一个元数据资源集，同时构建元数据并生成该应用唯一（一般情况下）的一个session工厂
		this.sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
		this.session = this.sessionFactory.openSession();
		this.transaction = this.session.beginTransaction();

	}

	/**
	 * 
	 * @MethodName: destroy
	 * @Description: TODO (执行每次@Test 方法后需要执行的方法注解)
	 * @Return Type: void
	 * @Author: zc-cris
	 */
	@AfterEach
	public void destroy() {

		this.transaction.commit();
		this.session.close();
		this.sessionFactory.close();
	}

}
