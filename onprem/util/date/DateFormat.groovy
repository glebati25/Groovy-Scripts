package utils.date

import com.atlassian.jira.component.ComponentAccessor

import java.text.DateFormat
import java.text.SimpleDateFormat

class DateFormats {
    public static final DateFormat JIRA_DATE_FORMAT = new SimpleDateFormat('d/MMM/yy', Locale.default)
    public static final DateFormat CSV_DEFAULT_DATE_FORMAT
    static {
        CSV_DEFAULT_DATE_FORMAT = new SimpleDateFormat('E MMM dd HH:mm:ss Z yyyy', Locale.default)
        CSV_DEFAULT_DATE_FORMAT.lenient = false
    }

    static String translateDateToJiraFormat(String dateStr, DateFormat initialFormat) {
        DateFormat authedJiraDateFormat = new SimpleDateFormat('d/MMM/yy', ComponentAccessor.jiraAuthenticationContext.locale)
        authedJiraDateFormat.format(initialFormat.parse(dateStr))
    }
}
