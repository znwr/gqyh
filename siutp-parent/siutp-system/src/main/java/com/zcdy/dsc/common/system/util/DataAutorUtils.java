package com.zcdy.dsc.common.system.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

import com.zcdy.dsc.common.system.vo.SysUserCacheInfo;
import com.zcdy.dsc.common.util.SpringContextUtils;
import com.zcdy.dsc.modules.system.entity.SysPermissionDataRule;

/**
 * 数据权限查询规则容器工具类
 * @author admin
 * @date 2020/05/09
 */
public class DataAutorUtils {
	
	public static final String MENU_DATA_AUTHOR_RULES = "MENU_DATA_AUTHOR_RULES";
	
	public static final String MENU_DATA_AUTHOR_RULE_SQL = "MENU_DATA_AUTHOR_RULE_SQL";
	
	public static final String SYS_USER_INFO = "SYS_USER_INFO";

	/**
	 * 往链接请求里面，传入数据查询条件
	 * 
	 * @param request
	 * @param MENU_DATA_AUTHOR_RULES
	 */
	public static synchronized void installDataSearchConditon(HttpServletRequest request, List<SysPermissionDataRule> dataRules) {
		List<SysPermissionDataRule> list = (List<SysPermissionDataRule>)loadDataSearchConditon();
		// 1.先从request获取MENU_DATA_AUTHOR_RULES，如果存则获取到LIST
		if (list==null) {
			// 2.如果不存在，则new一个list
			list = new ArrayList<SysPermissionDataRule>();
		}
		for (SysPermissionDataRule tsDataRule : dataRules) {
			list.add(tsDataRule);
		}
		// 3.往list里面增量存指
		request.setAttribute(MENU_DATA_AUTHOR_RULES, list); 
	}

	/**
	 * 获取请求对应的数据权限规则
	 * 
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static synchronized List<SysPermissionDataRule> loadDataSearchConditon() {
		return (List<SysPermissionDataRule>) SpringContextUtils.getHttpServletRequest().getAttribute(MENU_DATA_AUTHOR_RULES);
				
	}

	/**
	 * 获取请求对应的数据权限SQL
	 * 
	 * @param request
	 * @return
	 */
	private static synchronized String loadDataSearchConditonSQLString() {
		return (String) SpringContextUtils.getHttpServletRequest().getAttribute(MENU_DATA_AUTHOR_RULE_SQL);
	}

	/**
	 * 往链接请求里面，传入数据查询条件
	 * 
	 * @param request
	 * @param MENU_DATA_AUTHOR_RULE_SQL
	 */
	public static synchronized void installDataSearchConditon(HttpServletRequest request, String sql) {
		String ruleSql = (String)loadDataSearchConditonSQLString();
		if (!StringUtils.hasText(ruleSql)) {
			request.setAttribute(MENU_DATA_AUTHOR_RULE_SQL,sql);
		}
	}
	
	public static synchronized void installUserInfo(HttpServletRequest request, SysUserCacheInfo userinfo) {
		request.setAttribute(SYS_USER_INFO, userinfo);
	}
	
	public static synchronized SysUserCacheInfo loadUserInfo() {
		return (SysUserCacheInfo) SpringContextUtils.getHttpServletRequest().getAttribute(SYS_USER_INFO);
				
	}
}
