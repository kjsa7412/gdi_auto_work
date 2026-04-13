package com.sjinc.proj.{{module_group}}.{{sub_group}}.{{module_id}};

import com.sjinc.frame.annotation.AddUserInfo;
import com.sjinc.frame.exception.FramePostgresException;
import com.sjinc.frame.exception.FramePostgresExceptionCode;
import com.sjinc.proj.base.BaseController;
import com.sjinc.proj.base.BaseParm;
import com.sjinc.proj.base.BaseResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

{{required_imports}}

@Slf4j
@Controller
@RequestMapping("/{{module_id}}")
public class {{controller_class}} extends BaseController {

    private final {{service_class}} {{service_instance}};

    public {{controller_class}}({{service_class}} {{service_instance}}) {
        this.{{service_instance}} = {{service_instance}};
    }

    /* OPTIONAL: process block start (input-type popup with server processing) */
    @AddUserInfo
    @ResponseBody
    @PostMapping("/process")
    public BaseResponse process(HttpServletRequest request, @RequestBody BaseParm param) {
        try {
            Map<String, Object> result = {{service_instance}}.process(param);
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
    /* OPTIONAL: process block end */

}
