package com.zcdy.dsc.modules.collection.gis.controller;

import com.alibaba.fastjson.JSON;
import com.zcdy.dsc.common.api.vo.Result;
import com.zcdy.dsc.common.framework.kafka.entity.ValueEntity;
import com.zcdy.dsc.common.framework.redis.RedisOperation;
import com.zcdy.dsc.common.framework.swagger.ApiVersion;
import com.zcdy.dsc.common.framework.swagger.VersionConstant;
import com.zcdy.dsc.common.system.base.controller.AbstractBaseController;
import com.zcdy.dsc.common.util.StringUtil;
import com.zcdy.dsc.constant.RedisKeyConstantV2;
import com.zcdy.dsc.constant.VariableTypeConstant;
import com.zcdy.dsc.modules.collection.gis.entity.IotShowData;
import com.zcdy.dsc.modules.collection.gis.service.IGisModelService;
import com.zcdy.dsc.modules.collection.gis.vo.*;
import com.zcdy.dsc.modules.collection.gis.vo.GISModelMessage.MessageBody;
import com.zcdy.dsc.modules.collection.iot.constant.ModelStatusConstant;
import com.zcdy.dsc.modules.collection.iot.entity.Iot2GisEntity;
import com.zcdy.dsc.modules.collection.iot.entity.ModelStatusEntity;
import com.zcdy.dsc.modules.collection.iot.entity.VariableInfo;
import com.zcdy.dsc.modules.collection.iot.service.IOTModelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiOperationSort;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import javax.annotation.Resource;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * ??????gis??????????????????
 *
 * @author Roberto
 * @date 2020/05/12
 */
@Api(tags = "gis????????????iot??????", value = "GisIotData")
@RestController
@RequestMapping("/gis/iot")
public class GisIotDataController extends AbstractBaseController {

    @Resource
    private IGisModelService iGisModelService;

    @Resource
    private IOTModelService iotModelService;

    @Resource(name = "stringRedisOperation")
    private RedisOperation<String> stringRedisOperation;

    @ApiOperation(value = "???????????????????????????????????????")
    @ApiOperationSort(value = 2)
    @ApiVersion(group = VersionConstant.VERSION_APP)
    @GetMapping("/iots/state")
    public Result<List<MessageBody>> getAllChangedStatusData() {
        // ??????????????????????????????
        List<Iot2GisEntity> iot2Gis = this.iotModelService.getIot2Gis();

        List<MessageBody> messages = new ArrayList<>(16);
        // ?????????????????????
        for (Iot2GisEntity model : iot2Gis) {
            // ???????????????????????????????????????(???????????????????????????)
            String statusConstant = getLastestStatus(model.getIotId());
            if (null == statusConstant) {
                continue;
            }

            ModelStatusEntity statusEntity = JSON.parseObject(statusConstant, ModelStatusEntity.class);

            // ?????????????????????
            String prevStatusConstant = getPrevstatus(model.getIotId());
            if (null != prevStatusConstant) {
                // ???????????????
                ModelStatusEntity prevStatusEntity = JSON.parseObject(prevStatusConstant, ModelStatusEntity.class);
                // ??????????????????????????????
                if (statusEntity.getStatus().equals(ModelStatusConstant.STATUS_NORMAL)
                        && statusEntity.getDetailStatus().equals(ModelStatusConstant.STATUS_NORMAL_OPEN)
                        && prevStatusEntity.getStatus().equals(statusEntity.getStatus())
                        && prevStatusEntity.getDetailStatus().equals(statusEntity.getDetailStatus())) {
                    changePrevStatus(model.getIotId(), statusConstant);
                    continue;
                }
            }
            changePrevStatus(model.getIotId(), statusConstant);
            OperationSatus perationStatus = fillOperation(statusEntity);
            MessageBody body = new MessageBody();
            body.setId(model.getGisId());
            body.setOpration(perationStatus);
            messages.add(body);
        }
        return Result.success(messages, "Operation successfully!");
    }

    /**
     * ????????????????????????
     *
     * @param typeCode
     * @return
     * @author songguang.jiao
     * @date 2020/05/20 12:00:08
     */
    @ApiOperation(value = "??????????????????????????????????????????????????????",notes = "??????????????????????????????????????????????????????")
    @GetMapping("/list/{typeCode}")
    @ApiVersion(group = VersionConstant.VERSION_APP)
    public Result<List<IotEquipInfoVo>> getEquipInfoByModelType(@PathVariable("typeCode") String typeCode) {
        Result<List<IotEquipInfoVo>> result = new Result<>();
        List<IotEquipInfoVo> data = iotModelService.getEquipInfoByModelType(typeCode);
        List<IotEquipInfoVo> ret = new ArrayList<>();
        if(data != null)
        {
            for (IotEquipInfoVo iotEquipInfoVo : data) {
                if(StringUtils.isNotEmpty(iotEquipInfoVo.getEquipmentName()))
                {
                    IotShowData iotShowData = this.getIotDataById(iotEquipInfoVo.getGisId());
                    iotEquipInfoVo.setShowData(StringUtil.isNotNull(iotShowData.getShowData()) ? iotShowData.getShowData() : "??????");
                    ret.add(iotEquipInfoVo);
                }
            }
        }

        result.setResult(ret);
        return result.success();
    }

    /**
     * ??????GisId??????????????????
     *
     * @param gisId
     * @return
     * @author songguang.jiao
     * @date 2020/05/20 13:20:18
     */
    @ApiVersion(group = VersionConstant.VERSION_APP)
    @ApiOperation("??????GisId??????????????????")
    @GetMapping("/getIotData/{gisId}")
    public Result<MessageBody> getIotData(@PathVariable("gisId") String gisId) {
        Result<MessageBody> result = new Result<>();
        MessageBody body = new MessageBody();
        IotShowData iotData = this.getIotDataById(gisId);
        body.setData(iotData.getIotDataVos());
        body.setShowData(iotData.getShowData());
        result.setResult(body);
        return result.success();
    }

    /**
     * ??????gisId??????????????????
     *
     * @param gisId
     * @return
     * @author songguang.jiao
     * @date 2020/05/22 14:33:27
     */
    private IotShowData getIotDataById(String gisId) {
        IotShowData iotShowData = new IotShowData();
        List<VariableInfo> vars = this.iotModelService.getVarsByModelId(gisId);
        List<IotDataVO> data = new ArrayList<>(vars.size());
        if(vars == null)
        {
            return iotShowData;
        }
        //vars.forEach(item -> {
        for(VariableInfo item : vars){
            String messageStr = this.stringRedisOperation.get(String.format(RedisKeyConstantV2.REDISDATAKEY, item.getVarName()));
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
                    // ?????????
                    String iotValue = bg.toEngineeringString();
                    if (!StringUtils.isEmpty(item.getUnited())) {
                        iotValue = iotValue + "[" + item.getUnited() + "]";
                    }
                    dataVO.setVaribleValue(iotValue);
                }
                if(item.getVariableType() != null) {
                    // ????????????????????????APP????????????????????????
                    switch (item.getVariableType()) {
                        case VariableTypeConstant.WH_SLUDGET: //????????????-?????????????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WH_RAINGAUGE: //????????????-???????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WH_LEVEL: //????????????-???????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WH_FLOWV: //????????????-?????????????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_TP: //????????????-????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_TN: //????????????-????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_TEMP: //????????????-????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_SS: //????????????-????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_PH: //????????????-PH??????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_NH4_N: //????????????-????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_EC: //????????????-???????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_DO: //????????????-???????????????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        case VariableTypeConstant.WQ_COD: //????????????-COD??????
                            iotShowData.setShowData(dataVO.getVaribleValue());
                            break;
                        default:
                            break;
                    }
                }
                data.add(dataVO);
            }
        }
        iotShowData.setIotDataVos(data);
        return iotShowData;
    }

    /**
     * @param iotId
     * @return ???????????????
     */
    private String getLastestStatus(String iotId) {
        return stringRedisOperation.get(String.format(RedisKeyConstantV2.MODEL_LAST_STATUS_KEY, iotId));
    }

    /**
     * @param iotId
     * @return ???????????????
     */
    private String getPrevstatus(String iotId) {
        return stringRedisOperation.get(String.format(RedisKeyConstantV2.MODEL_PREV_STATUS_KEY, iotId));
    }

    /**
     * ????????????????????????
     *
     * @param statusEntity
     * @return
     */
    private OperationSatus fillOperation(ModelStatusEntity statusEntity) {
        OperationSatus operationSatus = OperationSatus.OPERATION_SATUS.clone();
        operationSatus.setMessage(statusEntity.getMessage());
        operationSatus.setWorkStatus(statusEntity.getStatus());
        operationSatus.setSwitchSatus(statusEntity.getDetailStatus());
        return operationSatus;
    }

    /**
     * @author???Roberto
     * @create:2020???3???13??? ??????9:35:10 ??????:
     * <p>
     * ???????????????????????????
     * </p>
     */
    private synchronized void changePrevStatus(String iotId, String statusConstant) {
        // ????????????????????????????????????
        stringRedisOperation.set(String.format(RedisKeyConstantV2.MODEL_PREV_STATUS_KEY, iotId), statusConstant);
    }

    /**
     * ???????????????
     * @param typeCode
     * @return
     */
    @ApiVersion(group = VersionConstant.VERSION_APP)
    @ApiOperation("????????????????????????2")
    @GetMapping("/listNew/{typeCode}")
    public Result<List<IotEquipInfoVoNew>> getEquipInfoByModelTypeNew(@PathVariable("typeCode") String typeCode) {
        Result<List<IotEquipInfoVoNew>> result = new Result<>();
        List<IotEquipInfoVo> dataList = iotModelService.getEquipInfoByModelType(typeCode);
        List<IotEquipInfoVoNew> list = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            IotEquipInfoVoNew newEq = new IotEquipInfoVoNew();
            newEq.setGisId(dataList.get(i).getGisId());
            newEq.setEquipmentName(dataList.get(i).getEquipmentName());
            newEq.setShowData(dataList.get(i).getShowData());
            IotShowData iotShowData = this.getIotDataById(dataList.get(i).getGisId());
            List<IotDataVO> iotDataVOList = iotShowData.getIotDataVos();
            String context = "";
            if (iotDataVOList.size() > 0) {
                for (int j = 0; j < iotDataVOList.size(); j++) {
                    IotDataVO iotDataVos = new IotDataVO();
                    iotDataVos = iotDataVOList.get(j);
                    if(iotDataVos !=null && iotDataVos.getVariableName() !=null && iotDataVos.getVariableName().length()>0){
                        context = context + (iotDataVos.getVariableName().concat(":").concat(iotDataVos.getVaribleValue().toString()));
                    }
                }
            }
            newEq.setConText(context);
            list.add(newEq);
        }
        result.setResult(list);
        return result.success();
    }

    @ApiVersion(group = VersionConstant.VERSION_APP)
    @ApiOperation("????????????????????????")
    @GetMapping(value = "/dataNew", produces = "application/json;charset=utf-8")
    public Result<List<GisDataVo>> initAllGisDataNew() {
        Result<List<GisDataVo>> result = new Result<List<GisDataVo>>();
        List<GisModelImgVo> modelsList = iGisModelService.queryListByIn();
        List<GisDataVo> returnGisDataVo = new ArrayList<>();
        // ?????????????????????,????????????????????????
        for (GisModelImgVo gisModelImgVo : modelsList) {
            GisDataVo gisDataVo =new GisDataVo();
            gisDataVo.setTitle(gisModelImgVo.getEquipLocation());
            gisDataVo.setGisId(gisModelImgVo.getId());
            String s=this.map_bd2tx(Double.valueOf(gisModelImgVo.getLatitude()),Double.valueOf(gisModelImgVo.getLongitude()));
            String[] strs = s.split(",");
            for(int i=0;i<strs.length;i++){
                gisDataVo.setLatitude(strs[0].toString());
                gisDataVo.setLongitude(strs[1].toString());
            }
            /**
             * ??????gis???????????????????????????
             */
            IotShowData iotShowData = this.getIotDataById(gisModelImgVo.getId());
            List<IotDataVO> iotDataVOList = iotShowData.getIotDataVos();
            String context = "";
            if (iotDataVOList.size() > 0) {
                for (int j = 0; j < iotDataVOList.size(); j++) {
                    IotDataVO iotDataVos = new IotDataVO();
                    iotDataVos = iotDataVOList.get(j);
                    context = context + (iotDataVos.getVariableName().concat(":").concat(iotDataVos.getVaribleValue().toString()));
                }
            }
            gisDataVo.setTitle(context);
            gisDataVo.setIotDataVos(iotDataVOList);
           /* gisDataVo.setImgUrl(gisModelImgVo.getModelOnImg().getImgUrl());*/
            /**
             * ????????????????????????????????????????????????gis??????
             */
            returnGisDataVo.add(gisDataVo);
        }

        result.setResult(returnGisDataVo);
        return result.success("ok");
    }
    /**
     * ????????????????????????????????????????????????????????????
     * @param lat  ??????????????????
     * @param lon  ??????????????????
     * @return ?????????????????????,??????
     */
    private String map_bd2tx(double lat, double lon){
        double tx_lat;
        double tx_lon;
        double x_pi=3.14159265358979324;
        double x = lon - 0.0065;
        double y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
        tx_lon = z * Math.cos(theta);
        tx_lat = z * Math.sin(theta);
        return tx_lat+","+tx_lon;
    }
}
