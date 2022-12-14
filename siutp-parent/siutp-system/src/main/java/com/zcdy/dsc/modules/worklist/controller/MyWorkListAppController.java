package com.zcdy.dsc.modules.worklist.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zcdy.dsc.common.api.vo.Result;
import com.zcdy.dsc.common.api.vo.ResultData;
import com.zcdy.dsc.common.aspect.annotation.AutoLog;
import com.zcdy.dsc.common.framework.influxdb.InfluxService;
import com.zcdy.dsc.common.framework.influxdb.entity.WorkListLocationInflux;
import com.zcdy.dsc.common.framework.redis.RedisService;
import com.zcdy.dsc.common.framework.swagger.ApiVersion;
import com.zcdy.dsc.common.framework.swagger.VersionConstant;
import com.zcdy.dsc.common.system.base.controller.BaseController;
import com.zcdy.dsc.common.system.util.JwtUtil;
import com.zcdy.dsc.common.system.vo.DictModel;
import com.zcdy.dsc.common.util.ConvertUtils;
import com.zcdy.dsc.common.util.DateUtil;
import com.zcdy.dsc.constant.RedisKeyConstantV2;
import com.zcdy.dsc.modules.operation.equipment.constant.DelFlagConstant;
import com.zcdy.dsc.modules.system.service.ISysDictService;
import com.zcdy.dsc.modules.system.vo.DictDropdown;
import com.zcdy.dsc.modules.worklist.constant.WorkListConstant;
import com.zcdy.dsc.modules.worklist.entity.WorkList;
import com.zcdy.dsc.modules.worklist.entity.WorkListLocation;
import com.zcdy.dsc.modules.worklist.entity.WorkListMatter;
import com.zcdy.dsc.modules.worklist.param.MyWorkListParam;
import com.zcdy.dsc.modules.worklist.service.MyWorkListService;
import com.zcdy.dsc.modules.worklist.service.WorkListService;
import com.zcdy.dsc.modules.worklist.utils.MapUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description: ????????????App
 * @Author: ????????????
 * @Date:   2021-03-02
 * @Version: V1.0
 */
@Slf4j
@Api(tags="????????????")
@RestController
@RequestMapping("/worklist/myWorkList/app")
public class MyWorkListAppController extends BaseController<WorkList, MyWorkListService> {

    @Autowired
    private MyWorkListService myWorkListService;

    @Autowired
    private WorkListService workListService;

    @Autowired
    private ISysDictService sysDictService;

    @Autowired
    private RedisService redisService;

    @Resource
    private InfluxService influxService;

    /**
     * ??????code??????????????????
     * @param code
     * @return
     */
    private List<DictDropdown> getDictIteamsByCode(String code)
    {
        List<DictModel> dmlist = sysDictService.queryDictItemsByCode(code);
        List<DictDropdown> value = dmlist.stream().map(dictModel -> {
            DictDropdown dd = new DictDropdown();
            dd.setCode(dictModel.getValue());
            dd.setTitle(dictModel.getText());
            return dd;
        }).collect(Collectors.toList());
        return value;
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????
     * @return
     */
    @ApiOperation(value="????????????????????????")
    @GetMapping(value="/dateSearch")
    @ApiVersion(group = VersionConstant.VERSION_APP)
    public Result<List<DictDropdown>> getTypeList() {
        String code = WorkListConstant.WORK_LIST_APP_DATE_SEARCH_LIST;
        Result<List<DictDropdown>> result=new Result<>();
        result.setResult(getDictIteamsByCode(code));
        result.success("????????????");
        return result;
    }

    @AutoLog(value = "????????????-??????????????????")
    @ApiOperation(value="????????????-??????????????????", notes="????????????-??????????????????")
    @GetMapping(value = "/mylistnum")
    @ApiVersion(group = VersionConstant.VERSION_APP)
    public ResultData<?> getMylistNum(HttpServletRequest req) {
        MyWorkListParam myWorkListParam = new MyWorkListParam();
        myWorkListParam.setSearchStatus(WorkListConstant.MY_WORK_LIST_STATUS_DOING);
        ResultData<?> resultData = queryPageListInsp(myWorkListParam, req);
        IPage<WorkList> data = (IPage<WorkList>) resultData.getData();
        return ResultData.ok(data.getTotal());
    }

    @AutoLog(value = "????????????-??????????????????")
    @ApiOperation(value="????????????-??????????????????", notes="????????????-??????????????????")
    @GetMapping(value = "/list")
    @ApiVersion(group = VersionConstant.VERSION_APP)
    public ResultData<?> queryPageListInsp(MyWorkListParam workListParam,
                                           HttpServletRequest req) {
        WorkList searchParam = new WorkList(workListParam);
        searchParam.setDelFlag(DelFlagConstant.NORMAL);//????????????=0
        String username = JwtUtil.getUserNameByToken(req);
        searchParam.setTeamMemberUsername(username);
        searchParam.setQueryStatusCode(WorkListConstant.WORK_LIST_STATUS_CODE);
        searchParam.setQueryTypeCode(WorkListConstant.WORK_LIST_TYPE_CODE);
        if(workListParam.getSearchDate() != null)//??????????????????
        {
            String searchDate = workListParam.getSearchDate();
            Date today = new Date();
            if(WorkListConstant.WORK_LIST_APP_DATE_SEARCH_LIST_WEEK.equals(searchDate))
            {
                //??????
                Date monday = DateUtil.getDayOfWeek(today, 1);
                Date sunday = DateUtil.addDay(monday, 6);
                searchParam.setStartDate(monday);//??????
                searchParam.setOverDate(sunday);//??????
            }
            else if(WorkListConstant.WORK_LIST_APP_DATE_SEARCH_LIST_MONTH.equals(searchDate))
            {
                //??????
                Date firstDay = DateUtil.getDayOfMonth(today, 1);//???????????????
                Date lastDay = DateUtil.getLastDayOfMonth(today);//??????????????????
                searchParam.setStartDate(firstDay);
                searchParam.setOverDate(lastDay);
            }
            else if(WorkListConstant.WORK_LIST_APP_DATE_SEARCH_LIST_YEAR.equals(searchDate))
            {
                //??????
                Date firstDay = DateUtil.getFirstDayOfYear(today);//???????????????
                Date lastDay = DateUtil.getLastDayOfYear(today);//??????????????????
                searchParam.setStartDate(firstDay);
                searchParam.setOverDate(lastDay);
            }
            else if(WorkListConstant.WORK_LIST_APP_DATE_SEARCH_LIST_ALL.equals(searchDate))
            {
                //??????
                searchParam.setStartDate(null);
                searchParam.setOverDate(null);
            }
            else
            {
                return ResultData.fail("??????????????????");
            }
        }
        Page<WorkList> page = new Page<WorkList>(workListParam.getPageNo(), workListParam.getPageSize());
        IPage<WorkList> pageList = myWorkListService.selectPageDate(page, searchParam);
        return ResultData.ok(pageList);
    }

    /**
     * ??????id??????
     * @param id
     * @return
     */
    @AutoLog(value = "??????-??????id??????")
    @ApiOperation(value="??????-??????id??????", notes="??????-??????id??????")
    @GetMapping(value = "/query")
    @ApiVersion(group = VersionConstant.VERSION_APP)
    public ResultData<?> queryById(@RequestParam("id") String id) {
        ResultData<WorkList> resultData = new ResultData<WorkList>();
        try {
            if (ConvertUtils.isEmpty(id)) {
                resultData.error500("id????????????");
                return resultData;
            }
            WorkList workList = new WorkList();
            workList.setId(id);
            workList.setDelFlag(DelFlagConstant.NORMAL);
            workList.setQueryStatusCode(WorkListConstant.WORK_LIST_STATUS_CODE);
            workList.setQueryTypeCode(WorkListConstant.WORK_LIST_TYPE_CODE);
            workList.setQueryMatterTypeCode(WorkListConstant.WORK_LIST_MATTER_TYPE);
            workList.setQueryMatterStatusCode(WorkListConstant.WORK_LIST_MATTER_STATUS);
            workList.setQueryMatterLevelCode(WorkListConstant.WORK_LIST_MATTER__MATTER_LEVEL_CODE);
            workList.setQueryMatterMatterTypeCode(WorkListConstant.WORK_LIST_MATTER__MATTER_TYPE_CODE);
            workList = workListService.getOneById(workList);
            resultData.setData(workList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            resultData.error500("????????????");
        }
        return resultData;
    }

    @AutoLog(value = "????????????-????????????")
    @ApiOperation(value="????????????-????????????", notes="????????????-????????????")
    @PostMapping(value = "/completeOne")
    @ApiVersion(group = VersionConstant.VERSION_APP)
    public ResultData<?> completeOne(@RequestBody WorkListMatter matter, HttpServletRequest req) {
        String username = JwtUtil.getUserNameByToken(req);
        //?????????????????????????????????????????????
        Map<String, Double> map = MapUtil.TencentMap2baiduMap(matter.getSolveLongitudeTencent(), matter.getSolveLatitudeTencent());
        matter.setSolveLongitude(String.valueOf(map.get("x")));
        matter.setSolveLatitude(String.valueOf(map.get("y")));
        return myWorkListService.completeOne(matter, username);
    }

    @AutoLog(value = "app???????????????????????????")
    @ApiOperation(value="app???????????????????????????", notes="app???????????????????????????")
    @PostMapping(value = "/subLocation")
    @ApiVersion(group = VersionConstant.VERSION_APP)
    public ResultData<?> subLocation(@RequestBody WorkListLocation workListLocation, HttpServletRequest req)
    {
        if(workListLocation == null)
        {
            return ResultData.fail("????????????????????????");
        }
        if(workListLocation.getLatitude() == null || workListLocation.getLongitude() == null)
        {
            return ResultData.fail("?????????????????????");
        }
        String username = JwtUtil.getUserNameByToken(req);
        String locationKey = RedisKeyConstantV2.WORKLIST_APP_LOCATION;
        locationKey = String.format(locationKey, username);
        workListLocation.setDateTime(new Date());
        //?????????????????????????????????????????????
        Map<String, Double> map = MapUtil.TencentMap2baiduMap(workListLocation.getLongitude(), workListLocation.getLatitude());
        workListLocation.setLongitude(String.valueOf(map.get("x")));
        workListLocation.setLatitude(String.valueOf(map.get("y")));
        //??????redis???redis??????????????????
        redisService.set(locationKey, JSON.toJSONString(workListLocation));

        //??????influxdb
        WorkListLocationInflux workListLocationInflux = WorkListLocationInflux.LOCATION_DATA.clone();
        workListLocationInflux.setUserName(username);
        workListLocationInflux.setTime(System.currentTimeMillis());
        workListLocationInflux.setLon(workListLocation.getLongitude());
        workListLocationInflux.setLat(workListLocation.getLatitude());
        List<WorkListLocationInflux> list = new ArrayList<>();
        list.add(workListLocationInflux);
        influxService.writeBatchWithTime(list, WorkListLocationInflux.class);
        return ResultData.ok();
    }
}
