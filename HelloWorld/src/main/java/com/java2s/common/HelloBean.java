package com.java2s.common;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import stuff.Database;
import stuff.Person;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

@ManagedBean
@SessionScoped
public class HelloBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private Database db;

	@PostConstruct
	public void initialise()
	{
		db = new Database();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Person> getList()
	{
		return db.getPeopleList();
	}

	public void addNameToDB()
	{
		db.addName(name);
		/*try {
			FacesContext.getCurrentInstance().getExternalContext().dispatch("/welcome.xhtml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}