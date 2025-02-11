package utils.plugins.scriptrunner.behaviours

import com.onresolve.jira.groovy.user.FormField

class BehavioursUtil {

    static void hideField(FormField formField) {
        formField.with {
            hidden = true
            clearError()
        }
    }

    static void hideAndClearField(FormField formField) {
        formField.with {
            hidden = true
            clearError()
            formValue = null
        }
    }

    static void requireField(FormField formField, String textError) {
        formField.with {
            hidden = false
            value ? clearError() : (error = textError)
        }
    }
}
