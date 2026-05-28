package com.nexusvoice.infrastructure.persistence.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL text[] 与 Java String[] 的转换器。
 */
public class PGTextArrayTypeHandler extends BaseTypeHandler<String[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String[] parameter, JdbcType jdbcType)
            throws SQLException {
        Array sqlArray = ps.getConnection().createArrayOf("text", parameter);
        ps.setArray(i, sqlArray);
    }

    @Override
    public String[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return extractArray(rs.getArray(columnName));
    }

    @Override
    public String[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return extractArray(rs.getArray(columnIndex));
    }

    @Override
    public String[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return extractArray(cs.getArray(columnIndex));
    }

    private String[] extractArray(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return null;
        }
        Object array = sqlArray.getArray();
        if (array instanceof String[] values) {
            return values;
        }
        Object[] values = (Object[]) array;
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] == null ? null : String.valueOf(values[i]);
        }
        return result;
    }
}
