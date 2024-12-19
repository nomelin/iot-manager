package top.nomelin.iot.model;


import org.apache.tsfile.read.common.Field;
import org.apache.tsfile.read.common.RowRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Record {
    private Map<String, Object> fields;

    public Record() {
        fields = new HashMap<>();
    }

    /**
     * 将 IoTDB 的 RowRecord 转换为自定义的 Record 对象
     *
     * @param rowRecord  IoTDB RowRecord
     * @param fieldNames 字段名列表
     * @return 转换后的自定义 Record
     */
    public static Record convertToRowRecord(RowRecord rowRecord, List<String> fieldNames) {
        Record result = new Record();
        List<Field> fields = rowRecord.getFields();

        // 遍历每个 Field，获取字段值并存入 result 的 fields 中
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            String fieldName = fieldNames.get(i); // 假设字段名列表与字段顺序一致
            Object fieldValue = field.getObjectValue(field.getDataType()); // 使用 Field 的方法获取字段值
            result.fields.put(fieldName, fieldValue);
        }
        return result;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
}