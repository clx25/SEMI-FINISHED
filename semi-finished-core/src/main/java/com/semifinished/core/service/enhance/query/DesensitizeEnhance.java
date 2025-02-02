package com.semifinished.core.service.enhance.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.semifinished.core.jdbc.QuerySqlCombiner;
import com.semifinished.core.jdbc.SqlDefinition;
import com.semifinished.core.pojo.Column;
import com.semifinished.core.pojo.Desensitization;
import com.semifinished.core.pojo.ResultHolder;
import com.semifinished.core.utils.ParamsUtils;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 脱敏增强
 */
@Getter
@Component
public class DesensitizeEnhance implements AfterQueryEnhance {

    private final List<Desensitization> desensitizes = new ArrayList<>();

    @Override
    public void afterQuery(ResultHolder resultHolder, SqlDefinition sqlDefinition) {
        List<ObjectNode> records = resultHolder.getRecords();
        if (records.isEmpty()) {
            return;
        }
        Map<String, Function<JsonNode, JsonNode>> desensitizeMap = getDesensitizeMap(sqlDefinition);
        desensitizeMap.keySet().forEach(k -> {
            for (ObjectNode record : records) {
                if (record.has(k)) {
                    record.set(k, desensitizeMap.get(k).apply(record.path(k)));
                }
            }
        });
    }


    /**
     * 获取查询字段与脱敏方法的映射
     *
     * @param sqlDefinition SQL定义信息
     * @return 查询字段与脱敏方法的映射
     */
    private Map<String, Function<JsonNode, JsonNode>> getDesensitizeMap(SqlDefinition sqlDefinition) {

        List<Column> columns = QuerySqlCombiner.columnAggregationAll(sqlDefinition);

        Map<String, Function<JsonNode, JsonNode>> desensitizeMap = new HashMap<>();

        desensitizes.forEach(d -> {
            columns.stream()
                    .filter(col -> StringUtils.hasText(col.getTable()))
                    .filter(col -> col.getTable().equals(d.getTable()))
                    .filter(col -> col.getColumn().equals(d.getColumn()))
                    .map(col -> ParamsUtils.hasText(col.getAlias(), col.getColumn()))
                    .forEach(column -> {
                        Function<JsonNode, JsonNode> desensitize = d.getDesensitize();
                        if (desensitize == null) {
                            desensitize = value -> desensitize(value, d.getLeft(), d.getRight());
                        }
                        desensitizeMap.put(column, desensitize);
                    });
        });
        return desensitizeMap;
    }


    /**
     * 数据脱敏
     *
     * @param valueNode 需要脱敏的数据
     * @param left      保留数据的前几个字符
     * @param right     保留数据的后几个字符
     * @return 脱敏后的数据
     */
    private JsonNode desensitize(JsonNode valueNode, double left, double right) {
        String value = valueNode.asText(null);
        if (!StringUtils.hasText(value)) {
            return valueNode;
        }

        if (left > 0 && left < 1 || (right > 0 && right < 1)) {
            int length = value.length();
            left = length * left;
            right = length * right;
        }
        char[] chars = value.toCharArray();
        for (double i = left; i < chars.length - (int) right; i++) {
            chars[(int) i] = '*';
        }

        return TextNode.valueOf(new String(chars));

    }


}

