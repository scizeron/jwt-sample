package com.example;

import java.util.List;

import javax.validation.constraints.NotNull;

public class UserInfoContext {
	
	@NotNull
	private String id;
	
	@NotNull
	private List<String> facilities;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getFacilities() {
		return facilities;
	}

	public void setFacilities(List<String> facilities) {
		this.facilities = facilities;
	}
}
