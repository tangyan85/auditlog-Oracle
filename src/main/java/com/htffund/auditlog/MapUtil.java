package com.htffund.auditlog;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class MapUtil {
	private static final String UNDERLINE ="_";
    /**
    *
    * @Title: objectToMap
    * @Description: 将object转换为map，默认不保留空值
    * @param @param obj
    * @return Map<String,Object> 返回类型
    * @throws
    */
   public static Map<String, Object> objectToMap(Object obj) {

       Map<String, Object> map = new HashMap<String, Object>();
       map = objectToMap(obj, false);
       return map;
   }

   public static Map<String, Object> objectToMap(Object obj, boolean keepNullVal) {
       if (obj == null) {
           return null;
       }

       Map<String, Object> map = new HashMap<String, Object>();
       try {
           Field[] declaredFields = obj.getClass().getDeclaredFields();
           for (Field field : declaredFields) {
               field.setAccessible(true);
               if (keepNullVal == true) {
                   map.put(field.getName(), field.get(obj));
               } else {
                   if (field.get(obj) != null && !"".equals(field.get(obj).toString())) {
                       map.put(field.getName(), field.get(obj));
                   }
               }
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
       return map;
   }
   
   
   public static Map<String, Object> convertDbColumnList(Object obj,List<String> toColumnList,String PrimaryKey) throws Exception{
	   List<String> columnList = toColumnList;
	   if(StringUtils.isNotEmpty(PrimaryKey)){
		   columnList.add(PrimaryKey.toLowerCase());
	   }

	  return convertDbColumnList(obj,columnList); 
   }
   
   /*
    * Object obj  mybatis返回回来的对象与数值（roleid：1，rolename:管理员），其中字段是驼峰方式；如role_id对应roleid
    * List<String> toColumnList  存放的是从数据获取的字段，如role_id
    * 返回map结果为role_id：1  role_name:管理员
    */
   public static Map<String, Object> convertDbColumnList(Object obj,List<String> toColumnList) throws Exception{
	   Map<String, Object> dbValueMap=objectToMap(obj);
	   Map<String, Object> resultMap = new HashMap<String, Object>();
	   Map<String, String> relationMap = new HashMap<String, String>();
	   
	   //去除下划线后进行匹配
	   for(String column:toColumnList){
		   relationMap.put(column.replaceAll(UNDERLINE, "").toLowerCase(),column);
	   }
	   
	   String tmpKey="";
	   String resultKey="";
	   for (String key : dbValueMap.keySet()) {
		   tmpKey=key.replaceAll(UNDERLINE, "").toLowerCase();
		   resultKey=relationMap.get(tmpKey);
			if(StringUtils.isNotEmpty(resultKey)){
				resultMap.put(resultKey, dbValueMap.get(key));
			}
		}

	  return resultMap; 
   }
}
