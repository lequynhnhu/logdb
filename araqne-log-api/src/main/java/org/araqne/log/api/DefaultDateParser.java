package org.araqne.log.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultDateParser implements DateParser {
	private SimpleDateFormat dateFormat;
	private Pattern p;
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultDateParser.class);

	public DefaultDateParser(String simpleDateFormat) {
		this.dateFormat = new SimpleDateFormat(simpleDateFormat);
		this.p = Pattern.compile(dateFormatToRegex(simpleDateFormat));
	}
	public DefaultDateParser(SimpleDateFormat dateFormat, String dateExtractor) {
		this.dateFormat = dateFormat;
		this.p = Pattern.compile(dateExtractor);
	}

	@Override
	public Date parse(String line) {
		Matcher m = p.matcher(line);

		if (!m.find() || m.groupCount() == 0) {
			logger.trace("araqne log api: cannot find date extractor pattern in line: {}", line);
			return null;
		}

		do {
			for (int group = 1; group <= m.groupCount(); group++) {
				try {
					String dateString = m.group(group);
					Date date = dateFormat.parse(dateString);
					Calendar c = Calendar.getInstance();
					int currentYear = c.get(Calendar.YEAR);
					c.setTime(date);

					int year = c.get(Calendar.YEAR);
					if (year == 1970)
						c.set(Calendar.YEAR, currentYear);

					return c.getTime();
				} catch (ParseException e) {
				}
			}
		} while (m.find());

		logger.error("araqne log api: cannot find date in line: " + line);
		return null;
	}
	
	public static String dateFormatToRegex(String pattern) {
		StringBuilder regex = new StringBuilder();
		boolean isInQuote = false;
		int l = pattern.length();

		regex.append("(");

		for (int i = 0; i < l; i++) {
			if (i + 1 < l && pattern.charAt(i) == '\'') {
				if (pattern.charAt(i + 1) == '\'') {
					regex.append("'");
					i++;
				} else {
					if (isInQuote) {
						if (pattern.charAt(i) == '\'') {
							isInQuote = false;
							continue;
						}
						regex.append(pattern.charAt(i));
						continue;
					} else
						isInQuote = true;
				}
				continue;
			}

			int r = 1;
			while (i + 1 < l && pattern.charAt(i) == pattern.charAt(i + 1)) {
				r++;
				i++;
				continue;
			}

			switch (pattern.charAt(i)) {
			case 'G':
				regex.append("(AD|BC)");
				break;

			case 'W':
			case 'F':
				regex.append("\\d" + repeat(1, r));
				break;

			case 'E':
				if (r <= 3)
					regex.append(".{3}");
				else
					regex.append("\\p{Upper}\\p{Lower}+day");
				break;

			case 'a':
				regex.append("(AM|PM)");
				break;

			case 'M':
				if (r > 3) {
					regex.append("(?i)(January|February|March|April|May|June|July|August|September|"
							+ "October|November|December|Undecimber)");
					break;
				} else if (r == 3) {
					regex.append(".{3}");
					break;
				}
			case 'w':
			case 'd':
			case 'H':
			case 'k':
			case 'K':
			case 'h':
			case 'm':
			case 's':
				regex.append("\\d" + repeat(Math.max(1, r), Math.max(2, r)));
				break;

			case 'D':
				regex.append("\\d" + repeat(Math.max(1, r), Math.max(3, r)));
				break;

			case 'y':
				regex.append("\\d" + repeat(Math.max(1, r), Math.max(2, r)));
				break;

			case 'S':
				regex.append("\\d" + repeat(Math.max(1, r), Math.max(3, r)));
				break;

			case 'Z':
				regex.append("[+-]\\d" + repeat(4));
				break;

			case '(':
			case ')':
			case '{':
			case '}':
				regex.append("\\");
			default:
				regex.append(pattern.charAt(i));
				if (r > 1)
					regex.append(repeat(r));
			}
		}
		regex.append(")");

		return regex.toString();
	}

	private static String repeat(int num) {
		return "{" + num + "}";
	}

	private static String repeat(int min, int max) {
		if (min == max)
			return repeat(min);
		return "{" + min + "," + max + "}";
	}

}
