package top.nomelin.iot.util;

import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import top.nomelin.iot.common.Constants;

import java.util.Date;

/**
 * TokenUtil
 *
 * @author nomelin
 * @since 2024/12/18 16:50
 **/
public class TokenUtil {
    /**
     * 创建 token
     *
     * @param data 载荷数据
     * @param sign 密钥
     * @return token
     */
    public static String createToken(String data, String sign) {
        return JWT.create().withAudience(data) // data作为载荷
                .withIssuer("iot-manager")// 设置 token 的签发者
                .withIssuedAt(new Date()) // 设置 token 的签发时间
                .withExpiresAt(DateUtil.offsetHour(new Date(), Constants.TOKEN_EXPIRE_TIME)) // 设置 token 的过期时间
                .sign(Algorithm.HMAC256(sign)); // sign 作为 token 的密钥
    }
}
