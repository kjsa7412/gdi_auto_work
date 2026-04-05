package com.sjinc.proj.{{module_group}}.{{sub_group}}.{{module_id}};

import com.sjinc.frame.annotation.AddUserInfo;
import com.sjinc.frame.annotation.LogAction;
import com.sjinc.frame.exception.FramePostgresException;
import com.sjinc.frame.exception.FramePostgresExceptionCode;
import com.sjinc.frame.utils.FrameConstants;
import com.sjinc.proj.base.BaseParm;
import com.sjinc.proj.base.BaseResponse;
import com.sjinc.proj.common.CommonService;
import com.sjinc.proj.login.LoginUserVo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

{{required_imports}}

@Slf4j
@Controller
@RequestMapping("/{{request_base_path}}")
public class {{controller_class}} {

    private final CommonService commonService;
    private final {{service_class}} {{service_instance}};

    public {{controller_class}}(CommonService commonService, {{service_class}} {{service_instance}}) {
        this.commonService = commonService;
        this.{{service_instance}} = {{service_instance}};
    }

    /* OPTIONAL: custom save block start */
    /**
     * 저장 (복잡 트랜잭션)
     */
    @AddUserInfo
    @LogAction
    @ResponseBody
    @RequestMapping(value = "/saveData", method = RequestMethod.POST)
    public BaseResponse saveData(HttpServletRequest request, @RequestBody BaseParm param) {
        try {
            LoginUserVo loginUserVo = (LoginUserVo) request.getAttribute(FrameConstants.LOGIN_USER_ATTR);
            {{service_instance}}.saveData(param, loginUserVo);
            return BaseResponse.Ok();
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
    /* OPTIONAL: custom save block end */

    /* OPTIONAL: custom delete block start */
    /**
     * 삭제
     */
    @AddUserInfo
    @LogAction
    @ResponseBody
    @RequestMapping(value = "/deleteData", method = RequestMethod.POST)
    public BaseResponse deleteData(HttpServletRequest request, @RequestBody BaseParm param) {
        try {
            LoginUserVo loginUserVo = (LoginUserVo) request.getAttribute(FrameConstants.LOGIN_USER_ATTR);
            {{service_instance}}.deleteData(param, loginUserVo);
            return BaseResponse.Ok();
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
    /* OPTIONAL: custom delete block end */

    /* OPTIONAL: excel upload block */
    /* OPTIONAL: validation block */

}
