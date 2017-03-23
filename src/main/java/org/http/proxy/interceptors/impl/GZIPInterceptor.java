package org.http.proxy.interceptors.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.http.proxy.ConstantsAware;
import org.http.proxy.ProxyThread;
import org.http.proxy.interceptors.IProxyInterceptor;
import org.http.proxy.utils.GarUtils;
import org.http.proxy.utils.HttpHeaderUtil;
import org.http.proxy.utils.MyByteArrayOutputStream;

public class GZIPInterceptor implements IProxyInterceptor, ConstantsAware {
	Logger logger = Logger.getLogger(GZIPInterceptor.class);

	private static final int MaxSize = 1024 * 1024 * 2;
	private static final char CR = '\r';
	private static final char LF = '\n';

	@Override
	public void on(ProxyThread thread) {
		try {
			String responseCodeStr = thread.responseFirstLine[1];
			if (responseCodeStr != null && !responseCodeStr.equals("") && StringUtils.isNumeric(responseCodeStr)) {
				int code = Integer.parseInt(responseCodeStr);
				if (code == 200) {
					String contentLength = HttpHeaderUtil.getHeaderValue(thread.responseHeaders, ContentLength);
					String contentEncoding = HttpHeaderUtil.getHeaderValue(thread.responseHeaders, ContentEncoding);
					String transferEncoding = HttpHeaderUtil.getHeaderValue(thread.responseHeaders, TransferEncoding);
					if (thread.responseInput != null) {
						// is normal gzip
						if ("gzip".equalsIgnoreCase(contentEncoding) && !(transferEncoding != null && transferEncoding.equals("chunked"))) {
							// int length = Integer.parseInt(contentLength);
							// if (length <= MaxSize) {
							MyByteArrayOutputStream buffer = thread.outputBuf;
							buffer.reset();
							int size = 0;
							GZIPInputStream input = null;
							try {
								input = new GZIPInputStream(thread.responseInput);
								size = GarUtils.transfer(input, buffer).length;
							} catch (Exception e) {
								logger.error("Wrong gzip format data.");
								logger.error(e.getMessage());
								logger.error(thread.uriToks[1]);
								logger.error(thread.requestFirstLine[1]);
							} finally {
								if (input != null)
									input.close();
							}
							thread.responseInput = new ByteArrayInputStream(buffer.getBuffer(), 0, size);
							HttpHeaderUtil.removeHeader(thread.responseHeaders, ContentEncoding);
							HttpHeaderUtil.setHeader(thread.responseHeaders, ContentLength, size);

							// is chunked
						} else if (transferEncoding != null && transferEncoding.equals("chunked")) {
							int totalSize = 0;
							byte[] unchunkedDataBuffer = new byte[MaxSize];
							int offset = 0;
							int chunkSize = 0;

							InputStream chunkedInput = thread.responseInput;

							chunkSize = readChunkSize(chunkedInput);
							while (chunkSize != 0 && (chunkSize != -1)) {
								totalSize += chunkSize;
								readChunkData(chunkedInput, unchunkedDataBuffer, offset, chunkSize);
								offset += chunkSize;
								// read CRLF
								int cr = chunkedInput.read();
								int lf = chunkedInput.read();

								chunkSize = readChunkSize(chunkedInput);
							}
							ByteArrayInputStream unchunkedDataBufferStream = new ByteArrayInputStream(unchunkedDataBuffer, 0, totalSize);

							if ("gzip".equalsIgnoreCase(contentEncoding)) {
								transferGZIPInputStream(unchunkedDataBufferStream, thread);
							} else {
								MyByteArrayOutputStream buffer = thread.outputBuf;
								buffer.reset();
								int size;
								try {
									size = GarUtils.transfer(unchunkedDataBufferStream, buffer).length;
								} finally {
									if (unchunkedDataBufferStream != null)
										unchunkedDataBufferStream.close();
								}
								thread.responseInput = new ByteArrayInputStream(buffer.getBuffer(), 0, size);
								HttpHeaderUtil.removeHeader(thread.responseHeaders, TransferEncoding);
								HttpHeaderUtil.setHeader(thread.responseHeaders, ContentLength, size);
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void transferGZIPInputStream(InputStream gZIPInputStream, ProxyThread proxyThread) throws IOException {
		MyByteArrayOutputStream buffer = proxyThread.outputBuf;
		buffer.reset();
		int size;
		GZIPInputStream input = null;
		try {
			input = new GZIPInputStream(gZIPInputStream);
			size = GarUtils.transfer(input, buffer).length;
		} finally {
			if (input != null)
				input.close();
		}
		proxyThread.responseInput = new ByteArrayInputStream(buffer.getBuffer(), 0, size);
		HttpHeaderUtil.removeHeader(proxyThread.responseHeaders, ContentEncoding);
		HttpHeaderUtil.removeHeader(proxyThread.responseHeaders, TransferEncoding);
		HttpHeaderUtil.setHeader(proxyThread.responseHeaders, ContentLength, size);
	}

	private int readChunkSize(InputStream input) throws IOException {
		byte[] chunkSizeBuffer = new byte[1024];
		int b;
		int index = 0;
		while ((b = input.read()) != -1) {
			// end of chunk
			if (b == 0)
				return 0;

			if (b != CR) {
				chunkSizeBuffer[index] = (byte) b;
				index++;
			} else {
				// read the extra LF character
				input.read();
				String chunkSizeStr = new String(chunkSizeBuffer, 0, index, "US-ASCII");
				int chunkSize = Integer.parseInt(chunkSizeStr.trim(), 16);
				return chunkSize;
			}
		}
		return -1;
	}

	private void readChunkData(InputStream input, byte[] unchunkedDataBuffer, int offset, int length) throws IOException {
		int readSize = 0;
		while (readSize < length) {
			readSize += input.read(unchunkedDataBuffer, offset + readSize, length - readSize);
		}
	}
}
