package com.inzent.apim.proxy.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class MakeCertUtil {
	public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";

	public static final String END_CERT = "-----END CERTIFICATE-----";

	public final static String LINE_SEPARATOR = System.getProperty("line.separator");

	public static String formatCrtFileContents(final Certificate certificate) throws CertificateEncodingException {
		final Base64.Encoder encoder = Base64.getMimeEncoder(	64,
																LINE_SEPARATOR.getBytes());

		final byte[] rawCrtText = certificate.getEncoded();
		final String encodedCertText = new String(encoder.encode(rawCrtText));
		final String prettified_cert = BEGIN_CERT + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_CERT;
		return prettified_cert;
	}

	public static void executeCmd(	String command,
									File dir)	throws InterruptedException,
												IOException {
		String[] splitter = command.split(" ");
		List<String> commands = Arrays.asList(splitter);

		Process process = new ProcessBuilder(commands)	.directory(dir)
														.inheritIO()
														.start();
		// checkState(process.waitFor() == 0);
		int ret = process.waitFor();
	}

	public static void createFile(	X509Certificate cert,
									File dir)	throws CertificateEncodingException,
												FileNotFoundException,
												UnsupportedEncodingException {
		String str = formatCrtFileContents(cert);
		PrintWriter writer = new PrintWriter(	dir.getAbsolutePath()	+ "/newCert.crt",
												"UTF-8");
		writer.println(str);
		writer.close();
	}
}
