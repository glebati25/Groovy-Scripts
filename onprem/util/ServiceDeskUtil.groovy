package utils.sd

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.Project
import com.atlassian.servicedesk.api.ServiceDesk
import com.atlassian.servicedesk.api.ServiceDeskManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ServiceDeskUtil {
    static final Logger log = LoggerFactory.getLogger(ServiceDeskUtil)
    static final ServiceDeskManager serviceDeskManager = ComponentAccessor.getOSGiComponentInstanceOfType(ServiceDeskManager)

    static ServiceDesk getServiceDeskForProject(Project project) {
        Objects.requireNonNull(project, 'project must not be null')
        def serviceDesk
        try {
            serviceDesk = serviceDeskManager.getServiceDeskForProject(project)
        } catch (ex) {
            // swallow because of new API behaviour in JSD 4.x.x.
        }
        return (serviceDesk instanceof ServiceDesk) ? serviceDesk : serviceDesk?.getOrNull()
    }
}
