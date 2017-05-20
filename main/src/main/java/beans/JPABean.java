package main.java.beans;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@ManagedBean(name="jpaBean")
@ApplicationScoped
public class JPABean {
	private EntityManagerFactory emf;
	
	public EntityManagerFactory getEMF() {
		if(emf == null)
			emf = Persistence.createEntityManagerFactory("default");
		
		return emf;
	}
}
