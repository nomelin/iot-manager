package top.nomelin.iot.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import top.nomelin.iot.model.alert.ActionConfig;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ActionConfigTypeHandler
 *
 * @author nomelin
 * @since 2025/03/19 21:58
 **/
public class ActionConfigTypeHandler extends BaseTypeHandler<ActionConfig> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ActionConfig parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Error converting ActionConfig to JSON string: " + parameter, e);
        }
    }

    @Override
    public ActionConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public ActionConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public ActionConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private ActionConfig parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ActionConfig.class);
        } catch (Exception e) {
            throw new SQLException("Error converting JSON string to ActionConfig: " + json, e);
        }
    }
}
