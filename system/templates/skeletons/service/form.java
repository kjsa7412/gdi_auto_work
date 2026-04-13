package com.sjinc.proj.{{module_group}}.{{sub_group}}.{{module_id}};

import com.sjinc.proj.base.BaseService;
import com.sjinc.proj.base.BaseParmAll;

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

    /* OPTIONAL: custom save block start */
    /**
     * 저장
     */
    @Transactional(value = "txManager")
    protected void save(List<BaseParmAll> parm) {
        for (BaseParmAll item : parm) {
            Object data = item.getData();
            if (data instanceof List) {
                List<Map<String, String>> dataList = (List) data;
                for (Map<String, String> dataitem : dataList) {
                    sqlSessionTemplate.insert(item.getStatement(), dataitem);
                }
            }
            if (data instanceof Map) {
                Map<String, String> dataMap = (Map<String, String>) data;
                sqlSessionTemplate.insert(item.getStatement(), dataMap);
            }
        }
    }
    /* OPTIONAL: custom save block end */

    /* OPTIONAL: delete block start */
    /**
     * 삭제 (참조 무결성 검사 포함)
     */
    @Transactional(value = "txManager")
    protected void delete(Map<String, Object> param) {
        Integer cnt = sqlSessionTemplate.selectOne("{{mapper_namespace}}.checkCount", param);
        if (cnt != null && cnt > 0) {
            throw new IllegalStateException("This record is referenced by other records and cannot be deleted.");
        }
        sqlSessionTemplate.delete("{{mapper_namespace}}.{{delete_sql_id}}", param);
    }
    /* OPTIONAL: delete block end */

    /* OPTIONAL: file upload block */
    /* OPTIONAL: validation block */

}
