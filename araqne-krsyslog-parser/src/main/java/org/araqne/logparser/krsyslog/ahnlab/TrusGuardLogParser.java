/*
 * Copyright 2012 Future Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.araqne.logparser.krsyslog.ahnlab;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.araqne.log.api.V1LogParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrusGuardLogParser extends V1LogParser {
	private final Logger logger = LoggerFactory.getLogger(TrusGuardLogParser.class.getName());

	@Override
	public Map<String, Object> parse(Map<String, Object> params) {
		String line = (String) params.get("line");
		if (line == null)
			return null;

		String[] tokenizedLine = tokenizeLine(line, "`");
		try {
			Map<String, Object> m = new HashMap<String, Object>();
			int index = 0;
			int version = Integer.parseInt(tokenizedLine[index++]);
			m.put("version", version);
			m.put("encrypt", Integer.valueOf(tokenizedLine[index++]));

			int type = Integer.valueOf(tokenizedLine[index++]);
			m.put("type", type);
			m.put("count", Integer.valueOf(tokenizedLine[index++]));
			m.put("utm_id", tokenizedLine[index++]);

			if (version == 1) {
				if (type == 1) { // kernel log (packet filter)
					parseFirewallLogV1(tokenizedLine, m);
				} else if (type == 2) { // application log
					parseApplicationLogV1(tokenizedLine, m);
				}
			} else if (version == 3) {
				Integer moduleFlag = Integer.valueOf(tokenizedLine[index++]);
				m.put("module_flag", moduleFlag);
				// log data
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
				String dateToken = tokenizedLine[index++];
				String timeToken = tokenizedLine[index++];
				try {
					m.put("date", dateFormat.parse(dateToken + " " + timeToken));
				} catch (ParseException e) {
				}

				if (type == 1) { // kernel log (packet filter)
					parseFirewallLogV3(tokenizedLine, m);
				} else if (type == 2) { // application log
					parseApplicationLogV3(tokenizedLine, moduleFlag, m);
				}
			}

			return m;
		} catch (Throwable t) {
			logger.debug("araqne syslog parser: cannot parse trusguard log => " + line, t);
			return null;
		}
	}

	private void parseApplicationLogV3(String[] tokenizedLine, Integer moduleFlag, Map<String, Object> m) {
		if (moduleFlag == 1010) {// operation
			parseOperationLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1011) {// stat
			parseStatLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1050) {// webfilter
			parseWebFilterLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1070) {// app filter
			parseAppFilterLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1100) {// IPS
			parseIpsLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1110) {// DNS
			parseDnsLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1120 || moduleFlag == 1121) {// IAC
			parseInternetAccessControlLogV3(tokenizedLine, moduleFlag, m);
		} else if (moduleFlag == 1140) {// Qos
			parseQosLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1141) {// LBQos
			parseLbqosLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1150 || moduleFlag == 1151) {// proxy
			parseProxyLogV3(tokenizedLine, m);
		} else if (moduleFlag == 1160) {// system quarantine
			parseSystemQuarantineLogV3(tokenizedLine, m);
		}
	}

	private void parseSystemQuarantineLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));
		String action = tokenizedLine[index++];
		if (action.equals("0"))
			action = "격리";
		else if (action.equals("1"))
			action = "웹리디렉션";
		else if (action.equals("2"))
			action = "세션차단";
		m.put("action", action);
		String userToken = tokenizedLine[index++];
		m.put("user", userToken.isEmpty() ? null : userToken);
		m.put("module_name", tokenizedLine[index++]);
		m.put("description", tokenizedLine[index++]);
		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseProxyLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));
		String action = tokenizedLine[index++];
		if (action.equals("3003"))
			action = "ACT_PASS";
		else if (action.equals("3001"))
			action = "ACT_DROP";
		m.put("action", action);
		String userToken = tokenizedLine[index++];
		m.put("user", userToken.isEmpty() ? null : userToken);
		m.put("module_name", tokenizedLine[index++]);
		m.put("description", tokenizedLine[index++]);
		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseLbqosLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));
		m.put("action", tokenizedLine[index++]);
		index++;// user not use
		m.put("module_name", tokenizedLine[index++]);
		m.put("description", tokenizedLine[index++]);
		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseQosLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));
		m.put("action", tokenizedLine[index++]);
		String userToken = tokenizedLine[index++];
		m.put("user", userToken.isEmpty() ? null : userToken);
		m.put("module_name", tokenizedLine[index++]);
		m.put("qos_name", tokenizedLine[index++]);
		m.put("eth_name", tokenizedLine[index++]);
		m.put("bps", Integer.valueOf(tokenizedLine[index++]));
		m.put("pps", Integer.valueOf(tokenizedLine[index++]));
		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseInternetAccessControlLogV3(String[] tokenizedLine, Integer moduleFlag, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));
		String action = tokenizedLine[index++];
		if (action.equals("0"))
			action = "허용";
		else if (action.equals("1"))
			action = "차단";
		else if (action.equals("2"))
			action = "설치유도";
		else if (action.equals("3"))
			action = "삭제";
		else if (action.equals("4"))
			action = "차단(미설치)";
		else if (action.equals("10"))
			action = "악성패킷차단요청";
		else if (action.equals("11"))
			action = "치료상태감시중";
		else if (action.equals("12"))
			action = "안리포트수집요청";
		m.put("action", action);
		String userToken = tokenizedLine[index++];
		m.put("user", userToken.isEmpty() ? null : userToken);
		m.put("module_name", tokenizedLine[index++]);
		if (moduleFlag == 1120)
			m.put("mac", tokenizedLine[index++]);
		else if (moduleFlag == 1121)
			m.put("group_name", tokenizedLine[index++]);

		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseDnsLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));
		m.put("action", tokenizedLine[index++]);
		index++;// user not use
		m.put("module_name", tokenizedLine[index++]);
		m.put("reason", tokenizedLine[index++]);
		m.put("description", tokenizedLine[index++]);

		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseIpsLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));

		String action = tokenizedLine[index++];
		if (action.equals("3003"))
			action = "허용";
		else if (action.equals("3001"))
			action = "차단";
		else if (action.equals("3002"))
			action = "시스템 격리";
		else if (action.equals("3006"))
			action = "세션 끊기";
		else if (action.equals("3007"))
			action = "사용량 제한";
		else if (action.equals("3008"))
			action = "DDos차단";

		m.put("action", action);
		String userToken = tokenizedLine[index++];
		m.put("user", userToken.isEmpty() ? null : userToken);
		m.put("module_name", tokenizedLine[index++]);
		m.put("reason", tokenizedLine[index++]);
		m.put("nif", tokenizedLine[index++]);
		m.put("eth_protocol", tokenizedLine[index++]);
		m.put("src_mac", tokenizedLine[index++]);
		m.put("rule_id", tokenizedLine[index++]);
		m.put("vlan_id", tokenizedLine[index++]);
		m.put("msg", tokenizedLine[index++]);

		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseAppFilterLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));
		m.put("action", tokenizedLine[index++]);
		String userToken = tokenizedLine[index++];
		m.put("user", userToken.isEmpty() ? null : userToken);
		m.put("module_name", tokenizedLine[index++]);
		m.put("ap_protocol", tokenizedLine[index++]);
		m.put("description", tokenizedLine[index++]);

		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseWebFilterLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		String severityToken = tokenizedLine[index++];
		String protocolToken = tokenizedLine[index++];

		m.put("severity", severityToken.isEmpty() ? null : Integer.valueOf(severityToken));
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("action", tokenizedLine[index++]);
		String userToken = tokenizedLine[index++];
		m.put("user", userToken.isEmpty() ? null : userToken);
		m.put("module_name", tokenizedLine[index++]);
		m.put("wf_type", tokenizedLine[index++]);
		m.put("reason", tokenizedLine[index++]);
		m.put("url", tokenizedLine[index++]);

		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseStatLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;

		m.put("module_name", tokenizedLine[index++]);
		m.put("cpu", Integer.valueOf(tokenizedLine[index++]));
		m.put("mem", Integer.valueOf(tokenizedLine[index++]));
		m.put("hdd", Integer.valueOf(tokenizedLine[index++]));
		m.put("session", tokenizedLine[index++]);
		m.put("in_data", Long.parseLong(tokenizedLine[index++]));
		m.put("out_data", Long.parseLong(tokenizedLine[index++]));
		m.put("in_pkt", Long.parseLong(tokenizedLine[index++]));
		m.put("out_pkt", Long.parseLong(tokenizedLine[index++]));
		m.put("ha", tokenizedLine[index++]);
		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseOperationLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		String severityToken = tokenizedLine[index++];
		m.put("severity", severityToken.isEmpty() ? null : Integer.valueOf(severityToken));

		String protocolToken = tokenizedLine[index++];
		String srcIpToken = tokenizedLine[index++];
		String srcPortToken = tokenizedLine[index++];
		String dstIpToken = tokenizedLine[index++];
		String dstPortToken = tokenizedLine[index++];

		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", srcIpToken.isEmpty() ? null : srcIpToken);
		m.put("src_port", srcPortToken.isEmpty() ? null : Integer.valueOf(srcPortToken));
		m.put("dst_ip", dstIpToken.isEmpty() ? null : dstIpToken);
		m.put("dst_port", dstPortToken.isEmpty() ? null : Integer.valueOf(dstPortToken));

		m.put("action", tokenizedLine[index++]);
		String userToken = tokenizedLine[index++];
		m.put("user", userToken.isEmpty() ? null : userToken);
		m.put("module_name", tokenizedLine[index++]);
		m.put("description", tokenizedLine[index++]);
		String codeToken = tokenizedLine[index++];
		m.put("code", codeToken.isEmpty() ? null : codeToken);
	}

	private void parseFirewallLogV3(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;

		String logType = tokenizedLine[index++];
		if (logType.equals("1")) {
			logType = "Allow";
		} else if (logType.equals("2")) {
			logType = "Deny";
		} else if (logType.equals("3")) {
			logType = "Expire";
		} else if (logType.equals("4")) {
			logType = "Alive";
		}
		m.put("logtype", logType);

		String protocolToken = tokenizedLine[index++];
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("policy_id", tokenizedLine[index++]);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("in_nic", tokenizedLine[index++]);
		m.put("out_nic", tokenizedLine[index++]);

		String natTypeToken = tokenizedLine[index++];
		m.put("nat_type", natTypeToken.isEmpty() ? null : natTypeToken);

		String natIp = tokenizedLine[index++];
		m.put("nat_ip", natIp.isEmpty() ? null : natIp);

		String natPortToken = tokenizedLine[index++];
		m.put("nat_port", natPortToken.isEmpty() ? null : Integer.valueOf(natPortToken));

		String sentDataToken = tokenizedLine[index++];
		String sentPktToken = tokenizedLine[index++];
		String rcvdDataToken = tokenizedLine[index++];
		String rcvdPktToken = tokenizedLine[index++];
		String duration = tokenizedLine[index++];
		String state = tokenizedLine[index++];
		String reason = tokenizedLine[index++];
		String code = tokenizedLine[index++];
		String tcpFlag = tokenizedLine[index++];

		m.put("sent_data", sentDataToken.isEmpty() ? null : Long.valueOf(sentDataToken));
		m.put("sent_pkt", sentPktToken.isEmpty() ? null : Long.valueOf(sentPktToken));
		m.put("rcvd_data", rcvdDataToken.isEmpty() ? null : Long.valueOf(rcvdDataToken));
		m.put("rcvd_pkt", rcvdPktToken.isEmpty() ? null : Long.valueOf(rcvdPktToken));
		m.put("duration", duration.isEmpty() ? null : duration);
		m.put("state", state.isEmpty() ? null : state);
		m.put("reason", reason.isEmpty() ? null : reason);
		m.put("code", code.isEmpty() ? null : code);
		m.put("tcp_flag", tcpFlag.isEmpty() ? null : tcpFlag);

	}

	private void parseFirewallLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 5;
		// log data
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		String dateToken = tokenizedLine[index++];
		String timeToken = tokenizedLine[index++];
		try {
			m.put("date", dateFormat.parse(dateToken + " " + timeToken));
		} catch (ParseException e) {
		}

		String logType = tokenizedLine[index++];
		m.put("logtype", logType);

		String protocolToken = tokenizedLine[index++];
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("policy_id", tokenizedLine[index++]);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("in_nic", tokenizedLine[index++]);
		m.put("out_nic", tokenizedLine[index++]);

		String natTypeToken = tokenizedLine[index++];
		m.put("nat_type", natTypeToken.isEmpty() ? null : natTypeToken);

		String natIp = tokenizedLine[index++];
		m.put("nat_ip", natIp.isEmpty() ? null : natIp);

		String natPortToken = tokenizedLine[index++];
		m.put("nat_port", natPortToken.isEmpty() ? null : Integer.valueOf(natPortToken));

		String sentDataToken = tokenizedLine[index++];
		String sentPktToken = tokenizedLine[index++];
		String rcvdDataToken = tokenizedLine[index++];
		String rcvdPktToken = tokenizedLine[index++];

		m.put("sent_data", sentDataToken.isEmpty() ? null : Long.valueOf(sentDataToken));
		m.put("sent_pkt", sentPktToken.isEmpty() ? null : Long.valueOf(sentPktToken));
		m.put("rcvd_data", rcvdDataToken.isEmpty() ? null : Long.valueOf(rcvdDataToken));
		m.put("rcvd_pkt", rcvdPktToken.isEmpty() ? null : Long.valueOf(rcvdPktToken));
	}

	private void parseApplicationLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 5;
		int moduleFlag = Integer.valueOf(tokenizedLine[index++]);
		m.put("module_flag", moduleFlag);

		// log data
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		String dateToken = tokenizedLine[index++];
		String timeToken = tokenizedLine[index++];
		try {
			m.put("date", dateFormat.parse(dateToken + " " + timeToken));
		} catch (ParseException e) {
		}

		if (moduleFlag == 1)
			parseOperationLogV1(tokenizedLine, m);
		else if (moduleFlag == 2)
			parseVirusLogV1(tokenizedLine, m);
		else if (moduleFlag == 3)
			parseSpamLogV1(tokenizedLine, m);
		else if (moduleFlag == 4)
			parseWebFilterLogV1(tokenizedLine, m);
		else if (moduleFlag == 6)
			parseAppFilterLogV1(tokenizedLine, m);
		else if (moduleFlag == 8)
			parseSslVpnLogV1(tokenizedLine, m);
		else if (moduleFlag == 9)
			parseIpsLogV1(tokenizedLine, m);
		else if (moduleFlag == 12)
			parseInternetAccessControlLogV1(tokenizedLine, m);
	}

	private void parseOperationLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		String severityToken = tokenizedLine[index++];
		index++;

		m.put("severity", severityToken);
		index++;
		index++;
		m.put("action", tokenizedLine[index++]);
		index++;
		m.put("module_name", tokenizedLine[index++]);
		m.put("description", tokenizedLine[index++]);
	}

	private void parseVirusLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		String severityToken = tokenizedLine[index++];
		String protocolToken = tokenizedLine[index++];

		m.put("severity", severityToken);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("action", tokenizedLine[index++]);
		m.put("user", tokenizedLine[index++]);
		m.put("module_name", tokenizedLine[index++]);
		m.put("virus_filter", tokenizedLine[index++]);
		m.put("virus_name", tokenizedLine[index++]);

		String path = tokenizedLine[index++];
		if (path.startsWith("[") && path.endsWith("]"))
			m.put("virus_url", path.substring(1, path.length() - 1));
		else
			m.put("virus_fname", path);

		if (tokenizedLine.length > 21) {
			m.put("sender_addr", tokenizedLine[index++]);
			m.put("recipients_addr", tokenizedLine[index++]);
			m.put("subject", tokenizedLine[index++]);
		}
	}

	private void parseSpamLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		String severityToken = tokenizedLine[index++];
		String protocolToken = tokenizedLine[index++];

		m.put("severity", severityToken);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("action", tokenizedLine[index++]);
		m.put("user", tokenizedLine[index++]);
		m.put("module_name", tokenizedLine[index++]);

		m.put("spam_filter", tokenizedLine[index++]);
		m.put("send_spam_log", tokenizedLine[index++]);
		m.put("sender_addr", tokenizedLine[index++]);
		m.put("recipients_addr", tokenizedLine[index++]);
		m.put("subject", tokenizedLine[index++]);
	}

	private void parseWebFilterLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		String severityToken = tokenizedLine[index++];
		String protocolToken = tokenizedLine[index++];

		m.put("severity", severityToken);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("action", tokenizedLine[index++]);
		m.put("user", tokenizedLine[index++]);
		m.put("module_name", tokenizedLine[index++]);
		m.put("wf_type", tokenizedLine[index++]);
		m.put("reason", tokenizedLine[index++]);

		String url = tokenizedLine[index++];
		m.put("url", url.substring(1, url.length() - 1));
	}

	private void parseAppFilterLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src", tokenizedLine[index++]);
		m.put("dst", tokenizedLine[index++]);
		m.put("action", tokenizedLine[index++]);
		m.put("user", tokenizedLine[index++]);
		m.put("module_name", tokenizedLine[index++]);
		m.put("ap_protocol", tokenizedLine[index++]);
		m.put("description", tokenizedLine[index++]);
	}

	private void parseSslVpnLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("action", tokenizedLine[index++]);
		m.put("user", tokenizedLine[index++]);
		m.put("module_name", tokenizedLine[index++]);
		m.put("event", tokenizedLine[index++]);
		m.put("epsec", tokenizedLine[index++]);
	}

	private void parseIpsLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("action", tokenizedLine[index++]);
		m.put("user", tokenizedLine[index++]);
		m.put("module_name", tokenizedLine[index++]);
		m.put("reason", tokenizedLine[index++]);
		m.put("nif", tokenizedLine[index++]);
		m.put("eth_protocol", tokenizedLine[index++]);
		m.put("src_mac", tokenizedLine[index++]);
		m.put("rule_id", tokenizedLine[index++]);
		m.put("vlan_id", tokenizedLine[index++]);
		m.put("msg", tokenizedLine[index++]);
	}

	private void parseInternetAccessControlLogV1(String[] tokenizedLine, Map<String, Object> m) {
		int index = 8;
		int severity = Integer.valueOf(tokenizedLine[index++]);
		String protocolToken = tokenizedLine[index++];

		m.put("severity", severity);
		m.put("protocol", protocolToken.isEmpty() ? null : protocolToken);
		m.put("src_ip", tokenizedLine[index++]);
		m.put("src_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("dst_ip", tokenizedLine[index++]);
		m.put("dst_port", Integer.valueOf(tokenizedLine[index++]));
		m.put("action", tokenizedLine[index++]);
		m.put("user", tokenizedLine[index++]);
		m.put("module_name", tokenizedLine[index++]);
		m.put("mac", tokenizedLine[index++]);
	}

	private String[] tokenizeLine(String line, String delimiter) {
		int last = 0;
		List<String> tokenizedLine = new ArrayList<String>(32);
		while (true) {
			int p = line.indexOf(delimiter, last);

			String token = null;
			if (p >= 0)
				token = line.substring(last, p);
			else
				token = line.substring(last);

			tokenizedLine.add(token);

			if (p < 0)
				break;
			last = ++p;
		}

		return tokenizedLine.toArray(new String[0]);
	}
}
