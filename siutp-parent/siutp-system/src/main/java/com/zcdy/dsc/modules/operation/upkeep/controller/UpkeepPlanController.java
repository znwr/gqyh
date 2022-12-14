package com.zcdy.dsc.modules.operation.upkeep.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zcdy.dsc.common.api.vo.Result;
import com.zcdy.dsc.common.aspect.annotation.AutoLog;
import com.zcdy.dsc.common.aspect.log.LogEnum.Operation;
import com.zcdy.dsc.common.aspect.log.LogEnum.Type;
import com.zcdy.dsc.common.system.base.controller.AbstractPrincipalController;
import com.zcdy.dsc.common.system.vo.LoginUser;
import com.zcdy.dsc.constant.ModuleConstant;
import com.zcdy.dsc.modules.configcentre.constant.SystemParamConstant;
import com.zcdy.dsc.modules.configcentre.entity.SystemConfig;
import com.zcdy.dsc.modules.configcentre.service.SystemParamService;
import com.zcdy.dsc.modules.datacenter.statistic.entity.SmsUsers;
import com.zcdy.dsc.modules.datacenter.statistic.service.SmsUsersService;
import com.zcdy.dsc.modules.operation.equipment.service.IOptEquipmentService;
import com.zcdy.dsc.modules.operation.equipment.vo.EquipmentDropdown;
import com.zcdy.dsc.modules.operation.upkeep.entity.UpkeepPlan;
import com.zcdy.dsc.modules.operation.upkeep.entity.UpkeepPlanRecord;
import com.zcdy.dsc.modules.operation.upkeep.service.UpkeepPlanRecordService;
import com.zcdy.dsc.modules.operation.upkeep.service.UpkeepPlanService;
import com.zcdy.dsc.modules.operation.upkeep.vo.KeepAdvisePageParam;
import com.zcdy.dsc.modules.operation.upkeep.vo.KeepAdviseVo;
import com.zcdy.dsc.modules.operation.upkeep.vo.KeepPlanParam;
import com.zcdy.dsc.modules.operation.work.constant.DispathchStatusConstant;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ????????????????????????
 *
 * @author Roberto
 * @date 2020/06/04
 */
@Api(tags = "????????????????????????")
@RestController
@RequestMapping("/opt/upkeep/plan")
public class UpkeepPlanController implements AbstractPrincipalController {

    @Resource
    private UpkeepPlanService upkeepPlanService;

    @Resource
    private SystemParamService systemParamService;

    @Resource
    private SmsUsersService smsUsersService;

    @Resource
    private UpkeepPlanRecordService upkeepPlanRecordService;

    @Resource
    private IOptEquipmentService equipmentService;

    /**
     * ??????????????????
     *
     * @param param
     * @return
     */
    @AutoLog(value = "??????????????????", group = ModuleConstant.OPERATION, operation = Operation.ADD, type = Type.OPERATION)
    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @PostMapping(value = "/createPlan")
    public Result<Object> createPlan(@RequestBody KeepPlanParam param) {
        if (param.getPlanDate().compareTo(new Date()) < 1) {
            return Result.error("????????????????????????????????????");
        }
        QueryWrapper<UpkeepPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UpkeepPlan::getOptId, param.getOptId());
        UpkeepPlan one = this.upkeepPlanService.getOne(queryWrapper);
        //???????????????????????????record??????
        if (null != one) {
            one.setPrevPlanDate(one.getNextPlanDate());
            one.setNextPlanDate(param.getPlanDate());

            UpkeepPlanRecord record = new UpkeepPlanRecord();
            record.setPlanDate(param.getPlanDate());
            record.setPlanId(one.getId());

            this.upkeepPlanService.updatePlan(one, record);
        } else {
            one = new UpkeepPlan();
            one.setOptId(param.getOptId());
            one.setUpkeepTimes(0);
            one.setNextPlanDate(param.getPlanDate());

            UpkeepPlanRecord record = new UpkeepPlanRecord();
            record.setPlanDate(param.getPlanDate());
            this.upkeepPlanService.addUpkeepPlan(one, record);
        }
        return Result.ok(one);
    }

    /**
     * ?????????????????????????????????
     *
     * @param days
     * @param usersId
     * @return
     */
    @AutoLog(value = "?????????????????????????????????")
    @ApiOperation(value = "?????????????????????????????????")
    @GetMapping("/change/userAndTime")
    public Result<Object> changeUserAndTime(@RequestParam(name = "days") @NotBlank(message = "??????????????????") String days, String usersId) {
        upkeepPlanService.changeUserAndTime(days, usersId);
        return Result.ok();
    }


    /**
     * ?????????????????????????????????
     *
     * @return
     */
    @AutoLog("?????????????????????????????????")
    @ApiOperation("?????????????????????????????????")
    @GetMapping("getUserAndTime")
    public Result<JSONObject> getUserAndTime() {
        Result<JSONObject> result = new Result<>();
        JSONObject jsonObject = new JSONObject();
        SystemConfig config = systemParamService.getConfigByKey(SystemParamConstant.UPKEEPER_REMIND_DAYS);
        if (config == null) {
            jsonObject.put("time", "");
            jsonObject.put("userId", "");
            result.setResult(jsonObject);
            return result.success();
        }
        List<SmsUsers> smsUsers = smsUsersService.list(Wrappers.lambdaQuery(new SmsUsers()).eq(SmsUsers::getModuleId, config.getId()));
        List<String> userIds = new ArrayList<>(smsUsers.size());
        smsUsers.forEach(user -> {
            userIds.add(user.getUserId());
        });
        jsonObject.put("time", config.getConfigValue());
        jsonObject.put("userId", userIds);
        result.setResult(jsonObject);
        return result.success();
    }

    /**
     * ???????????? ?????????????????????????????????????????????  ??????????????????????????????????????????
     *
     * @return
     */
    @AutoLog(value = "????????????")
    @ApiOperation(value = "????????????")
    @GetMapping("/adviseData")
    public Result<IPage<KeepAdviseVo>> adviseData(KeepAdvisePageParam param) {
        Page<KeepAdviseVo> page = new Page<>(param.getPageNo(), param.getPageSize());
        SystemConfig config = systemParamService.getConfigByKey(SystemParamConstant.UPKEEPER_REMIND_DAYS);
        if (config != null) {
            //?????????????????????
            List<SmsUsers> smsUsers = smsUsersService.list(Wrappers.lambdaQuery(new SmsUsers()).eq(SmsUsers::getModuleId, config.getId()));
            LoginUser loginUser = getCurrentUser();
            boolean flag = false;
            for (SmsUsers user : smsUsers) {
                if (loginUser.getId().equals(user.getUserId())) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                return Result.ok(upkeepPlanService.advisePageData(page, param));
            }
        }
        return Result.ok(page);
    }



    /**
     * ????????????
     * ?????????????????????  ????????????????????????
     *
     * @param id
     * @return
     */
    @AutoLog("????????????")
    @ApiOperation("????????????")
    @GetMapping("/close/{id}")
    public Result<Object> close(@PathVariable("id") String id) {
        upkeepPlanRecordService.update(new UpkeepPlanRecord().setDispatchStatus(DispathchStatusConstant.CLOSED), new LambdaUpdateWrapper<UpkeepPlanRecord>().eq(UpkeepPlanRecord::getPlanId, id));
        return Result.ok("??????????????????");
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    @AutoLog("?????????????????????")
    @ApiOperation("?????????????????????")
    @GetMapping("/unDispatchEquipment")
    public Result<List<EquipmentDropdown>> unDispatchEquipmentDropDown(String equipmentSn) {
        return Result.ok(equipmentService.unDispatchEquipmentDropDown(equipmentSn)) ;
    }
}