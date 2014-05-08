package org.araqne.log.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

@Component(name = "gzip-directory-watch-logger-factory")
@Provides
public class GzipDirectoryWatchLoggerFactory extends AbstractLoggerFactory {

	@Override
	public String getName() {
		return "gzip-dirwatch";
	}

	@Override
	public String getDisplayName(Locale locale) {
		if (locale != null && locale.equals(Locale.KOREAN))
			return "GZIP 디렉터리 와처";
		if (locale != null && locale.equals(Locale.JAPANESE))
			return "GZIP ディレクトリウォッチャー";
		return "GZIP Directory Watcher";
	}

	@Override
	public Collection<Locale> getDisplayNameLocales() {
		return Arrays.asList(Locale.ENGLISH, Locale.KOREAN, Locale.JAPANESE);
	}

	@Override
	public String getDescription(Locale locale) {
		if (locale != null && locale.equals(Locale.KOREAN))
			return "지정된 디렉터리에서 파일이름 패턴과 일치하는 모든 gzip 파일을 수집합니다.";
		if (locale != null && locale.equals(Locale.JAPANESE))
			return "指定されたディレクトリからファイル名パータンと一致するすべてのgzipファイルを収集します。";
		return "Collect all gzip files in specified directory";
	}

	@Override
	public Collection<Locale> getDescriptionLocales() {
		return Arrays.asList(Locale.ENGLISH, Locale.KOREAN, Locale.JAPANESE);
	}

	@Override
	public Collection<LoggerConfigOption> getConfigOptions() {
		LoggerConfigOption basePath = new StringConfigType("base_path", t("Directory path", "디렉터리 경로", "ディレクトリ経路"), t(
				"Base gzip file directory path", "gzip 파일을 수집할 대상 디렉터리 경로", "gzipファイルを収集する対象ディレクトリ経路"), true);

		LoggerConfigOption fileNamePattern = new StringConfigType("filename_pattern", t("Filename pattern", "파일이름 패턴",
				"ファイル名パータン"), t("Regular expression to match gzip file name", "대상 gzip 파일을 선택하는데 사용할 정규표현식",
				"対象gzipファイルを選択するための正規表現"), true);

		LoggerConfigOption datePattern = new StringConfigType("date_pattern", t("Date pattern", "날짜 정규표현식", "日付正規表現"), t(
				"Regular expression to match date and time strings", "날짜 및 시각을 추출하는데 사용할 정규표현식", "日付と時刻を解析する正規表現"), false);

		LoggerConfigOption dateFormat = new StringConfigType("date_format", t("Date format", "날짜 포맷", "日付フォーマット"), t(
				"date format to parse date and time strings. e.g. yyyy-MM-dd HH:mm:ss",
				"날짜 및 시각 문자열을 파싱하는데 사용할 포맷. 예) yyyy-MM-dd HH:mm:ss", "日付と時刻の文字列を解析するためのフォーマット"), false);

		LoggerConfigOption dateLocale = new StringConfigType("date_locale", t("Date locale", "날짜 로케일", "日付ロケール"), t(
				"date locale, e.g. en", "날짜 로케일, 예를 들면 ko", "日付ロケール、例)jp"), false);

		LoggerConfigOption timezone = new StringConfigType("timezone", t("Time zone", "시간대","時間帯"), t("time zone, e.g. EST or America/New_york ",
					"시간대, 예를 들면 KST 또는 Asia/Seoul","時間帯。例えばJSTまたはAsia/Tokyo"), false);

		LoggerConfigOption newlogRegex = new StringConfigType("newlog_designator", t("Regex for first line", "로그 시작 정규식",
				"ログ始め正規表現"), t("Regular expression to determine whether the line is start of new log."
				+ "(if a line does not matches, the line will be merged to prev line.).",
				"새 로그의 시작을 인식하기 위한 정규식(매칭되지 않는 경우 이전 줄에 병합됨)", "新しいログの始まりを認識する正規表現 (マッチングされない場合は前のラインに繋げる)"), false);

		LoggerConfigOption newlogEndRegex = new StringConfigType("newlog_end_designator", t("Regex for last line", "로그 끝 정규식",
				"ログ終わり正規表現"), t("Regular expression to determine whether the line is end of new log."
				+ "(if a line does not matches, the line will be merged to prev line.).",
				"로그의 끝을 인식하기 위한 정규식(매칭되지 않는 경우 이전 줄에 병합됨)", "ログの終わりを認識する正規表現 (マッチングされない場合は前のラインに繋げる)"), false);

		LoggerConfigOption charset = new StringConfigType("charset", t("Charset", "문자 집합", "文字セット"), t("charset encoding",
				"gzip 압축 해제된 텍스트 파일의 문자 인코딩 방식", "gzipから解凍されたテキストファイルの文字エンコーディング方式"), false);

		LoggerConfigOption isDeleteFile = new StringConfigType("is_delete", t("Delete GZIP file", "GZIP 파일 삭제", "GZIPファイル削除"), t(
				"Delete GZIP file after load, true or false", "수집 완료된 GZIP 파일의 삭제 여부, true 혹은 false",
				"収集を完了したGZIPファイルの削除可否。trueかfalse"), false);

		return Arrays
				.asList(basePath, fileNamePattern, datePattern, dateFormat, dateLocale, timezone, newlogRegex, newlogEndRegex, charset,
						isDeleteFile);

	}

	private Map<Locale, String> t(String enText, String koText, String jpText) {
		Map<Locale, String> m = new HashMap<Locale, String>();
		m.put(Locale.ENGLISH, enText);
		m.put(Locale.KOREAN, koText);
		m.put(Locale.JAPANESE, jpText);
		return m;
	}

	@Override
	protected Logger createLogger(LoggerSpecification spec) {
		return new GzipDirectoryWatchLogger(spec, this);
	}
}
