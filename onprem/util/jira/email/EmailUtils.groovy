package utils.jira.email

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.mail.Email
import com.atlassian.mail.queue.MailQueue
import com.atlassian.mail.queue.SingleMailQueueItem

import java.nio.charset.StandardCharsets

class EmailUtils {
    static final String MIME = 'text/html'
    static final String ENCODING = StandardCharsets.UTF_8.name()

    static void send(final Email... emails) {
        final MailQueue queue = ComponentAccessor.mailQueue
        emails.each {
            queue.addItem(new SingleMailQueueItem(it))
        }
    }

    static Email prepareSimpleEmail(final String address, final String subject, final String body) {
        final Email email = new Email(address)
        email.with {
            setSubject(subject)
            setBody(body)
            setMimeType(MIME)
            setEncoding(ENCODING)
        }
    }
}

