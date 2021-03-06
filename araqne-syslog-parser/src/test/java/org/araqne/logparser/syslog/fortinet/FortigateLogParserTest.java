package org.araqne.logparser.syslog.fortinet;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class FortigateLogParserTest {
	@Test
	public void test_1() {
		String line = "<188>date=2015-03-09 time=18:01:33 devname=COWON device_id=FG300A3907505547 log_id=0038000007 type=traffic subtype=other pri=warning vd=root src=14.50.130.103 src_port=54041 src_int=\"port1\" dst=118.130.232.162 dst_port=53719 dst_int=\"root\" SN=39149719 status=deny policyid=0 dst_country=\"Korea, Republic of\" src_country=\"Korea, Republic of\" service=53719/udp proto=17 duration=5 sent=0 rcvd=0 msg=\"iprope_in_check() check failed, drop\"";

		FortigateLogParser p = new FortigateLogParser();	
		Map<String, Object> m = p.parse(line(line));
		assertEquals("2015-03-09", m.get("date"));
		assertEquals("root", m.get("dst_int"));
		assertEquals("Korea, Republic of", m.get("src_country"));
		assertEquals("iprope_in_check() check failed, drop", m.get("msg"));
	}
	
	@Test
	public void test_2() {
		String line = "<189>date=2015-03-09 time=18:01:33 devname=COWON device_id=FG300A3907505547 log_id=0021000002 type=traffic subtype=allowed pri=notice vd=root src=192.168.6.9 src_port=23709 src_int=\"Internal\" dst=175.158.20.62 dst_port=80 dst_int=\"port1\" SN=39149493 status=accept policyid=65 dst_country=\"Korea, Republic of\" src_country=\"Reserved\" dir_disp=org tran_disp=snat tran_sip=118.130.232.162 tran_sport=62489 service=HTTP proto=6 duration=16 sent=180 rcvd=92 sent_pkt=4 rcvd_pkt=2";

		FortigateLogParser p = new FortigateLogParser();	
		Map<String, Object> m = p.parse(line(line));
		assertEquals("2015-03-09", m.get("date"));
		assertEquals("118.130.232.162", m.get("tran_sip"));
		
	}
	
	@Test
	public void test_3() {
		String line = "<189>date=2015-03-09 time=18:01:33 devname=COWON device_id=FG300A3907505547 log_id=0021000002 type=traffic subtype=allowed pri=notice vd=root src=192.168.4.65 src_port=53217 src_int=\"Internal\" dst=202.179.177.202 dst_port=80 dst_int=\"port1\" SN=39149495 status=accept policyid=65 dst_country=\"Korea, Republic of\" src_country=\"Reserved\" dir_disp=org tran_disp=snat tran_sip=118.130.232.162 tran_sport=26469 service=HTTP proto=6 app=\"HTTP.BROWSER\" subapp=\"HTTP.BROWSER\" app_cat=\"Network.Service\" subappcat=\"Network.Service\" duration=16 sent=849 rcvd=1151 sent_pkt=7 rcvd_pkt=4";

		FortigateLogParser p = new FortigateLogParser();	
		Map<String, Object> m = p.parse(line(line));
		assertEquals("2015-03-09", m.get("date"));
		assertEquals("7", m.get("sent_pkt"));
		assertEquals("Network.Service", m.get("subappcat"));
	}
	
	@Test
	public void test_4() {
		String line = "<189>date=2015-03-09 time=18:01:33 devname=COWON device_id=FG300A3907505547 log_id=0038000004 type=traffic subtype=other pri=notice vd=root src=192.168.7.9 src_port=5360 src_int=\"Internal\" dst=125.209.238.154 dst_port=80 dst_int=\"port1\" SN=39149783 status=start policyid=65 dst_country=\"Korea, Republic of\" src_country=\"Reserved\" tran_sip=118.130.232.162 tran_sport=62580 service=HTTP proto=6 duration=0 sent=0 rcvd=0";

		FortigateLogParser p = new FortigateLogParser();	
		Map<String, Object> m = p.parse(line(line));
		assertEquals("2015-03-09", m.get("date"));
		assertEquals("port1", m.get("dst_int"));
		assertEquals("FG300A3907505547", m.get("device_id"));
	}
	
	@Test
	public void test_5() {
		String line = "<190>date=2015-03-09 time=18:01:33 devname=COWON device_id=FG300A3907505547 log_id=1059028704 type=app-ctrl subtype=app-ctrl-all pri=information vd=\"root\" attack_id=15893 user=\"N/A\" group=\"N/A\" src=192.168.4.66 src_port=52995 src_int=\"Internal\" dst=203.248.44.232 dst_port=80 dst_int=\"port1\" src_name=\"192.168.4.66\" dst_name=\"203.248.44.232\" profilegroup=\"N/A\" profiletype=\"N/A\" profile=\"N/A\" proto=6 service=\"http\" policyid=65 intf_policyid=0 identidx=0 serial=39149655 app_list=\"scan\" app_type=\"Network.Service\" app=\"HTTP.BROWSER\" action=pass count=1 hostname=\"cyber.kepco.co.kr\" url=\"/ckepco/front/jsp/CY/A/B/CYABPP006.jsp?div=2\" msg=\"Network.Service: HTTP.BROWSER, \"";

		FortigateLogParser p = new FortigateLogParser();	
		Map<String, Object> m = p.parse(line(line));
		assertEquals("2015-03-09", m.get("date"));
		assertEquals("N/A", m.get("profiletype"));
		assertEquals("/ckepco/front/jsp/CY/A/B/CYABPP006.jsp?div=2", m.get("url"));
	}
	
	private Map<String, Object> line(String line) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("line", line);
		return m;
	}
}
