package com.zcdy.dsc.modules.collection.iot.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zcdy.dsc.common.api.vo.Result;
import com.zcdy.dsc.common.aspect.annotation.AutoLog;
import com.zcdy.dsc.common.framework.kafka.entity.ValueEntity;
import com.zcdy.dsc.common.framework.redis.RedisOperation;
import com.zcdy.dsc.common.framework.swagger.ApiVersion;
import com.zcdy.dsc.common.framework.swagger.VersionConstant;
import com.zcdy.dsc.common.system.base.controller.BaseController;
import com.zcdy.dsc.common.util.ConvertUtils;
import com.zcdy.dsc.constant.RedisKeyConstantV2;
import com.zcdy.dsc.modules.collection.gis.vo.IotDataVO;
import com.zcdy.dsc.modules.collection.iot.entity.GisIotEquipEntity;
import com.zcdy.dsc.modules.collection.iot.entity.IotOperateModeEntityDefault;
import com.zcdy.dsc.modules.collection.iot.entity.IotOperatingMode;
import com.zcdy.dsc.modules.collection.iot.entity.VariableInfo;
import com.zcdy.dsc.modules.collection.iot.service.IOTModelService;
import com.zcdy.dsc.modules.collection.iot.service.IotOperatingModeService;
import com.zcdy.dsc.modules.collection.iot.vo.OperateDataInfo;
import com.zcdy.dsc.modules.collection.iot.vo.OperateEquipInfomation;
import com.zcdy.dsc.modules.collection.iot.vo.OperateEquipInfomation.OperateEquipInfoVo;
import com.zcdy.dsc.modules.collection.iot.vo.OperateEquipPageParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @Description: ??????????????????????????????
 * @Author: ??????????????????????????????
 * @Date: 2020-05-21 15:02:57
 * @Version: V1.0
 */
@Api(tags = "??????????????????????????????")
@RestController
@RequestMapping("/iot/operate")
public class IotOperatingModeController extends BaseController<IotOperatingMode, IotOperatingModeService> {
    @Resource
    private IotOperatingModeService iotOperatingModeService;

    @Resource
    private IOTModelService iotModelService;

    @Resource(name = "stringRedisOperation")
    private RedisOperation<String> stringRedisOperation;

    /**
     * 7????????????id
     */
    private final String IOT_LOCATION_7_ID = "39014c10abb711eab47f0050568c260c";
    /**
     * 2????????????id
     */
    private final String IOT_LOCATION_2_ID = "342a754fabb711eab47f0050568c260c";

    /**
     * 2????????????????????????
     */
    private final String section2 = "/res/file/equipImg/section2.jpg";
    /**
     * 7????????????????????????
     */
    private final String section7 = "/res/file/equipImg/section7.jpg";


    /**
     * ??????????????????
     *
     * @param param
     * @return
     * @author songguang.jiao
     * @date 2020/05/21 16:27:41
     */
    @ApiVersion(group = VersionConstant.VERSION_APP)
    @ApiOperation(value = "??????????????????")
    @GetMapping("/equip")
    public Result<OperateEquipInfomation> equipList(OperateEquipPageParam param) {
        Result<OperateEquipInfomation> result = new Result<>();
        OperateEquipInfomation operateEquipInfomation = new OperateEquipInfomation();
        Page<OperateEquipInfoVo> page = new Page<>(param.getPageNo(), param.getPageSize());
        IPage<OperateEquipInfoVo> data = iotOperatingModeService.getAllEquip(page, param);
        //??????????????????id
        List<Object> equipIdList = iotOperatingModeService.listObjs(Wrappers.lambdaQuery(new IotOperatingMode()).select(IotOperatingMode::getEquimentId));
        operateEquipInfomation.setIds(equipIdList);
        operateEquipInfomation.setOperateEquipInfoVo(data);
        result.setResult(operateEquipInfomation);
        return result.success();
    }

    /**
     * ??????????????????
     *
     * @return
     * @author songguang.jiao
     * @date 2020/05/21 16:40:59
     */
    @ApiVersion(group = VersionConstant.VERSION_APP)
    @ApiOperation(value = "??????????????????")
    @GetMapping("/mode")
    public Result<List<OperateDataInfo>> operateModeList() {
        Result<List<OperateDataInfo>> result = new Result<>();
        List<GisIotEquipEntity> iot2Gis = iotOperatingModeService.getOperateModeEquip();
        List<OperateDataInfo> dataList = new ArrayList<>(iot2Gis.size());
        // ??????????????????????????????????????????????????????
        for (GisIotEquipEntity iot2GisEntity : iot2Gis) {
            OperateDataInfo operateDataInfo = new OperateDataInfo();
            operateDataInfo.setEquipName(iot2GisEntity.getEquipName());
            if (!StringUtils.isEmpty(iot2GisEntity.getEquipImg())) {
                operateDataInfo.setEquipImg(baseStoragePath + iot2GisEntity.getEquipImg());
            }
            List<VariableInfo> vars = this.iotModelService.getVarsByModelId(iot2GisEntity.getGisId());
            List<IotDataVO> data = new ArrayList<>(vars.size());
            vars.forEach(item -> {
                String messageStr =
                        this.stringRedisOperation.get(String.format(RedisKeyConstantV2.REDISDATAKEY, item.getVarName()));
                if (!StringUtils.isEmpty(messageStr)) {
                    ValueEntity value = JSON.parseObject(messageStr, ValueEntity.class);
                    IotDataVO dataVO = new IotDataVO();
                    dataVO.setVariableName(item.getVarTitle());
                    if (null == value.getValue()) {
                        dataVO.setVaribleValue("");
                    } else {
                        BigDecimal bg = new BigDecimal(value.getValue());
                        // ?????????????????????
                        int scale = null == item.getScale() ? 2 : item.getScale().intValue();
                        if (scale >= 0) {
                            bg = bg.setScale(scale, RoundingMode.HALF_UP);
                        }
                        if (!StringUtils.isEmpty(item.getUnited())) {
                            dataVO.setVaribleValue(bg.toEngineeringString() + "[" + item.getUnited() + "]");
                        } else {
                            dataVO.setVaribleValue(bg.toEngineeringString());
                        }
                    }
                    data.add(dataVO);
                }
            });
            operateDataInfo.setDatas(data);
            dataList.add(operateDataInfo);
        }
        result.setResult(dataList);
        return result.success();
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    @AutoLog("???????????????????????????")
    @ApiVersion(group = VersionConstant.VERSION_APP)
    @ApiOperation(value = "???????????????????????????")
    @GetMapping("defaultMode")
    public Result<List<OperateDataInfo>> defaultMode() {
        Result<List<OperateDataInfo>> result = new Result<>();
        List<IotOperateModeEntityDefault> iotDefaults = iotOperatingModeService.getDefaultModeEquip();
        //???????????????????????????????????????????????????
        Map<String, OperateDataInfo> mapRerun = new TreeMap<>();
        for (IotOperateModeEntityDefault iotDefault : iotDefaults) {
            OperateDataInfo operateDataInfo = mapRerun.get(iotDefault.getOperateLocationId());
            //?????????????????????????????????
            if (null == operateDataInfo) {
                operateDataInfo = new OperateDataInfo();
                operateDataInfo.setEquipName(iotDefault.getOperateLocationName());
                //2??? 7??????????????????
                if (!IOT_LOCATION_7_ID.equals(iotDefault.getOperateLocationId()) &&
                        !IOT_LOCATION_2_ID.equals(iotDefault.getOperateLocationId()) &&
                        !StringUtils.isEmpty(iotDefault.getEquipImg())) {
                    operateDataInfo.setEquipImg(baseStoragePath + iotDefault.getEquipImg());
                }
                //2???,7???????????????
                if (IOT_LOCATION_2_ID.equals(iotDefault.getOperateLocationId())) {
                    operateDataInfo.setEquipImg(baseStoragePath + section2);
                }
                if (IOT_LOCATION_7_ID.equals(iotDefault.getOperateLocationId())) {
                    operateDataInfo.setEquipImg(baseStoragePath + section7);
                }
                mapRerun.put(iotDefault.getOperateLocationId(), operateDataInfo);
            }

            //??????????????????????????????????????????????????????
            List<VariableInfo> vars = this.iotModelService.getVarsByModelId(iotDefault.getGisId());
            List<IotDataVO> data = operateDataInfo.getDatas();
            if (null == data || data.size() == 0) {
                data = new ArrayList<>(vars.size());
            }
            List<IotDataVO> finalData = data;
            vars.forEach(item -> {
                String messageStr =
                        this.stringRedisOperation.get(String.format(RedisKeyConstantV2.REDISDATAKEY, item.getVarName()));
                if (!StringUtils.isEmpty(messageStr)) {
                    ValueEntity value = JSON.parseObject(messageStr, ValueEntity.class);
                    IotDataVO dataVO = new IotDataVO();
                    dataVO.setVariableName(iotDefault.getEquimentLocation() + "[" + item.getVarTitle() + "]");
                    if (null == value.getValue()) {
                        dataVO.setVaribleValue("");
                    } else {
                        BigDecimal bg = new BigDecimal(value.getValue());
                        // ?????????????????????
                        int scale = null == item.getScale() ? 2 : item.getScale().intValue();
                        if (scale >= 0) {
                            bg = bg.setScale(scale, RoundingMode.HALF_UP);
                        }
                        if (!StringUtils.isEmpty(item.getUnited())) {
                            dataVO.setVaribleValue(bg.toEngineeringString() + "[" + item.getUnited() + "]");
                        } else {
                            dataVO.setVaribleValue(bg.toEngineeringString());
                        }
                    }
                    finalData.add(dataVO);
                }
            });
            operateDataInfo.setDatas(data);
        }

        List<OperateDataInfo> dataList = new ArrayList<>(mapRerun.values());
        result.setResult(dataList);
        return result.success();
    }


    /**
     * ????????????
     *
     * @return
     * @author songguang.jiao
     * @date 2020/05/21 17:03:50
     */
    @ApiVersion(group = VersionConstant.VERSION_APP)
    @ApiOperation(value = "????????????")
    @GetMapping("/save")
    public Result<Object> save(String ids) {
        if (!StringUtils.isEmpty(ids)) {
            // ??????id??????
            String[] split = ids.split(",");
            List<String> paramIds = Arrays.asList(split);

            // ???????????????????????????????????????
            List<IotOperatingMode> existData = iotOperatingModeService
                    .list(Wrappers.lambdaQuery(new IotOperatingMode()).select(IotOperatingMode::getEquimentId));
            List<String> existIds = new ArrayList<>();
            for (IotOperatingMode iotOperatingMode : existData) {
                existIds.add(iotOperatingMode.getEquimentId());
            }

            // ??????
            List<String> delete = this.getDiff(paramIds, existIds);
            for (String equipmentId : delete) {
                iotOperatingModeService.remove(
                        Wrappers.lambdaQuery(new IotOperatingMode()).eq(IotOperatingMode::getEquimentId, equipmentId));
            }

            // ??????
            List<String> saveIds = this.getDiff(existIds, paramIds);
            List<IotOperatingMode> list = new ArrayList<>();
            for (String equipmentId : saveIds) {
                IotOperatingMode iotOperatingMode = new IotOperatingMode();
                iotOperatingMode.setEquimentId(equipmentId);
                list.add(iotOperatingMode);
            }
            iotOperatingModeService.saveBatch(list);
        } else {
            iotOperatingModeService.remove(new LambdaQueryWrapper<IotOperatingMode>());
        }
        return Result.ok("????????????");
    }

    /**
     * ????????????,???differ???????????????main?????????
     *
     * @param main
     * @param diff
     * @return
     * @author songguang.jiao
     * @date 2020/05/21 17:46:03
     */
    private List<String> getDiff(List<String> main, List<String> diff) {
        Map<String, Integer> map = new HashMap<>(main.size());
        for (String string : main) {
            map.put(string, 1);
        }
        List<String> res = new ArrayList<String>();
        for (String key : diff) {
            if (ConvertUtils.isNotEmpty(key) && !map.containsKey(key)) {
                res.add(key);
            }
        }
        return res;
    }
}