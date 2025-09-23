package com.nexusvoice.infrastructure.converter;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Map 类型转换器
 * 用于MyBatis-Plus处理JSON格式的Map对象
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@MappedTypes({Map.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class MapConverter extends AbstractJsonTypeHandler<Map<String, Object>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        if (MapUtil.isEmpty(parameter)) {
            ps.setString(i, "{}");
        } else {
            ps.setString(i, JSONUtil.toJsonStr(parameter));
        }
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    /**
     * 解析JSON字符串为Map
     */
    private Map<String, Object> parseJson(String json) {
        if (StrUtil.isBlank(json)) {
            return MapUtil.newHashMap();
        }
        
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // 如果解析失败，返回空Map
            return MapUtil.newHashMap();
        }
    }
}
