package dsl

import rulesengine.Application
import rulesengine.ElegibityRule
import rulesengine.ElegibityRuleBase
import rulesengine.ElegibityRulesEngine
import rulesengine.Person

/**
 * Created by msrivastava on 7/20/17.
 */

fun elegibilityengine(rulesBase: ElegibityRuleBase, application: Application, init: ElegibityRulesEngine.() -> Unit): ElegibityRulesEngine {
    val engine = ElegibityRulesEngine(rulesBase,application)
    engine.init()
    return engine
}

fun elegibityRuleBase(init: ElegibityRuleBase.()->Unit): ElegibityRuleBase {
    val elegibityRuleBase = ElegibityRuleBase()
    elegibityRuleBase.init()
    return elegibityRuleBase
}

fun elegibityRule (desc:String="", condition:(Application) -> Boolean, action:(Application)->Unit): ElegibityRule {
    return ElegibityRule(desc,condition,action)
}


fun main(args : Array<String>) {
    val tim = Person("Tim", income = 100)
    val application = Application(tim)

    val rules = elegibityRuleBase {
        addRule {
            elegibityRule("test2",{application.isGood},{println("we have a winner")})
        }
        addRule {
            elegibityRule("test1",{application.candidate.name!=null},{application.markAsGood()})
        }
    }

    elegibilityengine(rules,application) {
        runRules()
    }
}
