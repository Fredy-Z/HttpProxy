package org.http.proxy.utils;

import java.io.*;

import org.apache.commons.lang.ArrayUtils;

public class GarUtils {

	public static byte[] transfer(InputStream input, OutputStream output) throws IOException {
		return transfer(input, output, -1);
	}

	public static byte[] transfer(InputStream input, OutputStream output, long maxLength) throws IOException {
		byte[] total = {};
		if (maxLength < 0) {
			maxLength = Long.MAX_VALUE;
		}
		byte[] buffer = new byte[204800];
		int max = buffer.length;
		if (max > maxLength) {
			max = (int) maxLength;
		}
		int bufferSize;
		int loopTimes = 0;

		try {
			while ((bufferSize = input.read(buffer, 0, max)) >= 0) {
				buffer = ArrayUtils.subarray(buffer, 0, bufferSize);
				loopTimes++;
				if (loopTimes == 1) {
					total = buffer;
				} else {
					total = ArrayUtils.addAll(total, buffer);
				}

				output.write(buffer, 0, bufferSize);
				maxLength -= bufferSize;
				if (maxLength <= 0) {
					break;
				} else {
					if (max > maxLength) {
						max = (int) maxLength;
					}
				}
				buffer = new byte[204800];
			}
		} catch (EOFException e) {
			return total;
		}
		return total;
	}
}
