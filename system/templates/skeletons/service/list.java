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

    /* OPTIONAL: excel upload block start */
    /**
     * 엑셀 업로드 데이터 변환
     */
    public List<Map<String, Object>> uploadExcel(
            List<Map<String, Object>> xlsData,
            Map<String, Object> param) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : xlsData) {
            Map<String, Object> item = new HashMap<>();
            // TODO: 엑셀 컬럼 → DB 컬럼 매핑
            // item.put("db_column", row.get("엑셀헤더명"));
            result.add(item);
        }

        return result;
    }
    /* OPTIONAL: excel upload block end */

    /* OPTIONAL: validation block start */
    /**
     * 데이터 검증
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> validation(Map<String, Object> param) {

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) param.get("datalist");

        for (Map<String, Object> row : dataList) {
            row.put("valid", "Y");
            row.put("remark", "");
            // TODO: 검증 로직 구현
        }

        /* OPTIONAL: set-collect DB lookup validation start */
        // === Set 수집 → DB 일괄 조회 → Map 변환 → 행별 검증 ===
        // Set<String> codeSet = new HashSet<>();
        // for (Map<String, Object> row : dataList) {
        //     String code = (String) row.get("{{code_column}}");
        //     if (code != null && !code.isEmpty()) {
        //         codeSet.add(code);
        //     }
        // }
        //
        // Map<String, Object> lookupParam = new HashMap<>();
        // lookupParam.put("codes", new ArrayList<>(codeSet));
        // List<Map<String, Object>> dbList = sqlSessionTemplate.selectList("{{mapper_namespace}}.{{lookup_sql_id}}", lookupParam);
        //
        // Map<String, Map<String, Object>> dbMap = new HashMap<>();
        // for (Map<String, Object> dbRow : dbList) {
        //     dbMap.put((String) dbRow.get("{{code_column}}"), dbRow);
        // }
        //
        // for (Map<String, Object> row : dataList) {
        //     String code = (String) row.get("{{code_column}}");
        //     if (code != null && !dbMap.containsKey(code)) {
        //         row.put("valid", "N");
        //         row.put("remark", row.get("remark") + "존재하지 않는 코드입니다. ");
        //     }
        // }
        /* OPTIONAL: set-collect DB lookup validation end */

        return dataList;
    }
    /* OPTIONAL: validation block end */

    /* OPTIONAL: batch insert start */
    /**
     * Batch 처리 유틸 (대량 데이터 INSERT)
     */
    // private static final int BATCH_SIZE = 500;
    //
    // @Transactional(value = "txManager")
    // protected void insertBatch(String statementId, List<Map<String, Object>> dataList) {
    //     insertBatch(statementId, dataList, BATCH_SIZE);
    // }
    //
    // @Transactional(value = "txManager")
    // protected void insertBatch(String statementId, List<Map<String, Object>> dataList, int batchSize) {
    //     for (int i = 0; i < dataList.size(); i += batchSize) {
    //         int end = Math.min(i + batchSize, dataList.size());
    //         List<Map<String, Object>> batch = dataList.subList(i, end);
    //         for (Map<String, Object> item : batch) {
    //             sqlSessionTemplate.insert(statementId, item);
    //         }
    //         sqlSessionTemplate.flushStatements();
    //     }
    // }
    /* OPTIONAL: batch insert end */

}
