package top.nomelin.iot.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import top.nomelin.iot.model.alert.ConditionConfig;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ConditionConfigTypeHandler
 *
 * @author nomelin
 * @since 2025/03/19 21:56
 **/
public class ConditionConfigTypeHandler extends BaseTypeHandler<ConditionConfig> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ConditionConfig parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Error converting ConditionConfig to JSON string: " + parameter, e);
        }
    }

    @Override
    public ConditionConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public ConditionConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public ConditionConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private ConditionConfig parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ConditionConfig.class);
        } catch (Exception e) {
            throw new SQLException("Error converting JSON string to ConditionConfig: " + json, e);
        }
    }
}
