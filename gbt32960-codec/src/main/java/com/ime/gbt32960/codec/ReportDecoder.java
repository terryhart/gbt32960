package com.ime.gbt32960.codec;

import com.ime.iov.gbt32960.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import static com.ime.gbt32960.codec.GBT32960Message.*;

/**
 * @author Qingxi
 */

@Slf4j
public class ReportDecoder {

    public static RealTimeReport.Builder decodeFully(ByteBuf in) {
        RealTimeReport.Builder builder = RealTimeReport.newBuilder();
        long recordTime = readTime(in);
        builder.setRecordTime(recordTime);
        while (in.readableBytes() > 0) {
            RealTimeType type = RealTimeType.valueOf(in.readByte());
            switch (type) {
                case VEHICLE:
                    builder.setVehicleState(decodeVehicleState(in));
                    break;
                case MOTOR:
                    int count = in.readByte();
                    MotorState motorState;
                    for (int i = 0; i < count; i++) {
                        builder.addMotor(decodeMotor(in));
                    }
                    break;
                case FUEL_CELL:
                    builder.setFuelCell(decodeFuelCell(in));
                    break;
                case ENGINE:
                    builder.setEngine(decodeEngine(in));
                    break;
                case LOCATION:
                    builder.setLocation(decodeVehicleLocation(in));
                    break;
                case EXTREMUM:
                    builder.setExtremum(decodeExtremum(in));
                    break;
                case ALARM:
                    builder.setAlarm(decodeAlarm(in));
                    break;
                case BATTERY_VOLTAGE:
                    int elecCount = in.readByte();
                    for (int i = 0; i < elecCount; i++) {
                        builder.addChargeSystemElectric(decodeChargeableSubsystemElectric(in));
                    }
                    break;
                case BATTERY_TEMPERATURE:
                    int tempCount = in.readByte();
                    ChargeableSubsystemTemperature temperature;
                    for (int i = 0; i < tempCount; i++) {
                        builder.addChargeSystemTemperature(decodeChargeableSubsystemTemperature(in));
                    }
                    break;
                    default:
                        break;
            }
        }
        return builder;
    }



    /**
     * 解析整车数据
     * @param in ByteBuf
     * @return 整车数据
     */
    private static VehicleState decodeVehicleState(ByteBuf in) {
        VehicleState.Builder builder = VehicleState.newBuilder()
                .setOperatingStateValue(in.readByte())
                .setChargingStateValue(in.readByte())
                .setOperationModeValue(in.readByte())
                .setSpeed((in.readShort() /10.0f))
                .setMileage(in.readInt() / 10.0)
                .setVoltage(in.readShort() / 10.0f)
                .setCurrent(in.readShort() / 10.0f - 1000)
                .setStateOfCharge(in.readByte())
                .setDcInverterStateValue(in.readByte())
                .setGearPosition(in.readByte())
                .setInsulance(in.readShort())
                .setAcceleratorTravel(in.readByte())
                .setBrakeTravel(in.readByte());
        return builder.build();
    }

    /**
     * 解析驱动电机数据
     * @param in ByteBuf
     * @return 驱动电机数据
     */
    private static MotorState decodeMotor(ByteBuf in) {
        MotorState.Builder builder = MotorState.newBuilder()
                .setMotorSeq(in.readByte())
                .setStatusValue(in.readByte())
                .setControllerTemperature(in.readByte() - 40)
                .setMotorSpeed(in.readUnsignedShort() - 20000)
                .setMotorTorque(in.readUnsignedShort() / 10.0f - 2000)
                .setMotorTemperature(in.readByte() - 40)
                .setControllerVoltage(in.readUnsignedShort() / 10.0f)
                .setControllerCurrent(in.readShort() / 10.0f - 1000);
        return builder.build();
    }


    /**
     * 解析燃料电池数据
     * @param in ByteBuf
     * @return 燃料电池数据
     */
    private static FuelCell decodeFuelCell(ByteBuf in) {
        FuelCell.Builder builder = FuelCell.newBuilder()
                .setFuelCellVoltage(in.readShort() / 10.0)
                .setFuelCellCurrent(in.readShort() / 10.0)
                .setFuelConsumptionRate(in.readShort() / 100.0);

        short number = in.readShort();
        if (number < 0xFFFE) {
            for (int i = 0; i < number; i++) {
                builder.addProbeTemperatureValue(in.readByte() - 40);
            }
        }
        builder.setHighestTempOfHydrogenSystem(in.readShort() / 10.0 - 40)
                .setHighestTempProbeCodeOfHydSys(in.readByte())
                .setHighestConOfHydrogen(in.readShort())
                .setHighestHyConSensorCode(in.readByte())
                .setHydrogenMaxPressure(in.readShort() / 10.0)
                .setHydrogenMaxPressureSensorCode(in.readByte())
                .setHighVoltageDcStateValue(in.readByte());
        return builder.build();
    }

    /**
     * 解析发动机数据
     * @param in ByteBuf
     * @return 发动机数据
     */
    private static Engine decodeEngine(ByteBuf in) {
        Engine.Builder builder = Engine.newBuilder()
                .setEngineStateValue(in.readByte())
                .setCrankshaftSpeed(in.readShort())
                .setFuelConsumptionRate(in.readShort() / 100.0);
        return builder.build();
    }

    /**
     * 解析车辆位置数据
     * @param in ByteBuf
     * @return 车辆位置数据
     */
    private static VehicleLocation decodeVehicleLocation(ByteBuf in) {
        byte mark = in.readByte();
        VehicleLocation.Builder builder = VehicleLocation.newBuilder()
                .setIsValid((mark & 1) == 0)
                .setLongitude(in.readUnsignedInt() / 1000000.0 * ((mark >> 2 & 1) == 1 ? -1 : 1))
                .setLatitude(in.readUnsignedInt() / 1000000.0 * ((mark >> 1 & 1) == 1 ? -1 : 1));
        return builder.build();
    }

    /**
     * 解析极值数据
     * @param in ByteBuf
     * @return 极值数据
     */
    private static Extremum decodeExtremum(ByteBuf in) {
        Extremum.Builder builder = Extremum.newBuilder()
                .setVoltageMaxSubsystem(in.readByte())
                .setVoltageMaxBattery(in.readByte())
                .setMaxVoltage(in.readShort() / 1000.0f)
                .setVoltageMinSubsystem(in.readByte())
                .setVoltageMinBattery(in.readByte())
                .setMinVoltage(in.readShort() / 1000.0f)
                .setTemperatureMaxSubsystem(in.readByte())
                .setTemperatureMaxProbe(in.readByte())
                .setMaxTemperature(in.readByte())
                .setTemperatureMinSubsystem(in.readByte())
                .setTemperatureMinProbe(in.readByte())
                .setMinTemperature(in.readByte());
        return builder.build();
    }

    /**
     * 解析报警数据
     * @param in ByteBuf
     * @return 报警数据
     */
    private static Alarm decodeAlarm(ByteBuf in) {
        Alarm.Builder builder = Alarm.newBuilder()
                .setMaxAlarmLevel(in.readByte())
                .setAlarmBitIdentify(in.readInt());

        byte batteryFaultCount = in.readByte();
        if (batteryFaultCount < 0xFE) {
            for (int i = 0; i < batteryFaultCount; i++) {
                builder.addBatteryFaultData(in.readInt());
            }
        }

        byte motorFaultCount = in.readByte();
        if (motorFaultCount < 0xFE) {
            for (int i = 0; i < motorFaultCount; i++) {
                builder.addMotorFaultData(in.readInt());
            }
        }

        byte engineFaultCount = in.readByte();
        if (engineFaultCount < 0xFE) {
            for (int i = 0; i < engineFaultCount; i++) {
                builder.addEngineFaultData(in.readInt());
            }
        }

        byte otherFaultCount = in.readByte();
        if (otherFaultCount < 0xFE) {
            for (int i = 0; i < otherFaultCount; i++) {
                builder.addOtherFaultData(in.readInt());
            }
        }

        return builder.build();
    }

    /**
     * 解析单个可充电储能电压数据
     * @param in ByteBuf
     * @return 单个可充电储能电压数据
     */
    private static ChargeableSubsystemElectric decodeChargeableSubsystemElectric(ByteBuf in) {
        ChargeableSubsystemElectric.Builder builder =
                ChargeableSubsystemElectric.newBuilder()
                .setChargeableSubSystemNumber(in.readByte())
                .setVoltage(in.readShort() / 10.0f)
                .setCurrent(in.readShort() / 10.0f - 1000)
                .setBatteryTotalCount(in.readUnsignedShort())
                .setFrameStartBatterySeq(in.readUnsignedShort());
        int count = in.readByte();
        for (int i = 0; i < count; i++) {
            builder.addBatteryVoltage(in.readUnsignedShort() / 1000.0f);
        }
        return builder.build();
    }

    /**
     * 解析单个可充电储能装置温度数据
     * @param in ByteBuf
     * @return 单个可充电储能装置温度数据
     */
    private static ChargeableSubsystemTemperature decodeChargeableSubsystemTemperature(ByteBuf in) {
        ChargeableSubsystemTemperature.Builder builder =
                ChargeableSubsystemTemperature.newBuilder()
                .setSubSystemNumber(in.readByte());
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            builder.addProbeTemperature(in.readByte() - 40);
        }
        return builder.build();
    }

}
