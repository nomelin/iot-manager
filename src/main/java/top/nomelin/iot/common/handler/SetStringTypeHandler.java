package top.nomelin.iot.common.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * 用于转换Set<String>和JSON字符串。处理数据库的Json String类型到实体对象的Set<String>类型转换。
 */
public class SetStringTypeHandler extends BaseTypeHandler<Set<String>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Set<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Error converting Set<String> to JSON string: " + parameter, e);
        }
    }

    @Override
    public Set<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public Set<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public Set<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private Set<String> parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            // 使用 HashSet 来保证不重复的元素
            return objectMapper.readValue(json, new TypeReference<HashSet<String>>() {});
        } catch (Exception e) {
            throw new SQLException("Error converting JSON string to Set<String>: " + json, e);
        }
    }
}
