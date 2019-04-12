package com.ime.gbt32960.codec;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Qingxi
 * 命令标识
 */

@AllArgsConstructor
@Getter
public enum RequestType {
    // 车辆登入
    LOGIN((byte) 0x01),
    // 实时信息上报
    REAL_TIME((byte) 0x02),
    // 补发信息上报
    REISSUE((byte) 0x03),
    // 车辆登出
    LOGOUT((byte) 0x04),

    // 心跳
    HEARTBEAT((byte) 0x07),
    // 终端校时
    CLOCK_CORRECT((byte) 0x08),
    // 参数查询命令
    CONFIG_QUERY((byte) 0x80),
    // 参数设置命令
    CONFIG_SETUP((byte) 0x81),
    // 车载终端控制命令
    CONTROL((byte) 0x82);

    private final byte value;

    public static RequestType valueOf(byte type) {
        for (RequestType t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown message type : " + type);
    }

}
