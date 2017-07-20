package rulesengine

/**
 * Created by msrivastava on 7/19/17.
 */
class Application(val candidate: Person) {

    var isWorthyOfInterview:Boolean = false
        private set

    var isGood:Boolean = false
        private set

    var isProductive:Boolean=false
        private set

    var isOther:Boolean = false
        private set

    fun markAsWorthyOfInterview() = { this.isWorthyOfInterview=true }
    fun markAsGood() = { this.isGood=true }
    fun markAsProductive() = { this.isProductive=true }
    fun markAsOther() = { this.isOther=true }
}

class ElegibityRule(val description:String, val condition: (Application)->Boolean, val action: (Application) -> Unit) {
    fun canActivate(application: Application):Boolean {
        try {
            return condition(application)
        } catch (e:Exception) {
            return false
        }
    }

    fun fire(application: Application) {
        action(application)
    }
}

class ElegibityRuleBase() {
    val rules = mutableListOf<ElegibityRule>()
}

class ElegibityRulesEngine(val rulesBase:ElegibityRuleBase,val application: Application) {
    val availableRules = mutableListOf<ElegibityRule>()
    val agenda = mutableListOf<ElegibityRule>()
    val firedLog = mutableListOf<ElegibityRule>()

    private fun activateRules() {
        for (rule in availableRules) {
            if (rule.canActivate(application)) {
                agenda.add(rule)
            }
        }

        for (rule in agenda) {
            availableRules.remove(rule)
        }
    }

    private fun fireRulesOnAgenda() {
        while (agenda.size>0) {
            fire(agenda.first())
        }
    }

    private fun fire(rule:ElegibityRule){
        rule.fire(application)
        firedLog.add(rule)
        agenda.remove(rule)
    }

    fun runRules() {
        activateRules()
        while (agenda.size > 0) {
            fireRulesOnAgenda()
            activateRules()
        }
    }
}

