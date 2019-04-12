package com.ime.gbt32960.codec;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Qingxi
 * 实时消息类型
 */

@AllArgsConstructor
@Getter
public enum RealTimeType {

    // 整车数据
    VEHICLE((byte) 0x01),
    // 驱动电机数据
    MOTOR((byte) 0x02),
    // 燃料电池数据
    FUEL_CELL((byte) 0x03),
    // 发动机数据
    ENGINE((byte) 0x04),
    // 车辆位置
    LOCATION((byte) 0x05),
    // 极值数据
    EXTREMUM((byte) 0x06),
    // 报警数据
    ALARM((byte) 0x07),
    // 可充电储能装置电压数据
    BATTERY_VOLTAGE((byte) 0x08),
    // 可充电储能装置温度数据
    BATTERY_TEMPERATURE((byte) 0x09);

    private final byte value;

    public static RealTimeType valueOf(byte type) {
        for (RealTimeType t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown message type : " + type);
    }

}
