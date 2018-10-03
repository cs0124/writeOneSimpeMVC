package com.itcast.demo.service.impl;

import com.itcast.demo.service.IDemoService;
import com.itcast.mvcframework.anotation.GPService;

@GPService
public class DemoService implements IDemoService{

	public String get(String name) {
		return "my name is " + name;
	}

}
