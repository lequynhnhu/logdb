/**
 * Copyright 2014 Eediom Inc.
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
package org.araqne.log.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

@Component(name = "multi-rotation-file-logger-factory")
@Provides
public class MultiRotationFileLoggerFactory extends AbstractLoggerFactory {
	@Override
	public String getName() {
		return "multi-rotation";
	}

	@Override
	public String getDisplayName(Locale locale) {
		if (locale != null && locale.equals(Locale.KOREAN))
			return "멀티 로테이션 로그 파일";
		if (locale != null && locale.equals(Locale.CHINESE))
			return "多回滚日志文件";
		return "Multi Rotation Log File";
	}

	@Override
	public Collection<Locale> getDisplayNameLocales() {
		return Arrays.asList(Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE);
	}

	@Override
	public String getDescription(Locale locale) {
		if (locale != null && locale.equals(Locale.KOREAN))
			return "일정 주기마다 다른 경로에 백업 후 삭제하고 다시 쓰는 로그 파일들을 수집합니다.";
		if (locale != null && locale.equals(Locale.CHINESE))
			return "采集定期备份到其他路径之后删除并重新写的日志文件。";
		return "Collect matching rotation text log files";
	}

	@Override
	public Collection<Locale> getDescriptionLocales() {
		return Arrays.asList(Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE);
	}

	@Override
	public Collection<LoggerConfigOption> getConfigOptions() {
		LoggerConfigOption basePath = new MutableStringConfigType("base_path", t("Directory path", "디렉터리 경로", "ディレクトリ経路", "目录"), t(
				"Base log file directory path", "로그 파일을 수집할 대상 디렉터리 경로", "ログファイルを収集する対象ディレクトリ経路", "要采集的日志文件所在目录"), true);

		LoggerConfigOption fileNamePattern = new MutableStringConfigType("filename_pattern", t("Filename pattern", "파일이름 패턴",
				"ファイルなパータン", "文件名模式"), t("Regular expression to match log file name", "대상 로그 파일을 선택하는데 사용할 정규표현식",
				"対象ログファイルを選ぶとき使う正規表現", "用于筛选文件的正则表达式"), true);

		LoggerConfigOption datePattern = new MutableStringConfigType("date_pattern", t("Date Pattern", "날짜 정규식", "日付正規表現", "日期正则表达式"),
				t("Regex for date extraction", "날짜 문자열 추출에 사용되는 정규표현식", "日付文字列の抽出に使う正規表現", "用于提取日期字符串的正则表达式"), false, t(null));

		LoggerConfigOption dateFormat = new MutableStringConfigType("date_format", t("Date Format", "날짜 패턴", "日付パターン", "日期模式"), t(
				"Date pattern of log file", "날짜 파싱에 필요한 패턴 (예시: yyyy-MM-dd HH:mm:ss)", "日付の解析に使うパターン (例: yyyy-MM-dd HH:mm:ss)",
				"用于解析日期的特征"), false, t("MMM dd HH:mm:ss"));

		LoggerConfigOption dateLocale = new MutableStringConfigType("date_locale", t("Date Locale", "날짜 로케일", "日付ロケール", "日期区域"), t(
				"Date locale of log file", "날짜 문자열의 로케일. 가령 날짜 패턴의 MMM 지시어은 영문 로케일에서 Jan으로 인식됩니다.", "日付文字列ののロケール", "日期字符串区域"),
				false, t("en"));

		LoggerConfigOption charset = new MutableStringConfigType("charset", t("Charset", "문자 집합", "文字セット", "字符集"), t("Charset",
				"문자 집합. 기본값 UTF-8", "文字セット。基本値はutf-8", "字符集，默认值为UTF-8"), false, t("utf-8"));

		LoggerConfigOption timezone = new MutableStringConfigType("timezone", t("Time zone", "시간대", "時間帯", "时区"), t(
				"Time zone, e.g. America/New_york ", "시간대, 예를 들면 KST 또는 Asia/Seoul", "時間帯。例えばJSTまたはAsia/Tokyo",
				"时区， 例如 Asia/Beijing"), false);

		LoggerConfigOption logBeginRegex = new MutableStringConfigType("begin_regex", t("Log begin regex", "로그 시작 구분 정규식", "ログ始め正規表現",
				"日志起始正则表达式"), t("Regular expression to determine whether the line is start of new log."
				+ "(if a line does not matches, the line will be merged to prev line.).",
				"새 로그의 시작을 인식하기 위한 정규식(매칭되지 않는 경우 이전 줄에 병합됨)", "新しいログの始まりを認識する正規表現 (マッチングされない場合は前のラインに繋げる)",
				"用于识别日志起始位置的正则表达式(如没有匹配项，则合并到之前日志)"), false);

		LoggerConfigOption logEndRegex = new MutableStringConfigType("end_regex", t("Log end regex", "로그 끝 구분 정규식", "ログ終わり正規表現",
				"日志结束正则表达式"), t("Regular expression to determine whether the line is end of new log."
				+ "(if a line does not matches, the line will be merged to prev line.).",
				"로그의 끝을 인식하기 위한 정규식(매칭되지 않는 경우 이전 줄에 병합됨)", "ログの終わりを認識する正規表現 (マッチングされない場合は前のラインに繋げる)",
				"用于识别日志结束位置地正则表达式(如没有匹配项，则合并到之前日志)"), false);

		LoggerConfigOption fileTag = new MutableStringConfigType("file_tag", t("Filename Tag", "파일이름 태그", "ファイル名タグ", "文件名标记"), t(
				"Field name for filename tagging", "파일명을 태깅할 필드 이름", "ファイル名をタギングするフィールド名", "要进行文件名标记的字段"), false);

		return Arrays.asList(basePath, fileNamePattern, charset, datePattern, dateFormat, dateLocale, timezone, logBeginRegex,
				logEndRegex, fileTag);
	}

	private Map<Locale, String> t(String text) {
		return t(text, text, text, text);
	}

	private Map<Locale, String> t(String enText, String koText, String jpText, String cnText) {
		Map<Locale, String> m = new HashMap<Locale, String>();
		m.put(Locale.ENGLISH, enText);
		m.put(Locale.KOREAN, koText);
		m.put(Locale.JAPANESE, jpText);
		m.put(Locale.CHINESE, cnText);
		return m;
	}

	@Override
	protected Logger createLogger(LoggerSpecification spec) {
		return new MultiRotationFileLogger(spec, this);
	}
}
