package com.sjinc.proj.{{module_group}}.{{sub_group}}.{{module_id}};

import com.sjinc.proj.base.BaseService;
import com.sjinc.proj.login.LoginUserVo;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

{{required_imports}}

@Service
@Slf4j
public class {{service_class}} extends BaseService {

    private final SqlSessionTemplate sqlSessionTemplate;

    public {{service_class}}(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /**
     * 저장
     */
    @Transactional(value = "txManager")
    public void saveData(Map<String, Object> param, LoginUserVo loginUserVo) {
        param.put("login_comp_cd", loginUserVo.getLogin_comp_cd());
        param.put("login_emp_no", loginUserVo.getLogin_emp_no());
        param.put("login_user_id", loginUserVo.getLogin_user_id());
        param.put("login_user_ip", loginUserVo.getLogin_user_ip());
        param.put("reg_pgm_id", param.get("reg_pgm_id"));

        sqlSessionTemplate.insert("{{mapper_namespace}}.{{insert_sql_id}}", param);
    }

    /* OPTIONAL: delete block */
    /* OPTIONAL: file upload block */
    /* OPTIONAL: validation block */

}
