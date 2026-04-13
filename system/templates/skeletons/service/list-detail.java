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
     * 저장 (다중 테이블 트랜잭션)
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

    /* OPTIONAL: custom delete block start */
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
    /* OPTIONAL: custom delete block end */

    /* OPTIONAL: requires-new per-row error isolation start */
    /**
     * REQUIRES_NEW 트랜잭션으로 행별 에러 격리
     * - 각 행을 독립 트랜잭션으로 처리하여, 한 행 실패 시 다른 행에 영향 없음
     * - 이 메서드는 별도 Bean(Self-invocation 회피)이나 같은 서비스 내에서
     *   AOP 프록시를 통해 호출해야 REQUIRES_NEW가 적용됨
     */
    // @Transactional(value = "txManager", propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    // protected Map<String, Object> saveOneRow(Map<String, Object> row) {
    //     try {
    //         sqlSessionTemplate.insert("{{mapper_namespace}}.{{insert_sql_id}}", row);
    //         row.put("result", "OK");
    //     } catch (Exception ex) {
    //         row.put("result", "FAIL");
    //         row.put("remark", ex.getMessage());
    //     }
    //     return row;
    // }
    //
    // /**
    //  * 행별 독립 저장 (에러 격리)
    //  */
    // @Transactional(value = "txManager")
    // protected List<Map<String, Object>> saveWithIsolation(List<Map<String, Object>> dataList) {
    //     List<Map<String, Object>> results = new ArrayList<>();
    //     for (Map<String, Object> row : dataList) {
    //         Map<String, Object> result = saveOneRow(row);
    //         results.add(result);
    //     }
    //     return results;
    // }
    /* OPTIONAL: requires-new per-row error isolation end */

}
