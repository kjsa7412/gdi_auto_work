package com.sjinc.proj.{{module_group}}.{{sub_group}}.{{module_id}};

import com.sjinc.proj.base.BaseService;
import com.sjinc.proj.login.LoginUserVo;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

{{required_imports}}

@Service
@Slf4j
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
            Map<String, Object> param,
            LoginUserVo loginUserVo) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : xlsData) {
            Map<String, Object> item = new HashMap<>();
            // TODO: 엑셀 컬럼 → DB 컬럼 매핑
            // item.put("db_column", row.get("엑셀헤더명"));
            item.put("login_comp_cd", loginUserVo.getLogin_comp_cd());
            item.put("login_emp_no", loginUserVo.getLogin_emp_no());
            item.put("login_user_id", loginUserVo.getLogin_user_id());
            item.put("login_user_ip", loginUserVo.getLogin_user_ip());
            item.put("reg_pgm_id", param.get("reg_pgm_id"));
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
    public List<Map<String, Object>> validation(
            Map<String, Object> param,
            LoginUserVo loginUserVo) {

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) param.get("datalist");

        // 1. 코드 수집 (벌크 최적화)
        // Set<String> codes = dataList.stream()
        //     .map(row -> (String) row.get("code_field"))
        //     .filter(Objects::nonNull)
        //     .collect(Collectors.toSet());

        // 2. DB에서 일괄 조회
        // Map<String, Object> lookupParam = new HashMap<>();
        // lookupParam.put("login_comp_cd", loginUserVo.getLogin_comp_cd());
        // lookupParam.put("codes", codes);
        // List<Map<String, Object>> dbList = sqlSessionTemplate.selectList("{{mapper_namespace}}.selectCodeList", lookupParam);

        // 3. Map으로 변환
        // Map<String, Map<String, Object>> codeMap = dbList.stream()
        //     .collect(Collectors.toMap(m -> (String) m.get("cd"), m -> m));

        // 4. 행별 검증
        for (Map<String, Object> row : dataList) {
            row.put("valid", "Y");
            row.put("remark", "");
            // TODO: 검증 로직 구현
            // if (!codeMap.containsKey(row.get("code_field"))) {
            //     row.put("valid", "N");
            //     row.put("remark", "존재하지 않는 코드");
            // }
        }

        return dataList;
    }
    /* OPTIONAL: validation block end */

}
