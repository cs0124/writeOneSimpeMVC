package com.itcast.demo.mvc_action;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itcast.demo.service.IDemoService;
import com.itcast.mvcframework.anotation.GPAutowired;
import com.itcast.mvcframework.anotation.GPController;
import com.itcast.mvcframework.anotation.GPRequestMapping;
import com.itcast.mvcframework.anotation.GPRequestParam;

@GPController
@GPRequestMapping("/demo")
public class DemoAction {
	
	@GPAutowired
	private IDemoService demoService;
	
	@GPRequestMapping("/query.json")
	public void query (HttpServletRequest request,HttpServletResponse response,
			@GPRequestParam("name") String name){
		
		String result = demoService.get(name);
		
		try {
			
			response.getWriter().write(result);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@GPRequestMapping("/add.json")
	public void add (HttpServletRequest request,HttpServletResponse response,
			@GPRequestParam ("a") Integer a,@GPRequestParam ("b") Integer b){
		
		try {
			
			response.getWriter().write(a + "+" + b + "=" + (a+b));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@GPRequestMapping("/remove.json")
	public void remove (HttpServletRequest request,HttpServletResponse response,
			@GPRequestParam ("id") Integer id){
		
	}

}
