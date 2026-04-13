package com.sjinc.proj.{{module_group}}.{{sub_group}}.{{module_id}};

import com.sjinc.proj.base.BaseService;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

{{required_imports}}

@Slf4j
@Service
public class {{service_class}} extends BaseService {

    private final SqlSessionTemplate sqlSessionTemplate;

    public {{service_class}}(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /* OPTIONAL: process block start (input-type popup with server processing) */
    /**
     * 팝업 처리
     */
    @Transactional(value = "txManager")
    protected Map<String, Object> process(Map<String, Object> param) {
        // TODO: 팝업 비즈니스 로직 구현
        sqlSessionTemplate.insert("{{mapper_namespace}}.{{insert_sql_id}}", param);
        return param;
    }
    /* OPTIONAL: process block end */

}
