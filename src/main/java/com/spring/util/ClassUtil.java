package com.spring.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 加载类
 * @author cjm
 *
 */
public class ClassUtil {

	/**
	 * 获取类加载器
	 * @return
	 */
	public static ClassLoader getClassLoader(){
		return Thread.currentThread().getContextClassLoader();
	} 
	/**
	 * 加载类
	 * 需要提供类名是否初始化标志
	 * 初始化是指是否执行静态代码块
	 * @param className
	 * @param isInit
	 * @return
	 */
	public static Class<?> loadClass(String className,boolean isInit){
		Class<?> cls;
		try {
			cls = Class.forName(className, isInit, getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException();
		}
		return cls;
	}
	/**
	 * 加载类(默认将初始化类)
	 * @param className
	 * @return
	 */
	public static Class<?> loadClass(String className){
		return loadClass(className,true);
	}
	public static Set<Class<?>> getClassSet(String packageName) throws IOException{
		Set<Class<?>> classSet = new HashSet<>();
		try {
			Enumeration<URL> urls = getClassLoader().getResources(packageName.replace(".", "/"));
			//Enumeration相当于老板迭代器
			while(urls.hasMoreElements()){
				URL url = urls.nextElement();
				if(url!=null){
					String protocol = url.getProtocol();//获得URL的协议
					System.out.println("protocol:"+protocol);
					if(protocol.equals("file")){
						//转码
			            String packagePath = URLDecoder.decode(url.getFile(), "UTF-8");//转码为utf-8的格式
			            System.out.println("packagePath:"+packagePath+","+packageName);
			            addClass(classSet, packagePath, packageName);
					}else if(protocol.equals("jar")){
						JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();//解析Jar文件
						if (jarURLConnection != null) {
							JarFile jarFile = jarURLConnection.getJarFile();
							if (jarFile != null) {
								Enumeration<JarEntry> jarEntries = jarFile.entries();
								while (jarEntries.hasMoreElements()) {
									JarEntry jarEntry = jarEntries.nextElement();
									String jarEntryName = jarEntry.getName();
									if (jarEntryName.endsWith(".class")) {
										String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
			                            doAddClass(classSet, className);
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			throw e;
		}
		return classSet;
	}
	private static void addClass(Set<Class<?>> classSet,String packagePath,String packageName){
		//listFiles是获取该目录下所有文件和目录的绝对路径  
		//FileFilter：过滤文件名
		File[] files = new File(packagePath).listFiles(new FileFilter(){
			@Override
			public boolean accept(File file) {
				//获得的是标准文件且文件名称以class结尾 或者 是一个文件目录
				return (file.isFile()&&file.getName().endsWith("class")||file.isDirectory());
			}
		});
		for(File file:files){
			String fileName = file.getName();
			System.out.println("fileName:"+fileName);
			if(file.isFile()){
				String className = fileName.substring(0, fileName.lastIndexOf("."));
				System.out.println("className0:"+className);
				if(StringUtils.isNotBlank(packageName)){
					className = packageName+"."+className;
				}
				System.out.println("className1:"+className);
				//添加 class
				doAddClass(classSet,className);
			}else{
				//子目录
				String subPackagePath = fileName;
				if(StringUtils.isNotBlank(packagePath)){
					subPackagePath = packagePath + "/" + subPackagePath;
				}
				String subPackageName = fileName;
				if(StringUtils.isNotBlank(packageName)){
                    subPackageName = packageName + "." + subPackageName;
                }
				addClass(classSet,subPackagePath,subPackageName);
			}
		}
	}
	private static void doAddClass(Set<Class<?>> classSet,String className){
		Class<?> cls = loadClass(className,false);
		classSet.add(cls);
	}
}
