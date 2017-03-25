package com.java2s.common;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import stuff.Database;

import java.io.Serializable;

@ManagedBean
@SessionScoped
public class HelloBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private Database db;

	public HelloBean()
	{
		db = new Database();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getConnected()
	{
		db.connect();
		if(db.isConnected())
		{
			return "connected";
		}
		return "not connected";
	}

}