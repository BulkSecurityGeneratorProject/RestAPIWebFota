package com.hillrom.vest.service.util.FOTA;

import static com.hillrom.vest.config.FOTA.FOTAConstants.CHUNK_SIZE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.CONNECTION_TYPE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.CONNECTION_TYPE_ONE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.CONNECTION_TYPE_ZERO;
import static com.hillrom.vest.config.FOTA.FOTAConstants.CRC;
import static com.hillrom.vest.config.FOTA.FOTAConstants.DEVICE_MODEL;
import static com.hillrom.vest.config.FOTA.FOTAConstants.DEVICE_PARTNUMBER;
import static com.hillrom.vest.config.FOTA.FOTAConstants.DEVICE_SN;
import static com.hillrom.vest.config.FOTA.FOTAConstants.DEVICE_VER;
import static com.hillrom.vest.config.FOTA.FOTAConstants.HANDLE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.ONE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.PREV_REQ_STATUS;
import static com.hillrom.vest.config.FOTA.FOTAConstants.REQUEST_TYPE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.RESULT;
import static com.hillrom.vest.config.FOTA.FOTAConstants.SOFT_VER_DATE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.ZERO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FOTAParseUtil {

	private static final Logger log = LoggerFactory
			.getLogger(FOTAParseUtil.class);

	public static Map<String, String> getFOTAJsonDataFromRawMessage(
			String rawMessage) {

		Map<String, String> fotaJsonData = new LinkedHashMap<String, String>();

		String devModel = rawMessage.indexOf(DEVICE_MODEL) < 0 ? null
				: rawMessage.substring(rawMessage.indexOf(DEVICE_MODEL)
						+ DEVICE_MODEL.length() + 1,
						getNextIndex(rawMessage, DEVICE_MODEL));
		String requestType = rawMessage.indexOf(REQUEST_TYPE) < 0 ? null
				: rawMessage.substring(rawMessage.indexOf(REQUEST_TYPE)
						+ REQUEST_TYPE.length() + 1,
						getNextIndex(rawMessage, REQUEST_TYPE));
		String devSn = rawMessage.indexOf(DEVICE_SN) < 0 ? null : rawMessage
				.substring(rawMessage.indexOf(DEVICE_SN) + DEVICE_SN.length()
						+ 1, getNextIndex(rawMessage, DEVICE_SN));
		String connType = rawMessage.indexOf(CONNECTION_TYPE) < 0 ? null
				: rawMessage.substring(rawMessage.indexOf(CONNECTION_TYPE)
						+ CONNECTION_TYPE.length() + 1,
						getNextIndex(rawMessage, CONNECTION_TYPE));
		String devPartNumber = rawMessage.indexOf(DEVICE_PARTNUMBER) < 0 ? null
				: rawMessage.substring(rawMessage.indexOf(DEVICE_PARTNUMBER)
						+ DEVICE_PARTNUMBER.length() + 1,
						getNextIndex(rawMessage, DEVICE_PARTNUMBER));
		String devVer = rawMessage.indexOf(DEVICE_VER) < 0 ? null : rawMessage
				.substring(rawMessage.indexOf(DEVICE_VER) + DEVICE_VER.length()
						+ 1, getNextIndex(rawMessage, DEVICE_VER));
		String softVerDate = rawMessage.indexOf(SOFT_VER_DATE) < 0 ? null
				: rawMessage.substring(rawMessage.indexOf(SOFT_VER_DATE)
						+ SOFT_VER_DATE.length() + 1,
						getNextIndex(rawMessage, SOFT_VER_DATE));
		String chunkSize = rawMessage.indexOf(CHUNK_SIZE) < 0 ? null
				: rawMessage.substring(rawMessage.indexOf(CHUNK_SIZE)
						+ CHUNK_SIZE.length() + 1,
						getNextIndex(rawMessage, CHUNK_SIZE));
		log.debug("rawMessage:" + rawMessage);
		log.debug("HANDLE:" + HANDLE);
		String handle = rawMessage.indexOf(HANDLE) < 0 ? null : rawMessage
				.substring(rawMessage.indexOf(HANDLE) + HANDLE.length() + 1,
						getNextIndex(rawMessage, HANDLE));
		log.debug("handle:" + handle);
		String prevReqStatus = rawMessage.indexOf(PREV_REQ_STATUS) < 0 ? null
				: rawMessage.substring(rawMessage.indexOf(PREV_REQ_STATUS)
						+ PREV_REQ_STATUS.length() + 1,
						getNextIndex(rawMessage, PREV_REQ_STATUS));
		String result = rawMessage.indexOf(RESULT) < 0 ? null : rawMessage
				.substring(rawMessage.indexOf(RESULT) + RESULT.length() + 1,
						getNextIndex(rawMessage, RESULT));
		String devCrc = rawMessage.indexOf(CRC) < 0 ? null : rawMessage
				.substring(rawMessage.indexOf(CRC) + CRC.length() + 1,
						rawMessage.length());

		if (Objects.nonNull(devModel)) {
			fotaJsonData.put(DEVICE_MODEL, devModel);
		}
		if (Objects.nonNull(requestType)) {
			fotaJsonData.put(REQUEST_TYPE, requestType);
		}
		if (Objects.nonNull(connType)) {
			fotaJsonData.put(DEVICE_SN, devSn);
		}
		if (Objects.nonNull(connType)) {
			if (connType.equals(ZERO)) {
				fotaJsonData.put(CONNECTION_TYPE, CONNECTION_TYPE_ZERO);
			} else if (connType.equals(ONE)) {
				fotaJsonData.put(CONNECTION_TYPE, CONNECTION_TYPE_ONE);
			}

		}

		if (Objects.nonNull(connType)) {
			fotaJsonData.put(DEVICE_PARTNUMBER, devPartNumber);
		}
		if (Objects.nonNull(devVer)) {
			fotaJsonData.put(DEVICE_VER, devVer);
		}
		if (Objects.nonNull(softVerDate)) {
			fotaJsonData.put(SOFT_VER_DATE, softVerDate);
		}
		if (Objects.nonNull(chunkSize)) {
			fotaJsonData.put(CHUNK_SIZE, chunkSize);
		}
		if (Objects.nonNull(prevReqStatus)) {
			fotaJsonData.put(PREV_REQ_STATUS, prevReqStatus);
		}
		if (Objects.nonNull(handle)) {
			fotaJsonData.put(HANDLE, handle);
		}
		if (Objects.nonNull(result)) {
			fotaJsonData.put(RESULT, result);
		}

		if (Objects.nonNull(devCrc)) {
			fotaJsonData.put(CRC, devCrc);
		}

		return fotaJsonData;
	}

	private static int getNextIndex(String rawMessage, String namePair) {

		switch (namePair) {
		case DEVICE_MODEL:
			int i = rawMessage.indexOf(REQUEST_TYPE) < 0 ? getNextIndex(
					rawMessage, REQUEST_TYPE) : rawMessage
					.indexOf(REQUEST_TYPE) - 1;
			return i;
		case REQUEST_TYPE:
			return rawMessage.indexOf(DEVICE_SN) < 0 ? getNextIndex(rawMessage,
					DEVICE_SN) : rawMessage.indexOf(DEVICE_SN) - 1;
		case DEVICE_SN:
			return rawMessage.indexOf(CONNECTION_TYPE) < 0 ? getNextIndex(
					rawMessage, CONNECTION_TYPE) : rawMessage
					.indexOf(CONNECTION_TYPE) - 1;
		case CONNECTION_TYPE:
			return rawMessage.indexOf(DEVICE_PARTNUMBER) < 0 ? getNextIndex(
					rawMessage, DEVICE_PARTNUMBER) : rawMessage
					.indexOf(DEVICE_PARTNUMBER) - 1;
		case DEVICE_PARTNUMBER:
			return rawMessage.indexOf(DEVICE_VER) < 0 ? getNextIndex(
					rawMessage, DEVICE_VER)
					: rawMessage.indexOf(DEVICE_VER) - 1;
		case DEVICE_VER:
			return rawMessage.indexOf(SOFT_VER_DATE) < 0 ? getNextIndex(
					rawMessage, SOFT_VER_DATE) : rawMessage
					.indexOf(SOFT_VER_DATE) - 1;
		case SOFT_VER_DATE:
			return rawMessage.indexOf(CHUNK_SIZE) < 0 ? getNextIndex(
					rawMessage, CHUNK_SIZE)
					: rawMessage.indexOf(CHUNK_SIZE) - 1;
		case CHUNK_SIZE:
			return rawMessage.indexOf(HANDLE) < 0 ? getNextIndex(rawMessage,
					HANDLE) : rawMessage.indexOf(HANDLE) - 1;
		case HANDLE:
			return rawMessage.indexOf(PREV_REQ_STATUS) < 0 ? getNextIndex(
					rawMessage, PREV_REQ_STATUS) : rawMessage
					.indexOf(PREV_REQ_STATUS) - 1;
		case PREV_REQ_STATUS:
			return rawMessage.indexOf(RESULT) < 0 ? getNextIndex(rawMessage,
					RESULT) : rawMessage.indexOf(RESULT) - 1;
		case RESULT:
			return rawMessage.indexOf(CRC) < 0 ? rawMessage.length()
					: rawMessage.indexOf(CRC) - 1;
		default:
			return -1;
		}

	}

	public static int hex2decimal(String chunkStr) {

		String digits = "0123456789ABCDEF";
		chunkStr = chunkStr.toUpperCase();
		int val = 0;
		for (int i = 0; i < chunkStr.length(); i++) {
			char c = chunkStr.charAt(i);
			int d = digits.indexOf(c);
			val = 16 * val + d;
		}
		return val;
	}

	public static String getBufferLenTwoHexByte(int bufferLen) {
		// Convert to hex
		String bufferLenHex = Integer.toHexString(bufferLen);
		// convert in two byte format
		bufferLenHex = ("0000" + bufferLenHex).substring(bufferLenHex.length());
		// converting to little Endian
		String bufferInLsb = hexToAscii(asciiToHex(toLittleEndian((bufferLenHex))));
		return bufferInLsb;
	}

	// Convert String to ASCII
	public static String asciiToHex(String asciiValue) {
		char[] chars = asciiValue.toCharArray();
		StringBuffer hex = new StringBuffer();
		for (int i = 0; i < chars.length; i++) {
			hex.append(Integer.toHexString((int) chars[i]));
		}
		return hex.toString();

	}

	// Make it ready for raw data
	public static String hexToAscii(String hexStr) {
		String str = "";
		StringBuilder output = new StringBuilder("");
		try {
			for (int i = 0; i < hexStr.length(); i += 2) {
				str = hexStr.substring(i, i + 2);
				output.append((char) Integer.parseInt(str, 16));
			}
			System.out.println(output);

		} catch (Exception ex) {

		}
		return new String(output.toString());
	}

	public static String toLittleEndian(final String hex) {
		// int ret = 0;
		String hexLittleEndian = "";
		if (hex.length() % 2 != 0)
			return hexLittleEndian;
		for (int i = hex.length() - 2; i >= 0; i -= 2) {
			hexLittleEndian += hex.substring(i, i + 2);
		}
		return hexLittleEndian;
	}

}
