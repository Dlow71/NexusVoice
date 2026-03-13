package com.nexusvoice.infrastructure.persistence.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 PostgreSQL pgvector 列与 Java List<Float> 互转的 TypeHandler
 *
 * 列值格式示例: "[0.12,0.34,0.56]"
 */
public class PGVectorTypeHandler extends BaseTypeHandler<List<Float>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Float> parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("vector");
        pgObject.setValue(toVectorString(parameter));
        ps.setObject(i, pgObject);
    }

    @Override
    public List<Float> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return parseVectorString(value);
    }

    @Override
    public List<Float> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return parseVectorString(value);
    }

    @Override
    public List<Float> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return parseVectorString(value);
    }

    private String toVectorString(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(',');
            // 避免科学计数法，直接用 float 的 toString 即可
            sb.append(vector.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private List<Float> parseVectorString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String s = value.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        if (s.isBlank()) return new ArrayList<>();
        String[] parts = s.split(",");
        List<Float> list = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                list.add(Float.parseFloat(t));
            }
        }
        return list;
    }
}
