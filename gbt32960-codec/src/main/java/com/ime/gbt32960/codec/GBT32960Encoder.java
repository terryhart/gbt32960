package com.ime.gbt32960.codec;

import com.google.common.base.Preconditions;
import com.ime.iov.gbt32960.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;


import java.time.Instant;

import static com.ime.gbt32960.codec.GBT32960Message.*;

/**
 * @author Qingxi
 */

@Slf4j
public class GBT32960Encoder extends MessageToByteEncoder<ResponseMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseMessage response, ByteBuf out) {
        PlatformMessage message = response.getMessage();
        switch (message.getMessageCase()) {
            case CONFIG_QUERY:
                encodeCommand(out, response.getVin(), RequestType.CONFIG_QUERY, buf -> encodeConfigQuery(message.getConfigQuery(), buf));
                break;
            case CONFIG_SETUP:
                encodeCommand(out, response.getVin(), RequestType.CONFIG_SETUP, buf -> encodeConfigSetup(message.getConfigSetup(), buf));
                break;
            case CONTROL:
                encodeCommand(out, response.getVin(), RequestType.CONTROL, buf -> encodeControl(message.getControl(), buf));
                break;
            case UPGRADE:
                encodeCommand(out, response.getVin(), RequestType.CONTROL, buf -> encodeUpgrade(message.getUpgrade(), buf));
                break;
            case WARNING:
                encodeCommand(out, response.getVin(), RequestType.CONTROL, buf -> encodeWarning(message.getWarning(), buf));
                break;
            case CLOCK_CORRECT:
                encodeMessage(out, response.getVin(), RequestType.CLOCK_CORRECT, ResponseTag.SUCCESS,
                        buf -> writeTime(buf, message.getClockCorrect().getSystemTime()));
                break;
            case EMPTY_RESPONSE:
                EmptyResponse emptyResponse = message.getEmptyResponse();
                encodeMessage(out, response.getVin(), RequestType.valueOf((byte) emptyResponse.getMesssageType()),
                        ResponseTag.valueOf((byte) emptyResponse.getResult()), buf -> {
                            if (emptyResponse.getHasTime()) {
                                writeTime(buf, emptyResponse.getTime());
                            }
                        });
                break;
            case PROTO_RESPONSE:
                ProtoResponse protoResponse = message.getProtoResponse();
                LoginRequest login = protoResponse.getLogin();
                encodeMessage(out, response.getVin(), RequestType.valueOf((byte) protoResponse.getMesssageType()),
                        ResponseTag.valueOf((byte) protoResponse.getResult()), buf -> {
                            encodeLoginWithCurrentTime(login, buf);
                        });
                break;
                default:
                    log.error("未处理的平台消息类型: {}", message.getMessageCase());
                    break;
        }
        log.info("应答帧: {}", ByteBufUtil.hexDump(out));
    }

    private void encodeLoginWithCurrentTime(LoginRequest request, ByteBuf out) {
        writeTime(out, Instant.now());
        out.writeShort(request.getLoginDaySeq());
        out.writeBytes(request.getIccid().getBytes(ASCII_CHARSET));
        out.writeByte(request.getChargeableSubsystemCodeCount());
        out.writeByte(request.getSystemCodeLength());
        request.getChargeableSubsystemCodeList().forEach(
                code -> out.writeBytes(code.getBytes(ASCII_CHARSET)));
    }


    private void encodeControl(ControlCommand control, ByteBuf out) {
        writeTime(out, control.getRequestTime());
        out.writeByte(control.getCommandValue());
    }

    private void encodeWarning(Warning warning, ByteBuf out) {
        writeTime(out, warning.getRequestTime());
        out.writeByte(CommandType.WARNING_VALUE);
        out.writeByte(warning.getLevel());
        out.writeBytes(warning.getContent().getBytes(CHINESE_CHARSET));
    }

    private void encodeUpgrade(RemoteUpgradeCommand upgrade, ByteBuf out) {
        writeTime(out, upgrade.getRequestTime());
        out.writeByte(CommandType.UPGRADE_VALUE);
        StringBuilder builder = new StringBuilder();
        builder.append(upgrade.getDialName()).append(";");
        builder.append(upgrade.getDialAccount()).append(";");
        builder.append(upgrade.getDialPassword()).append(";");
        builder.append(upgrade.getAddress()).append(";");
        builder.append(upgrade.getPort()).append(";");
        builder.append(upgrade.getTerminalManufacturerId()).append(";");
        builder.append(upgrade.getHardwareVersion()).append(";");
        builder.append(upgrade.getFirmwareVersion()).append(";");
        builder.append(upgrade.getUpgradeUrl()).append(";");
        builder.append(upgrade.getUpgradeTimeLimit());
        out.writeBytes(builder.toString().getBytes(CHINESE_CHARSET));
    }

    private void encodeConfigSetup(ConfigSetupRequest configSetupRequest, ByteBuf out) {
        writeTime(out, configSetupRequest.getSetupTime());
        out.writeByte(configSetupRequest.getParametersCount());
        configSetupRequest.getParametersList().forEach(p->{
            out.writeByte(p.getParameterCase().getNumber());
            switch (p.getParameterCase()) {
                case STORAGE_PERIOD:
                    out.writeShort(p.getStoragePeriod());
                    break;
                case NORMAL_REPORT_INTERVAL:
                    out.writeShort(p.getNormalReportInterval());
                    break;
                case ALARM_REPORT_INTERVAL:
                    out.writeShort(p.getAlarmReportInterval());
                    break;
                case MANAGEMENT_PLATFORM_DOMAIN_LENGTH:
                    out.writeByte(p.getManagementPlatformDomainLength());
                    break;
                case MANAGEMENT_PLATFORM_DOMAIN:
                    out.writeBytes(p.getManagementPlatformDomain().getBytes(CHINESE_CHARSET));
                    break;
                case MANAGEMENT_PLATFORM_PORT:
                    out.writeShort(p.getManagementPlatformPort());
                    break;
                case PUBLIC_PLATFORM_DOMAIN_LENGTH:
                    out.writeByte(p.getPublicPlatformDomainLength());
                    break;
                case PUBLIC_PLATFORM_DOMAIN:
                    out.writeBytes(p.getPublicPlatformDomain().getBytes(CHINESE_CHARSET));
                    break;
                case PUBLIC_PLATFORM_PORT:
                    out.writeShort(p.getPublicPlatformPort());
                    break;
                case HARDWARE_VERSION:
                    Preconditions.checkArgument(p.getHardwareVersion().length() == 5);
                    out.writeBytes(p.getHardwareVersion().getBytes(ASCII_CHARSET));
                    break;
                case FIRMWARE_VERSION:
                    Preconditions.checkArgument(p.getFirmwareVersion().length() == 5);
                    out.writeBytes(p.getFirmwareVersion().getBytes(ASCII_CHARSET));
                    break;
                case HEARBEAT_INTERVAL:
                    out.writeByte(p.getHearbeatInterval());
                    break;
                case TERMINAL_RESPONSE_TIMEOUT:
                    out.writeShort(p.getTerminalResponseTimeout());
                    break;
                case PLATFORM_RESPONSE_TIMEOUT:
                    out.writeShort(p.getPlatformResponseTimeout());
                    break;
                case LOGIN_RETRY_INTERVAL:
                    out.writeByte(p.getLoginRetryInterval());
                    break;
                case SAMPLING:
                    out.writeByte(p.getSamplingValue());
                    break;
                    default:
                        log.error("未处理的参数类型: {}", p.getParameterCase());
                        break;
            }
        });
    }

    private void encodeConfigQuery(ConfigQueryRequest configQueryRequest, ByteBuf out) {
        writeTime(out, configQueryRequest.getQueryTime());
        out.writeByte(configQueryRequest.getParameterIdsCount());
        configQueryRequest.getParameterIdsList().forEach(p->out.writeByte(p.byteValue()));
    }

}
