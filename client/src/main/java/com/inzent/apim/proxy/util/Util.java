package com.inzent.apim.proxy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

public class Util {
	/**
	 * 허용 아이피를 설정한다.
	 * 
	 * @param remoteIP
	 * @param ip
	 * @return
	 */
	public static boolean allowIP(HttpServletRequest request, String ip) {
		String remoteIP = getIp(request);
		StringTokenizer st_remoteIP = new StringTokenizer(remoteIP, ".");
		String remote_ip1 = "";
		String remote_ip2 = "";
		String remote_ip3 = "";
		String remote_ip4 = "";

		// 접속한 IP가 정상적인 IP 인지를 체크.
		if (st_remoteIP.countTokens() == 4) {
			remote_ip1 = st_remoteIP.nextToken();
			remote_ip2 = st_remoteIP.nextToken();
			remote_ip3 = st_remoteIP.nextToken();
			remote_ip4 = st_remoteIP.nextToken();
		}
		// 로컬호스트는 ip 6형태로 나타남.
		else
			return true;

		ip = ip.replaceAll(" ", ""); // 공백제거.
		// System.out.println("context.property에서 불러온 블랙리스트=>" + blackIP);
		StringTokenizer st1 = new StringTokenizer(ip, ";");
		List<String> al_ip1 = new ArrayList<String>(); // 10.1.*.*
		List<String> al_ip2 = new ArrayList<String>(); // 10.1.61.*
		List<String> al_ip3 = new ArrayList<String>(); // 10.1.20..120

		// 설정된 블랙리스트 IP를 뒤져가며 접근한 IP가 블랙리스트에 포함되는지를 판단.

		while (st1.hasMoreTokens()) {
			String ip1 = st1.nextToken();
			StringTokenizer st2 = new StringTokenizer(ip1, ".");

			if (remoteIP.equals(ip1))
				return true;

			// balck list IP 형식이 정상일때...
			if (st2.countTokens() == 4) {
				String mask1 = st2.nextToken();
				String mask2 = st2.nextToken();
				String mask3 = st2.nextToken();
				String mask4 = st2.nextToken();
				// System.out.println(mask1 + "." + mask2 + "." + mask3 + "." + mask4);

				// 10.1.*.* 형식의 아이피
				if (!"*".equals(mask1) && !"*".equals(mask2) && "*".equals(mask3) && "*".equals(mask4)) {
					al_ip1.add(ip1);
				}
				// 10.1.61.* 형식의 아이피
				else if (!"*".equals(mask1) && !"*".equals(mask2) && !"*".equals(mask3) && "*".equals(mask4)) {
					al_ip2.add(ip1);
				}
				// 10.1.6.102 형식의 아이피
				else if (!"*".equals(mask1) && !"*".equals(mask2) && !"*".equals(mask3) && !"*".equals(mask4)) {
					al_ip3.add(ip1);
				}

				// 10.1.*.* 형식의 블랙리스트가 접근한 IP와 일치하는지를 비교.
				for (int i = 0; i < al_ip1.size(); i++) {
					String i1 = (String) al_ip1.get(i);
					StringTokenizer st_comp1 = new StringTokenizer(i1, ".");
					String ip_comp1 = st_comp1.nextToken(); // A클래스
					String ip_comp2 = st_comp1.nextToken(); // B클래스
					// 접근한 IP가 블랙리스트의 A,B클래스와 일치... 즉 접근한 IP는 블랙리스트에 포함됨
					if (ip_comp1.equals(remote_ip1) && ip_comp2.equals(remote_ip2)) {
						return true;
					}
				}
				al_ip1.clear();

				// 10.1.61.* 형식의 블랙리스트가 접근한 IP와 일치하는지를 비교.
				for (int i = 0; i < al_ip2.size(); i++) {
					String i2 = (String) al_ip2.get(i);
					StringTokenizer st_comp2 = new StringTokenizer(i2, ".");
					String ip_comp1 = st_comp2.nextToken();
					String ip_comp2 = st_comp2.nextToken();
					String ip_comp3 = st_comp2.nextToken();
					// 접근한 IP가 블랙리스트의 A,B,C클래스와 일치... 즉 접근한 IP는 블랙리스트에 포함됨
					if (ip_comp1.equals(remote_ip1) && ip_comp2.equals(remote_ip2) && ip_comp3.equals(remote_ip3)) {
						return true;
					}
					al_ip2.clear();
				}

				// 10.1.61.1 형식의 블랙리스트가 접근한 IP와 일치하는지를 비교.
				for (int i = 0; i < al_ip3.size(); i++) {
					String i3 = (String) al_ip3.get(i);
					StringTokenizer st_comp3 = new StringTokenizer(i3, ".");
					String ip_comp1 = st_comp3.nextToken();
					String ip_comp2 = st_comp3.nextToken();
					String ip_comp3 = st_comp3.nextToken();
					String ip_comp4 = st_comp3.nextToken();
					// 접근한 IP가 블랙리스트의 A,B,C,D클래스와 일치... 즉 접근한 IP는 블랙리스트에 포함됨
					if (ip_comp1.equals(remote_ip1) && ip_comp2.equals(remote_ip2) && ip_comp3.equals(remote_ip3)
							&& ip_comp4.equals(remote_ip4)) {
						return true;
					}
					al_ip3.clear();
				}
			} // if(st2.countTokens() == 4 ) {
		} // while(st1.hasMoreTokens()) {

		return false;
	}

	public static String getIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");

		if (ip == null) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null) {
			ip = request.getHeader("WL-Proxy-Client-IP"); // 웹로직
		}
		if (ip == null) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null) {
			ip = request.getRemoteAddr();
		}

		return ip;
	}
}
