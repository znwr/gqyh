package com.zcdy.dsc.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zcdy.dsc.modules.system.entity.SysPermissionDataRule;

import java.util.List;
/**
 * @author： admin
 * 创建时间：2019年12月24日 下午4:53:23
 * 描述: <p>权限service</p>
 */
public interface ISysPermissionDataRuleService extends IService<SysPermissionDataRule> {

	/**
	 * 根据菜单id查询其对应的权限数据
	 * 
	 * @param permRule
	 */
	List<SysPermissionDataRule> getPermRuleListByPermId(String permissionId);

	/**
	 * 根据页面传递的参数查询菜单权限数据
	 * 
	 * @return
	 */
	List<SysPermissionDataRule> queryPermissionRule(SysPermissionDataRule permRule);
	
	
	/**
	  * 根据菜单ID和用户名查找数据权限配置信息
	 * @param permission
	 * @param username
	 * @return
	 */
	List<SysPermissionDataRule> queryPermissionDataRules(String username,String permissionId);
	
	/**
	 * 新增菜单权限配置 修改菜单rule_flag
	 * @param sysPermissionDataRule
	 */
	public void savePermissionDataRule(SysPermissionDataRule sysPermissionDataRule);
	
	/**
	 * 删除菜单权限配置 判断菜单还有无权限
	 * @param dataRuleId
	 */
	public void deletePermissionDataRule(String dataRuleId);
	
	
}
