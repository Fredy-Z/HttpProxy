package org.http.proxy.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.http.proxy.ConstantsAware;
import org.http.proxy.models.KeyValuePair;

public class HttpHeaderUtil implements ConstantsAware {

	public static void sendHeaders(OutputStream output, List<KeyValuePair<String, String>> pairs, String encoding) throws IOException {
		for (KeyValuePair<String, String> pair : pairs) {
			output.write(pair.getKey().getBytes(encoding));
			String value = pair.getValue();
			if (value != null) {
				output.write(':');
				output.write(' ');
				output.write(value.getBytes(encoding));
			}
			output.write(CRLF.getBytes(encoding));
		}
	}

	public static boolean setHeader(List<KeyValuePair<String, String>> headerList, String name, Object value) {
		boolean find = false;
		for (KeyValuePair<String, String> pair : headerList) {
			if (pair.getKey().equalsIgnoreCase(name)) {
				pair.setValue(value.toString());
				find = true;
			}
		}
		if (!find) {
			headerList.add(new KeyValuePair<String, String>(name, value.toString()));
		}
		return find;
	}

	public static void parseHeaders(List<KeyValuePair<String, String>> headers, List<String> lines) {
		headers.clear();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			int index = line.indexOf(':');
			if (index > 0) {
				headers.add(new KeyValuePair<String, String>(line.substring(0, index), line.substring(index + 1).trim()));
			} else {
				headers.add(new KeyValuePair<String, String>(line, null));
			}
		}
	}

	public static int removeHeader(List<KeyValuePair<String, String>> headerList, String name) {
		int count = 0;
		for (Iterator<KeyValuePair<String, String>> it = headerList.iterator(); it.hasNext();) {
			if (it.next().getKey().equalsIgnoreCase(name)) {
				it.remove();
				count++;
			}
		}
		return count;
	}

	public static String getHeaderValue(List<KeyValuePair<String, String>> headerList, String name) {
		for (KeyValuePair<String, String> pair : headerList) {
			if (pair.getKey().equalsIgnoreCase(name)) {
				String value = pair.getValue();
				if (value != null) {
					return value;
				}
			}
		}
		return null;
	}

	public static void readHeadersIntoList(InputStream input, String encoding, MyByteArrayOutputStream outputBuffer, List<String> lines)
			throws IOException {
		outputBuffer.reset();
		lines.clear();
		int n;
		int start = 0;
		while ((n = input.read()) != -1) {
			outputBuffer.write(n);
			if (n == '\r') {
				n = input.read();
				outputBuffer.write(n);
				if (n == '\n') {
					int end = outputBuffer.size() - 1;
					byte[] buffer = outputBuffer.getBuffer();
					String line = new String(buffer, start, end - start).trim();
					System.out.println(line);
					if (line.equals("")) {
						break;
					}
					lines.add(line);
					start = end;
				}
			}
		}
	}
}
