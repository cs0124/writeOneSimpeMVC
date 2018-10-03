package com.itcast.mvcframework.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itcast.mvcframework.anotation.GPAutowired;
import com.itcast.mvcframework.anotation.GPController;
import com.itcast.mvcframework.anotation.GPRequestMapping;
import com.itcast.mvcframework.anotation.GPService;

/**
 * 
 * @author chishan
 *
 */
public class GPDispatcherServlet extends HttpServlet{

	private static final long serialVersionUID = 1L;
	private Properties contextConfig = new Properties();
    
	//存放scanPackage包下所有的类
	private List<String> classNames = new ArrayList<String>();
	
	//IOC容器
	private Map<String, Object> ioc = new HashMap<String, Object>();
	
	
	private Map<String, Method> handlerMapping = new HashMap<String, Method>();
 	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			
			doDispath(req,resp);
			
		} catch (Exception e) {
			resp.getWriter().write(Arrays.toString(e.getStackTrace()));
			e.printStackTrace();
		}
	}
	
	private void doDispath(HttpServletRequest req, HttpServletResponse resp) throws Exception {

		//绝对路径
		String uri = req.getRequestURI();

//		String contextPath = req.getContextPath();
		
//		uri = uri.replace(contextPath, "").replaceAll("/+", "/");
		
		if (!handlerMapping.containsKey(uri)) {
			resp.getWriter().write("NOT FOUND");
			return;
		}
		
		System.out.println(handlerMapping.get(uri));
		
		Map<String,String[]> params = req.getParameterMap();
		Method method = handlerMapping.get(uri);
		
		String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
		Object instance = ioc.get(beanName);
		
		method.invoke(instance, new Object[] {req,resp,params.get("name")[0]});
	}

	@Override
	public void init(ServletConfig config) throws ServletException{

		
		
		//1、加载配置文件
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		
		//2、解析配置文件，并且读取信息，完成扫描scanPackage包下的所有类
		
		doScanner(contextConfig.getProperty("scanPackage"));
		
		//3、初始化刚刚扫描的所有类，并且将其放入IOC容器内
		
		doInstance();
		
		//4、完成自动化注入的工作，DI
		
		doAutowired();
		
		//5、初始化HandlerMapping,将URL和METHOD一对一的关联起来
		
		initHandlerMapping(); 
		
		System.out.println("mvc framework is init......");
		
		
	}

	/**
	 * 初始化HandlerMapping,将URL和METHOD一对一的关联起来
	 */
	private void initHandlerMapping() {

		if (ioc.isEmpty()) {return;}

		//遍历ioc容器，得到具体的类
		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			
			Class<?> clazz = entry.getValue().getClass();
			
			if (!clazz.isAnnotationPresent(GPController.class)) {
				continue;
			}
			
			
			String baseUrl = "";
			
			if (clazz.isAnnotationPresent(GPRequestMapping.class)) {
				
				GPRequestMapping requestMapping = clazz.getAnnotation(GPRequestMapping.class);
				
				baseUrl = requestMapping.value();
			}
			
			//得到类里面的所有方法
			Method[] methods = clazz.getMethods();
			
			for (Method method : methods) {
				
				if (!method.isAnnotationPresent(GPRequestMapping.class)) {
					continue;
				}
				
				GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);
				
				String url = (baseUrl + requestMapping.value().replaceAll("/+", "/"));
				
				handlerMapping.put(url, method);
				
				System.out.println("Mapped : " + url + "," + method);
			}
			
		}
		
	}

	/**
	 * 自动注入
	 */
	private void doAutowired() {
		
		if (ioc.isEmpty()) {return;}
		
		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			
			//拿到类里面的所有字段
			Field [] fileds = entry.getValue().getClass().getDeclaredFields();
			
			for (Field field : fileds) {
				
				//没有加GPAutowired注解，不自动赋值
				if (!field.isAnnotationPresent(GPAutowired.class)) {continue;}
				
				GPAutowired autowired = field.getAnnotation(GPAutowired.class);
				
				String beanName = autowired.value().trim();
				
				if ("".equals(beanName)) {
					beanName =  field.getType().getName(); 
				}
				
				field.setAccessible(true);//强制赋值
				
				try {
					
					field.set(entry.getValue(), ioc.get(beanName));
					
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				
			}  
		}
		
	}

	
	/**
	 * 初始化刚刚扫描的类，并加入到ioc容器内
	 */
	
	private void doInstance() {
		
		if (classNames.isEmpty()) {return;}
		
		//不是所有的类都初始化，只有带注解的类才初始化
		try {
			
			for (String className:classNames) {
				
				Class<?> clazz = Class.forName(className);
				
				//判断类上是否有GPController注解
				if (clazz.isAnnotationPresent(GPController.class)) {
					
					//默认类名首字母小写作为key
					String beanName = lowerFirstCase(clazz.getSimpleName());
					
					if (ioc.containsKey(beanName)) {
						throw new Exception("该名字已被定义过了！");
					}
					
					//放到ioc容器内
					ioc.put(beanName, clazz.newInstance());
					
				}
				//判断类上是否有GPService注解
				else if (clazz.isAnnotationPresent(GPService.class)) {
					
					//拿到GPService注解
					GPService gpService = clazz.getAnnotation(GPService.class);
					
					String beanName = gpService.value();
					
					if ("".equals(beanName.trim())) {
						
						beanName = lowerFirstCase(clazz.getSimpleName());
						
					}

					Object instance = clazz.newInstance();
					
					if (ioc.containsKey(beanName)) {
						throw new Exception("该名字已被定义过了！");
					}
					
					ioc.put(beanName, instance);
					
					//得到这个实现类的所有父接口，一并加入到ioc容器里
					Class<?> [] interfaces = clazz.getInterfaces();
					for (Class<?> i :interfaces) {
						
						if (ioc.containsKey(i.getName())) {
							throw new Exception("该名字已被定义过了！");
						}
						ioc.put(i.getName(), instance);
					}
					
				}
				else {
					continue;
				}
				
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * 把类名首字母变小写
	 * @param simpleName
	 * @return
	 */
	private String lowerFirstCase(String simpleName) {
		
		char[] chars = simpleName.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
		
	}

	
	/**
	 * 解析配置文件，并且完成扫描scanPackage包下的所有类
	 * @param scanPackage
	 */
	private void doScanner(String scanPackage) {
		
		//拿到的这个包名实际上只是一个字符串，转换成类路径
		URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.", "/"));
		
		//获取到这个路径下的所有文件
		File classDir = new File(url.getFile());
		
		for (File file : classDir.listFiles()) {
			//如果是一个文件夹
			if (file.isDirectory()) {
				doScanner(scanPackage + "." + file.getName());
			} else {
				//是class,把这个class加载到内存中
				String className = (scanPackage + "." + file.getName().replace(".class", ""));
				
				classNames.add(className);
			}
		}
	}

	
	/**
	 * 加载配置文件
	 * @param contextConfigLocation
	 */
	private void doLoadConfig(String contextConfigLocation) {
		
		//在classpath下找到配置文件
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		
		try {
			contextConfig.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
}
