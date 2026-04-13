package com.sjinc.proj.{{module_group}}.{{sub_group}}.{{module_id}};

import com.sjinc.frame.annotation.AddUserInfo;
import com.sjinc.frame.exception.FramePostgresException;
import com.sjinc.frame.exception.FramePostgresExceptionCode;
import com.sjinc.frame.excel.ExcelUtil;
import com.sjinc.frame.utils.FrameConstants;
import com.sjinc.frame.utils.FrameFileUtil;
import com.sjinc.proj.base.BaseController;
import com.sjinc.proj.base.BaseParm;
import com.sjinc.proj.base.BaseResponse;
import com.sjinc.proj.common.CommonService;
import com.sjinc.proj.login.LoginUserVo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

{{required_imports}}

@Slf4j
@Controller
@RequestMapping("/{{module_id}}")
public class {{controller_class}} extends BaseController {

    private final {{service_class}} {{service_instance}};

    /* OPTIONAL: excel upload - CommonService DI start */
    // private final CommonService commonService;
    /* OPTIONAL: excel upload - CommonService DI end */

    /* OPTIONAL: constructor with CommonService start */
    // public {{controller_class}}({{service_class}} {{service_instance}}, CommonService commonService) {
    //     this.{{service_instance}} = {{service_instance}};
    //     this.commonService = commonService;
    // }
    /* OPTIONAL: constructor with CommonService end */

    /* OPTIONAL: constructor without CommonService start */
    public {{controller_class}}({{service_class}} {{service_instance}}) {
        this.{{service_instance}} = {{service_instance}};
    }
    /* OPTIONAL: constructor without CommonService end */

    /* OPTIONAL: excel upload block start */
    @AddUserInfo
    @ResponseBody
    @RequestMapping(value = "/uploadExcel", method = RequestMethod.POST)
    public BaseResponse uploadExcel(HttpServletRequest request, @RequestBody BaseParm param) {
        try {
            Map<String, String> fileInfo = commonService.selectOneAsT("file.selectFile", param);
            LoginUserVo loginUserVo = (LoginUserVo) request.getAttribute(FrameConstants.LOGIN_USER_ATTR);

            String filePath = fileInfo.get("file_path");
            String fileName = fileInfo.get("save_file_nm");
            Resource resource = FrameFileUtil.loadAsResource("", filePath, fileName);
            File file = resource.getFile();
            List<Map<String, Object>> xlsData = ExcelUtil.getList(file);

            List<Map<String, Object>> result = {{service_instance}}.uploadExcel(xlsData, param);
            return BaseResponse.Ok(result);
        } catch (FileNotFoundException ex) {
            log.error(ex.getMessage());
            return BaseResponse.Error("파일을 찾을 수 없습니다.");
        } catch (FramePostgresException ex) {
            if (FramePostgresExceptionCode.CUSTOM_P9999.getCode().equals(ex.getSqlState())) {
                return BaseResponse.Warn(ex.getMessage());
            }
            return BaseResponse.Error(ex.getMessage());
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return BaseResponse.Error(ex.getMessage());
        }
    }
    /* OPTIONAL: excel upload block end */

    /* OPTIONAL: validation block start */
    @AddUserInfo
    @ResponseBody
    @RequestMapping(value = "/validation", method = RequestMethod.POST)
    public BaseResponse validation(HttpServletRequest request, @RequestBody BaseParm param) {
        try {
            List<Map<String, Object>> result = {{service_instance}}.validation(param);
            return BaseResponse.Ok(result);
        } catch (FramePostgresException ex) {
            if (FramePostgresExceptionCode.CUSTOM_P9999.getCode().equals(ex.getSqlState())) {
                return BaseResponse.Warn(ex.getMessage());
            }
            return BaseResponse.Error(ex.getMessage());
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return BaseResponse.Error(ex.getMessage());
        }
    }
    /* OPTIONAL: validation block end */

}
