package com.sjinc.proj.{{module_group}}.{{sub_group}}.{{module_id}};

import com.sjinc.frame.annotation.AddUserInfo;
import com.sjinc.frame.exception.FramePostgresException;
import com.sjinc.frame.exception.FramePostgresExceptionCode;
import com.sjinc.proj.base.BaseController;
import com.sjinc.proj.base.BaseParm;
import com.sjinc.proj.base.BaseParmAll;
import com.sjinc.proj.base.BaseResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    /* OPTIONAL: custom save block start */
    @AddUserInfo
    @ResponseBody
    @PostMapping("/save")
    public BaseResponse save(HttpServletRequest request, @RequestBody List<BaseParmAll> parm) {
        try {
            {{service_instance}}.save(parm);
            return BaseResponse.Ok(parm);
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

    /* OPTIONAL: delete block start */
    @AddUserInfo
    @ResponseBody
    @PostMapping("/delete")
    public BaseResponse delete(HttpServletRequest request, @RequestBody Map<String, Object> param) {
        try {
            {{service_instance}}.delete(param);
            return BaseResponse.Ok(param);
        } catch (IllegalStateException ex) {
            return BaseResponse.Error(ex.getMessage());
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
    /* OPTIONAL: delete block end */

    /* OPTIONAL: file upload block */
    /* OPTIONAL: validation block */

}
