package top.nomelin.iot;

import org.apache.iotdb.session.Session;
import org.apache.tsfile.enums.TSDataType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;

@SpringBootTest
class IotApplicationTests {
    @Autowired
    Session session;

    @Test
    void test() {
        TSDataType dataType = TSDataType.DOUBLE;
        System.out.println(dataType);
        dataType = TSDataType.valueOf("FLOAT");
        System.out.println(dataType);
    }
}

