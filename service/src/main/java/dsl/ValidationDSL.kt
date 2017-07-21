package dsl

import rulesengine.Engine
import rulesengine.ExampleRule
import rulesengine.MustHave
import rulesengine.Person

/**
 * Created by msrivastava on 7/20/17.
 */
fun validationengine(init: Engine.() -> Unit): Engine {
    val engine = Engine()
    engine.init()
    return engine
}

fun examplerule (desc:String="",init:(Person) -> Boolean): ExampleRule {
    return ExampleRule(init,desc)
}

fun musthave(field:()->String): MustHave {
    return MustHave(field())
}

fun main(args : Array<String>) {
    val tim = Person("Tim", income = 100)

    validationengine {
        addRule {
            examplerule("Missing Nationality") {
                p -> p.nationality!=null
            }
        }
        addRule {
            musthave {
                "income"
            }
        }
        addRule {
            musthave {
                "university"
            }
        }
        println(runRules(tim).errors)
    }
}
