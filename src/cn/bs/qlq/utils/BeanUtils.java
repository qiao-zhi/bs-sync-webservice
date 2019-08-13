package cn.bs.qlq.utils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanUtils {

	/**
	 * 内省进行数据转换-javaBean转map
	 * 
	 * @param obj
	 *            需要转换的bean
	 * @return 转换完成的map
	 * @throws Exception
	 */
	public static <T> Map<String, Object> beanToMap(T obj, boolean putIfNull) throws Exception {
		Map<String, Object> map = new HashMap<>();
		// 获取javaBean的BeanInfo对象
		BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass(), Object.class);
		// 获取属性描述器
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			// 获取属性名
			String key = propertyDescriptor.getName();
			// 获取该属性的值
			Method readMethod = propertyDescriptor.getReadMethod();
			// 通过反射来调用javaBean定义的getName()方法
			Object value = readMethod.invoke(obj);

			if (value == null && !putIfNull) {
				continue;
			}

			map.put(key, value);
		}

		return map;
	}

	public static <T> List<Map<String, Object>> beansToMaps(List<T> objs, boolean putIfNull, boolean addIndex)
			throws Exception {
		List<Map<String, Object>> result = new ArrayList<>();
		Map<String, Object> beanToMap = null;
		int index = 0;
		for (Object obj : objs) {
			beanToMap = beanToMap(obj, putIfNull);
			if (addIndex) {
				beanToMap.put("index", ++index);
			}

			result.add(beanToMap);
		}

		return result;
	}

	/**
	 * Map转bean
	 * 
	 * @param map
	 *            map
	 * @param clz
	 *            被转换的类字节码对象
	 * @return
	 * @throws Exception
	 */
	public static <T> T map2Bean(Map<String, Object> map, Class<T> clz) throws Exception {
		// new 出一个对象
		T obj = clz.newInstance();
		// 获取person类的BeanInfo对象
		BeanInfo beanInfo = Introspector.getBeanInfo(clz, Object.class);
		// 获取属性描述器
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			// 获取属性名
			String key = propertyDescriptor.getName();
			Object value = map.get(key);

			// 通过反射来调用Person的定义的setName()方法
			Method writeMethod = propertyDescriptor.getWriteMethod();
			writeMethod.invoke(obj, value);
		}
		return obj;
	}

}
