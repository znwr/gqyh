package com.zcdy.dsc.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zcdy.dsc.common.system.vo.DictModel;
import com.zcdy.dsc.modules.system.entity.SysDict;
import com.zcdy.dsc.modules.system.entity.SysDictItem;
import com.zcdy.dsc.modules.system.model.TreeSelectModel;
import com.zcdy.dsc.modules.system.vo.DictDropdown;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 字典表 服务类
 * </p>
 *@author : songguang.jiao
 
 * @since 2018-12-28
 */
public interface ISysDictService extends IService<SysDict> {

    public List<DictModel> queryDictItemsByCode(String code);

    List<DictModel> queryTableDictItemsByCode(String table, String text, String code);

	/**
	 * SQL注入校验（查询条件SQL 特殊check，此方法仅供此处使用）
	 * @param table
	 * @param text
	 * @param code
	 * @param filterSql
	 * @return
	 */
	public List<DictModel> queryTableDictItemsByCodeAndFilter(String table, String text, String code, String filterSql);

	/**
	 * 获取字典数据
	 * @param code
	 * @param key
	 * @return
	 */
    public String queryDictTextByKey(String code, String key);

	/**
	 * 翻译字典文本
	 * @param table
	 * @param text
	 * @param code
	 * @param key
	 * @return
	 */
	String queryTableDictTextByKey(String table, String text, String code, String key);

	/**
	 * 根据字典code加载字典text 返回
	 * @param table
	 * @param text
	 * @param code
	 * @param keyArray
	 * @return
	 */
	List<String> queryTableDictByKeys(String table, String text, String code, String[] keyArray);

    /**
     * 根据字典类型删除关联表中其对应的数据
     *
     * @param sysDict
     * @return
     */
    boolean deleteByDictId(SysDict sysDict);

	/**
	 * 添加一对多
	 * @param sysDict
	 * @param sysDictItemList
	 */
    public void saveMain(SysDict sysDict, List<SysDictItem> sysDictItemList);
    
    /**
	 * 查询所有部门 作为字典信息 id -->value,departName -->text
	 * @return
	 */
	public List<DictModel> queryAllDepartBackDictModel();
	
	/**
	 * 查询所有用户  作为字典信息 username -->value,realname -->text
	 * @return
	 */
	public List<DictModel> queryAllUserBackDictModel();
	
	/**
	 * 通过关键字查询字典表
	 * @param table
	 * @param text
	 * @param code
	 * @param keyword
	 * @return
	 */
	public List<DictModel> queryTableDictItems(String table, String text, String code,String keyword);
	
	/**
	  * 根据表名、显示字段名、存储字段名 查询树
	 * @param table
	 * @param text
	 * @param code
	 * @param pidField
	 * @param pid
	 * @param hasChildField
	 * @return
	 */
	List<TreeSelectModel> queryTreeList(Map<String, String> query,String table, String text, String code, String pidField,String pid,String hasChildField);

	/**
	 * 真实删除
	 * @param id
	 */
	public void deleteOneDictPhysically(String id);

	/**
	 * 修改delFlag
	 * @param delFlag
	 * @param id
	 */
	public void updateDictDelFlag(int delFlag,String id);

	/**
	 * 查询被逻辑删除的数据
	 * @return
	 */
	public List<SysDict> queryDeleteList();

	/**
	 * 查询基础数据字典key-value
	 * @param code
	 * @return
	 */
	public List<DictDropdown> getDictValue(String code);

	/**
	 * app根据code查询基础数据字典key-value
	 * @param code 字典编号
	 * @return
	 */
	public List<DictDropdown> getAppDictValue(String code);

}
