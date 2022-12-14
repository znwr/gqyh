package com.zcdy.dsc.modules.message.entity;

import org.jeecgframework.poi.excel.annotation.Excel;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zcdy.dsc.common.aspect.annotation.Dict;
import com.zcdy.dsc.common.system.base.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 描述: 消息模板
 
 
 * 版本号: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_sms_template")
public class SysMessageTemplate extends BaseEntity{
	/**模板CODE*/
	@Excel(name = "模板CODE", width = 15)
	private java.lang.String templateCode;
	/**模板标题*/
	@Excel(name = "模板标题", width = 30)
	private java.lang.String templateName;
	/**模板内容*/
	@Excel(name = "模板内容", width = 50)
	private java.lang.String templateContent;
	/**模板测试json*/
	@Excel(name = "模板测试json", width = 15)
	private java.lang.String templateTestJson;
	/**模板类型*/
	@Excel(name = "模板类型", width = 15,dicCode="msgType")
	@Dict(dicCode = "msgType")
	private java.lang.String templateType;
}
