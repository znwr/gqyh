package com.zcdy.dsc.modules.system.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zcdy.dsc.common.api.vo.Result;
import com.zcdy.dsc.common.constant.CommonConstant;
import com.zcdy.dsc.common.framework.redis.RedisService;
import com.zcdy.dsc.common.system.api.ISysBaseApi;
import com.zcdy.dsc.common.system.util.JwtUtil;
import com.zcdy.dsc.common.system.vo.LoginUser;
import com.zcdy.dsc.common.util.ConvertUtils;
import com.zcdy.dsc.common.util.PasswordUtil;
import com.zcdy.dsc.modules.system.entity.SysDepart;
import com.zcdy.dsc.modules.system.entity.SysUser;
import com.zcdy.dsc.modules.system.entity.SysUserDepart;
import com.zcdy.dsc.modules.system.entity.SysUserRole;
import com.zcdy.dsc.modules.system.model.DepartIdModel;
import com.zcdy.dsc.modules.system.model.SysUserSysDepartModel;
import com.zcdy.dsc.modules.system.service.ISysDepartService;
import com.zcdy.dsc.modules.system.service.ISysUserDepartService;
import com.zcdy.dsc.modules.system.service.ISysUserRoleService;
import com.zcdy.dsc.modules.system.service.ISysUserService;
import com.zcdy.dsc.modules.system.vo.SysDepartUsersVO;
import com.zcdy.dsc.modules.system.vo.SysUserRoleVO;
import com.zcdy.dsc.modules.system.vo.SysUserVo;
import com.zcdy.dsc.modules.system.vo.UserIdDropdown;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p> ????????? ??????????????? </p>
 *@author : songguang.jiao
 * @since 2018-12-20
 */
@Slf4j
@RestController
@RequestMapping("/sys/user")
@Api(tags = "????????????")
public class SysUserController {
    @Autowired
    private ISysBaseApi sysBaseApi;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysDepartService sysDepartService;

    @Autowired
    private ISysUserRoleService sysUserRoleService;

    @Autowired
    private ISysUserDepartService sysUserDepartService;

    @Autowired
    private ISysUserRoleService userRoleService;

    @Autowired
    private RedisService redisService;

    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Result<IPage<SysUserVo>> queryPageList(SysUser user,
                                                  @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                  @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize, HttpServletRequest req) {
        Result<IPage<SysUserVo>> result = new Result<IPage<SysUserVo>>();
        Page<SysUserVo> page = new Page<SysUserVo>(pageNo, pageSize);
        if (StringUtils.isNotEmpty(user.getUsername())) {
            user.setUsername(user.getUsername().replace("*", ""));
        }
        if (StringUtils.isNotEmpty(user.getRealname())) {
            user.setRealname(user.getRealname().replace("*", ""));
        }
        IPage<SysUserVo> pageList = sysUserService.queryPage(page, user);
        result.setSuccess(true);
        result.setResult(pageList);
        return result;
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @RequiresPermissions("user:add")
    public Result<SysUser> add(@RequestBody JSONObject jsonObject) {
        Result<SysUser> result = new Result<SysUser>();
        String selectedRoles = jsonObject.getString("selectedroles");
        String selectedDeparts = jsonObject.getString("selecteddeparts");
        try {
            SysUser user = JSON.parseObject(jsonObject.toJSONString(), SysUser.class);
            // ??????????????????
            user.setCreateTime(new Date());
            String salt = ConvertUtils.randomGen(8);
            user.setSalt(salt);
            String passwordEncode = PasswordUtil.encrypt(user.getUsername(), user.getPassword(), salt);
            user.setPassword(passwordEncode);
            user.setStatus(1);
            user.setDelFlag("0");
            sysUserService.addUserWithRole(user, selectedRoles);
            sysUserService.addUserWithDepart(user, selectedDeparts);
            result.success("???????????????");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("????????????");
        }
        return result;
    }

    @RequestMapping(value = "/edit", method = RequestMethod.PUT)
    public Result<SysUser> edit(@RequestBody JSONObject jsonObject) {
        Result<SysUser> result = new Result<SysUser>();
        try {
            SysUser sysUser = sysUserService.getById(jsonObject.getString("id"));
            sysBaseApi.addLog("???????????????id??? " + jsonObject.getString("id"), CommonConstant.LOG_TYPE_2, 2);
            if (sysUser == null) {
                result.error500("?????????????????????");
            } else {
                SysUser user = JSON.parseObject(jsonObject.toJSONString(), SysUser.class);
                user.setUpdateTime(new Date());
                user.setPassword(sysUser.getPassword());
                String roles = jsonObject.getString("selectedroles");
                String departs = jsonObject.getString("selecteddeparts");
                sysUserService.editUserWithRole(user, roles);
                sysUserService.editUserWithDepart(user, departs);
                result.success("????????????!");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("????????????");
        }
        return result;
    }

    /**
     * ????????????
     */
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        sysBaseApi.addLog("???????????????id??? " + id, CommonConstant.LOG_TYPE_2, 3);
        this.sysUserService.deleteUser(id);
        return Result.ok("??????????????????");
    }

    /**
     * ??????????????????
     */
    @RequestMapping(value = "/deleteBatch", method = RequestMethod.DELETE)
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        sysBaseApi.addLog("????????????????????? ids??? " + ids, CommonConstant.LOG_TYPE_2, 3);
        this.sysUserService.deleteBatchUsers(ids);
        return Result.ok("????????????????????????");
    }

    /**
     * ??????&????????????
     *
     * @param jsonObject
     * @return
     */
    @RequestMapping(value = "/frozenBatch", method = RequestMethod.PUT)
    public Result<SysUser> frozenBatch(@RequestBody JSONObject jsonObject) {
        Result<SysUser> result = new Result<SysUser>();
        try {
            String ids = jsonObject.getString("ids");
            String status = jsonObject.getString("status");
            String[] arr = ids.split(",");
            for (String id : arr) {
                if (ConvertUtils.isNotEmpty(id)) {
                    this.sysUserService.update(new SysUser().setStatus(Integer.parseInt(status)),
                            new UpdateWrapper<SysUser>().lambda().eq(SysUser::getId, id));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("????????????" + e.getMessage());
        }
        result.success("????????????!");
        return result;

    }

    @RequestMapping(value = "/queryById", method = RequestMethod.GET)
    public Result<SysUser> queryById(@RequestParam(name = "id", required = true) String id) {
        Result<SysUser> result = new Result<SysUser>();
        SysUser sysUser = sysUserService.getById(id);
        if (sysUser == null) {
            result.error500("?????????????????????");
        } else {
            result.setResult(sysUser);
            result.setSuccess(true);
        }
        return result;
    }

    @RequestMapping(value = "/queryUserRole", method = RequestMethod.GET)
    public Result<List<String>> queryUserRole(@RequestParam(name = "userid", required = true) String userid) {
        Result<List<String>> result = new Result<>();
        List<String> list = new ArrayList<String>();
        List<SysUserRole> userRole = sysUserRoleService
                .list(new QueryWrapper<SysUserRole>().lambda().eq(SysUserRole::getUserId, userid));
        if (userRole == null || userRole.size() <= 0) {
            result.error500("?????????????????????????????????");
        } else {
            for (SysUserRole sysUserRole : userRole) {
                list.add(sysUserRole.getRoleId());
            }
            result.setSuccess(true);
            result.setResult(list);
        }
        return result;
    }

    /**
     * ??????????????????????????????<br> ?????????????????? ???????????????????????????????????????
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/checkOnlyUser", method = RequestMethod.GET)
    public Result<Boolean> checkOnlyUser(SysUser sysUser) {
        Result<Boolean> result = new Result<>();
        // ??????????????????false?????????????????????
        result.setResult(true);
        try {
            // ??????????????????????????????????????????
            SysUser user = sysUserService.getOne(new QueryWrapper<SysUser>(sysUser));
            if (user != null) {
                result.setSuccess(false);
                result.setMessage("?????????????????????");
                return result;
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
        result.setSuccess(true);
        return result;
    }

    /**
     * ????????????
     */
    @RequestMapping(value = "/changPassword", method = RequestMethod.PUT)
    public Result<?> changPassword(@RequestBody SysUser sysUser) {
        SysUser u = this.sysUserService
                .getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, sysUser.getUsername()));
        if (u == null) {
            return Result.error("??????????????????");
        }
        sysUser.setId(u.getId());
        return sysUserService.changePassword(sysUser);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/userDepartList", method = RequestMethod.GET)
    public Result<List<DepartIdModel>> getUserDepartsList(
            @RequestParam(name = "userId", required = true) String userId) {
        Result<List<DepartIdModel>> result = new Result<>();
        try {
            List<DepartIdModel> depIdModelList = this.sysUserDepartService.queryDepartIdsOfUser(userId);
            if (depIdModelList != null && depIdModelList.size() > 0) {
                result.setSuccess(true);
                result.setMessage("????????????");
                result.setResult(depIdModelList);
            } else {
                result.setSuccess(false);
                result.setMessage("????????????");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("??????????????????????????????: " + e.getMessage());
            return result;
        }

    }

    /**
     * ???????????????????????????????????????????????????,???????????????,?????????id??????????????????
     *
     * @return
     */
    @RequestMapping(value = "/generateUserId", method = RequestMethod.GET)
    public Result<String> generateUserId() {
        Result<String> result = new Result<>();
        String userId = UUID.randomUUID().toString().replace("-", "");
        result.setSuccess(true);
        result.setResult(userId);
        return result;
    }

    /**
     * ????????????id??????????????????
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/queryUserByDepId", method = RequestMethod.GET)
    public Result<List<SysUser>> queryUserByDepId(@RequestParam(name = "id", required = true) String id) {
        Result<List<SysUser>> result = new Result<>();
        List<SysUser> userList = sysUserDepartService.queryUserByDepId(id);
        try {
            result.setSuccess(true);
            result.setResult(userList);
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setSuccess(false);
            return result;
        }
    }

    /**
     * ??????????????????????????????????????????
     *
     * @return
     */
    @RequestMapping(value = "/queryUserRoleMap", method = RequestMethod.GET)
    public Result<Map<String, String>> queryUserRole() {
        Result<Map<String, String>> result = new Result<>();
        Map<String, String> map = userRoleService.queryUserRole();
        result.setResult(map);
        result.setSuccess(true);
        return result;
    }

    /**
     * ??????excel
     *
     * @param request
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(SysUser sysUser, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        String username = sysUser.getUsername();
        // ?????????????????????*?????????
        if (!StringUtils.isEmpty(username)) {
            if (username.startsWith("*") || username.endsWith("*")) {
                username = username.replace("*", "");
            }
        }
        List<SysUser> pageExportList = sysUserService.queryExport(username, sysUser.getSex());
        // ??????????????????
        mv.addObject(NormalExcelConstants.FILE_NAME, "????????????");
        mv.addObject(NormalExcelConstants.CLASS, SysUser.class);
        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowTime = sdf.format(new Date());
        String secondTitle = "?????????:" + user.getRealname() + "  ???????????????" + nowTime;
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("????????????", secondTitle, "????????????"));
        mv.addObject(NormalExcelConstants.DATA_LIST, pageExportList);
        return mv;
    }

    /**
     * ??????excel????????????
     *
     * @param request
     * @param response
     * @return
     */
    @RequiresPermissions("user:import")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile file = entity.getValue();
            ImportParams params = new ImportParams();
            params.setTitleRows(2);
            params.setHeadRows(1);
            //?????????????????????true?????????????????????excel????????????????????????
            params.setNeedSave(false);
            try {
                List<SysUser> listSysUsers = ExcelImportUtil.importExcel(file.getInputStream(), SysUser.class, params);
                for (SysUser sysUserExcel : listSysUsers) {
                    // ??????????????????
                    sysUserExcel.setCreateTime(new Date());
                    String salt = ConvertUtils.randomGen(8);
                    sysUserExcel.setSalt(salt);
                    if (sysUserExcel.getPassword() == null) {
                        // ??????????????????123456???
                        sysUserExcel.setPassword("123456");
                    }
                    String passwordEncode = PasswordUtil.encrypt(sysUserExcel.getUsername(), sysUserExcel.getPassword(),
                            salt);
                    sysUserExcel.setPassword(passwordEncode);
                    sysUserExcel.setStatus(1);
                    sysUserExcel.setDelFlag("0");

                    sysUserService.save(sysUserExcel);
                }
                return Result.ok("????????????????????????????????????" + listSysUsers.size());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Result.error("??????! ??????????????????????????????????????????.");
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return Result.error("?????????????????????");
    }

    /**
     * @param userIds
     * @return
     * @???????????????id ????????????
     */
    @RequestMapping(value = "/queryByIds", method = RequestMethod.GET)
    public Result<Collection<SysUser>> queryByIds(@RequestParam String userIds) {
        Result<Collection<SysUser>> result = new Result<>();
        String[] userId = userIds.split(",");
        Collection<String> idList = Arrays.asList(userId);
        Collection<SysUser> userRole = sysUserService.listByIds(idList);
        result.setSuccess(true);
        result.setResult(userRole);
        return result;
    }

    /**
     * ????????????????????????
     */
    @RequestMapping(value = "/updatePassword", method = RequestMethod.PUT)
    public Result<?> changPassword(@RequestBody JSONObject json) {
        String username = json.getString("username");
        String oldpassword = json.getString("oldpassword");
        String password = json.getString("password");
        String confirmpassword = json.getString("confirmpassword");
        SysUser user = this.sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            return Result.error("??????????????????");
        }
        return sysUserService.resetPassword(username, oldpassword, password, confirmpassword);
    }

    @RequestMapping(value = "/userRoleList", method = RequestMethod.GET)
    public Result<IPage<SysUser>> userRoleList(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize, HttpServletRequest req) {
        Result<IPage<SysUser>> result = new Result<IPage<SysUser>>();
        Page<SysUser> page = new Page<SysUser>(pageNo, pageSize);
        String roleId = req.getParameter("roleId");
        String username = req.getParameter("username");
        IPage<SysUser> pageList = sysUserService.getUserByRoleId(page, roleId, username);
        result.setSuccess(true);
        result.setResult(pageList);
        return result;
    }

    /**
     * ???????????????????????????
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/addSysUserRole", method = RequestMethod.POST)
    public Result<String> addSysUserRole(@RequestBody SysUserRoleVO sysUserRoleVO) {
        Result<String> result = new Result<String>();
        try {
            String sysRoleId = sysUserRoleVO.getRoleId();
            for (String sysUserId : sysUserRoleVO.getUserIdList()) {
                SysUserRole sysUserRole = new SysUserRole(sysUserId, sysRoleId);
                QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<SysUserRole>();
                queryWrapper.eq("role_id", sysRoleId).eq("user_id", sysUserId);
                SysUserRole one = sysUserRoleService.getOne(queryWrapper);
                if (one == null) {
                    sysUserRoleService.save(sysUserRole);
                }

            }
            result.setMessage("????????????!");
            result.setSuccess(true);
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("?????????: " + e.getMessage());
            return result;
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/deleteUserRole", method = RequestMethod.DELETE)
    public Result<SysUserRole> deleteUserRole(@RequestParam(name = "roleId") String roleId,
                                              @RequestParam(name = "userId", required = true) String userId) {
        Result<SysUserRole> result = new Result<SysUserRole>();
        try {
            QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<SysUserRole>();
            queryWrapper.eq("role_id", roleId).eq("user_id", userId);
            sysUserRoleService.remove(queryWrapper);
            result.success("????????????!");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/deleteUserRoleBatch", method = RequestMethod.DELETE)
    public Result<SysUserRole> deleteUserRoleBatch(@RequestParam(name = "roleId") String roleId,
                                                   @RequestParam(name = "userIds", required = true) String userIds) {
        Result<SysUserRole> result = new Result<SysUserRole>();
        try {
            QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<SysUserRole>();
            queryWrapper.eq("role_id", roleId).in("user_id", Arrays.asList(userIds.split(",")));
            sysUserRoleService.remove(queryWrapper);
            result.success("????????????!");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    /**
     * ??????????????????
     */
    @RequestMapping(value = "/departUserList", method = RequestMethod.GET)
    public Result<IPage<SysUser>> departUserList(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize, HttpServletRequest req) {
        Result<IPage<SysUser>> result = new Result<IPage<SysUser>>();
        Page<SysUser> page = new Page<SysUser>(pageNo, pageSize);
        String depId = req.getParameter("depId");
        String username = req.getParameter("username");
        IPage<SysUser> pageList = sysUserService.getUserByDepId(page, depId, username);
        result.setSuccess(true);
        result.setResult(pageList);
        return result;
    }

    /**
     * ?????? orgCode ?????????????????????????????????????????? ?????????????????????????????????????????????????????????????????????????????????????????????
     */
    @GetMapping("/queryByOrgCode")
    public Result<?> queryByDepartId(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                     @RequestParam(name = "orgCode") String orgCode, SysUser userParams) {
        IPage<SysUserSysDepartModel> pageList = sysUserService.queryUserByOrgCode(orgCode, userParams,
                new Page<SysUserSysDepartModel>(pageNo, pageSize));
        return Result.ok(pageList);
    }

    /**
     * ????????????????????????????????????
     */
    @RequestMapping(value = "/editSysDepartWithUser", method = RequestMethod.POST)
    public Result<String> editSysDepartWithUser(@RequestBody SysDepartUsersVO sysDepartUsersVO) {
        Result<String> result = new Result<String>();
        try {
            String sysDepId = sysDepartUsersVO.getDepId();
            for (String sysUserId : sysDepartUsersVO.getUserIdList()) {
                SysUserDepart sysUserDepart = new SysUserDepart(null, sysUserId, sysDepId);
                QueryWrapper<SysUserDepart> queryWrapper = new QueryWrapper<SysUserDepart>();
                queryWrapper.eq("dep_id", sysDepId).eq("user_id", sysUserId);
                SysUserDepart one = sysUserDepartService.getOne(queryWrapper);
                if (one == null) {
                    sysUserDepartService.save(sysUserDepart);
                }
            }
            result.setMessage("????????????!");
            result.setSuccess(true);
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("?????????: " + e.getMessage());
            return result;
        }
    }

    /**
     * ?????????????????????????????????
     */
    @RequestMapping(value = "/deleteUserInDepart", method = RequestMethod.DELETE)
    public Result<SysUserDepart> deleteUserInDepart(@RequestParam(name = "depId") String depId,
                                                    @RequestParam(name = "userId", required = true) String userId) {
        Result<SysUserDepart> result = new Result<SysUserDepart>();
        try {
            QueryWrapper<SysUserDepart> queryWrapper = new QueryWrapper<SysUserDepart>();
            queryWrapper.eq("dep_id", depId).eq("user_id", userId);
            sysUserDepartService.remove(queryWrapper);
            result.success("????????????!");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    /**
     * ???????????????????????????????????????
     */
    @RequestMapping(value = "/deleteUserInDepartBatch", method = RequestMethod.DELETE)
    public Result<SysUserDepart> deleteUserInDepartBatch(@RequestParam(name = "depId") String depId,
                                                         @RequestParam(name = "userIds", required = true) String userIds) {
        Result<SysUserDepart> result = new Result<SysUserDepart>();
        try {
            QueryWrapper<SysUserDepart> queryWrapper = new QueryWrapper<SysUserDepart>();
            queryWrapper.eq("dep_id", depId).in("user_id", Arrays.asList(userIds.split(",")));
            sysUserDepartService.remove(queryWrapper);
            result.success("????????????!");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    /**
     * ?????????????????????????????????/??????????????????
     *
     * @return
     */
    @RequestMapping(value = "/getCurrentUserDeparts", method = RequestMethod.GET)
    public Result<Map<String, Object>> getCurrentUserDeparts() {
        Result<Map<String, Object>> result = new Result<Map<String, Object>>();
        try {
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            List<SysDepart> list = this.sysDepartService.queryUserDeparts(sysUser.getId());
            Map<String, Object> map = new HashMap<String, Object>(2);
            map.put("list", list);
            map.put("orgCode", sysUser.getOrgCode());
            result.setSuccess(true);
            result.setResult(map);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    /**
     * ??????????????????
     *
     * @param jsonObject
     * @param user
     * @return
     */
    @Deprecated
    @PostMapping("/register")
    public Result<JSONObject> userRegister(@RequestBody JSONObject jsonObject, SysUser user) {
        Result<JSONObject> result = new Result<JSONObject>();
        String phone = jsonObject.getString("phone");
        String smscode = jsonObject.getString("smscode");
        Object code = redisService.get(phone);
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        String email = jsonObject.getString("email");
        SysUser sysUser1 = sysUserService.getUserByName(username);
        if (sysUser1 != null) {
            result.setMessage("??????????????????");
            result.setSuccess(false);
            return result;
        }
        SysUser sysUser2 = sysUserService.getUserByPhone(phone);

        if (sysUser2 != null) {
            result.setMessage("?????????????????????");
            result.setSuccess(false);
            return result;
        }
        SysUser sysUser3 = sysUserService.getUserByEmail(email);
        if (sysUser3 != null) {
            result.setMessage("??????????????????");
            result.setSuccess(false);
            return result;
        }

        if (!smscode.equals(code)) {
            result.setMessage("?????????????????????");
            result.setSuccess(false);
            return result;
        }

        try {
            // ??????????????????
            user.setCreateTime(new Date());
            String salt = ConvertUtils.randomGen(8);
            String passwordEncode = PasswordUtil.encrypt(username, password, salt);
            user.setSalt(salt);
            user.setUsername(username);
            user.setRealname(username);
            user.setPassword(passwordEncode);
            user.setEmail(email);
            user.setPhone(phone);
            user.setStatus(1);
            user.setDelFlag(CommonConstant.DEL_FLAG_0.toString());
            user.setActivitiSync(CommonConstant.ACT_SYNC_1);
            // ??????????????????
            sysUserService.addUserWithRole(user, "ee8626f80f7c2619917b6236f3a7f02b");
            result.success("????????????");
        } catch (Exception e) {
            result.error500("????????????");
        }
        return result;
    }

    /**
     * @param ?????????????????????????????????????????????
     * @return
     */
    @GetMapping("/querySysUser")
    public Result<Map<String, Object>> querySysUser(SysUser sysUser) {
        String phone = sysUser.getPhone();
        String username = sysUser.getUsername();
        Result<Map<String, Object>> result = new Result<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>(2);
        if (ConvertUtils.isNotEmpty(phone)) {
            SysUser user = sysUserService.getUserByPhone(phone);
            if (user != null) {
                map.put("username", user.getUsername());
                map.put("phone", user.getPhone());
                result.setSuccess(true);
                result.setResult(map);
                return result;
            }
        }
        if (ConvertUtils.isNotEmpty(username)) {
            SysUser user = sysUserService.getUserByName(username);
            if (user != null) {
                map.put("username", user.getUsername());
                map.put("phone", user.getPhone());
                result.setSuccess(true);
                result.setResult(map);
                return result;
            }
        }
        result.setSuccess(false);
        result.setMessage("????????????");
        return result;
    }

    /**
     * ?????????????????????
     */
    @PostMapping("/phoneVerification")
    public Result<String> phoneVerification(@RequestBody JSONObject jsonObject) {
        Result<String> result = new Result<String>();
        String phone = jsonObject.getString("phone");
        String smscode = jsonObject.getString("smscode");
        Object code = redisService.get(phone);
        if (!smscode.equals(code)) {
            result.setMessage("?????????????????????");
            result.setSuccess(false);
            return result;
        }
        redisService.set(phone, smscode);
        result.setResult(smscode);
        result.setSuccess(true);
        return result;
    }

    /**
     * ??????????????????
     */
    @GetMapping("/passwordChange")
    public Result<SysUser> passwordChange(@RequestParam(name = "username") String username,
                                          @RequestParam(name = "password") String password, @RequestParam(name = "smscode") String smscode,
                                          @RequestParam(name = "phone") String phone) {
        Result<SysUser> result = new Result<SysUser>();
        SysUser sysUser = new SysUser();
        Object object = redisService.get(phone);
        if (null == object) {
            result.setMessage("??????????????????");
            result.setSuccess(false);
        }
        if (!smscode.equals(object)) {
            result.setMessage("??????????????????");
            result.setSuccess(false);
        }
        sysUser = this.sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (sysUser == null) {
            result.setMessage("?????????????????????");
            result.setSuccess(false);
            return result;
        } else {
            String salt = ConvertUtils.randomGen(8);
            sysUser.setSalt(salt);
            String passwordEncode = PasswordUtil.encrypt(sysUser.getUsername(), password, salt);
            sysUser.setPassword(passwordEncode);
            this.sysUserService.updateById(sysUser);
            result.setSuccess(true);
            result.setMessage("?????????????????????");
            return result;
        }
    }

    /**
     * ??????TOKEN???????????????????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    @GetMapping("/getUserSectionInfoByToken")
    public Result<?> getUserSectionInfoByToken(HttpServletRequest request,
                                               @RequestParam(name = "token", required = false) String token) {
        try {
            String username = null;
            // ??????????????????token?????????header?????????token?????????????????????
            if (ConvertUtils.isEmpty(token)) {
                username = JwtUtil.getUserNameByToken(request);
            } else {
                username = JwtUtil.getUsername(token);
            }

            log.info(" ------ ?????????????????????????????????????????????????????? " + username);

            // ?????????????????????????????????
            SysUser sysUser = sysUserService.getUserByName(username);
            Map<String, Object> map = new HashMap<String, Object>(4);
            map.put("sysUserId", sysUser.getId());
            // ??????????????????????????????
            map.put("sysUserCode", sysUser.getUsername());
            // ??????????????????????????????
            map.put("sysUserName", sysUser.getRealname());
            // ??????????????????????????????
            map.put("sysOrgCode", sysUser.getOrgCode());

            log.info(" ------ ?????????????????????????????????????????????????????????????????? " + map);

            return Result.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(500, "????????????:" + e.getMessage());
        }
    }

    /**
     * ?????????????????? ??????????????????????????? ????????????
     *
     * @param keyword
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/appUserList")
    public Result<?> appUserList(@RequestParam(name = "keyword", required = false) String keyword,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>();
            query.eq(SysUser::getActivitiSync, "1");
            query.eq(SysUser::getDelFlag, "0");
            query.and(i -> i.like(SysUser::getUsername, keyword).or().like(SysUser::getRealname, keyword));

            Page<SysUser> page = new Page<>(pageNo, pageSize);
            IPage<SysUser> res = this.sysUserService.page(page, query);
            return Result.ok(res);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(500, "????????????:" + e.getMessage());
        }

    }

    /**
     * ??????: ??????????????????-(id-value) @author??? songguang.jiao ??????????????? 2020???1???19??? ??????4:07:31 ?????????:
     * V1.0
     */
    @ApiImplicitParams({@ApiImplicitParam(name = "name", value = "????????????", paramType = "query")})
    @ApiOperation(value = "??????????????????(id-value)", notes = "??????????????????(id-value)")
    @GetMapping("/idList")
    public Result<List<UserIdDropdown>> userList(String name) {
        Result<List<UserIdDropdown>> result = new Result<>();
        List<UserIdDropdown> userList = sysUserService.getUserIdList(name);
        result.setResult(userList);
        return result.success("??????????????????");
    }

}
