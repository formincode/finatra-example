package rulesengine

/**
 * Created by msrivastava on 7/20/17.
 */
fun engine(init: Engine.() -> Unit): Engine {
    val engine = Engine()
    engine.init()
    return engine
}

fun examplerule (desc:String="",init:(Person) -> Boolean):ExampleRule {
    return ExampleRule(init,desc)
}

fun musthave(field:()->String):MustHave {
    return MustHave(field())
}

fun main(args : Array<String>) {
    val tim = Person("Tim", income = 100)

    engine {
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
