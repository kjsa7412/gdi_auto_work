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

    /* OPTIONAL: custom save block start */
    /**
     * 저장 (복잡 트랜잭션)
     */
    @Transactional(value = "txManager")
    public void saveData(Map<String, Object> param, LoginUserVo loginUserVo) {
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) param.get("datalist");

        for (Map<String, Object> item : dataList) {
            item.put("login_comp_cd", loginUserVo.getLogin_comp_cd());
            item.put("login_emp_no", loginUserVo.getLogin_emp_no());
            item.put("login_user_id", loginUserVo.getLogin_user_id());
            item.put("login_user_ip", loginUserVo.getLogin_user_ip());
            item.put("reg_pgm_id", param.get("reg_pgm_id"));

            sqlSessionTemplate.insert("{{mapper_namespace}}.{{insert_sql_id}}", item);
        }
    }
    /* OPTIONAL: custom save block end */

    /* OPTIONAL: custom delete block start */
    /**
     * 삭제
     */
    @Transactional(value = "txManager")
    public void deleteData(Map<String, Object> param, LoginUserVo loginUserVo) {
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) param.get("datalist");

        for (Map<String, Object> item : dataList) {
            item.put("login_comp_cd", loginUserVo.getLogin_comp_cd());
            sqlSessionTemplate.delete("{{mapper_namespace}}.{{delete_sql_id}}", item);
        }
    }
    /* OPTIONAL: custom delete block end */

}
