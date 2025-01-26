package top.nomelin.iot.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import top.nomelin.iot.model.ViewConfig;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ViewConfigTypeHandler
 *
 * @author nomelin
 * @since 2025/01/26 23:21
 **/
public class ViewConfigTypeHandler extends BaseTypeHandler<ViewConfig> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ViewConfig parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Error converting Config to JSON string: " + parameter, e);
        }
    }


    @Override
    public ViewConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public ViewConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public ViewConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private ViewConfig parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ViewConfig.class);
        } catch (Exception e) {
            throw new SQLException("Error converting JSON string to ViewConfig: " + json, e);
        }
    }
}
