package rulesengine

/**
 * Created by msrivastava on 7/19/17.
 */
data class Person(val name:String?=null,var university:String?=null, var income:Int?=null, var nationality:String?=null)

class Notification() {
    val errors = mutableListOf<String>()
}

interface Rule {
    fun check(notification:Notification, person:Person)
}

class ExampleRule(val condition: (person: Person) -> Boolean, val description: String): Rule {
    override fun check(notification:Notification, person:Person) {
        if (!condition(person)) {
            notification.errors.add("Validation failed for: ${description}")
        }
    }
}

class MustHave(val property: String): Rule {
    override fun check(notification:Notification, person:Person) {
        val value = person.javaClass.getMethod("get"+property.capitalize()).invoke(person)
        if (value==null) {
            notification.errors.add("Validation failed for: ${property}")
        }
    }
}

class Engine {
    val rules = mutableListOf<Rule>()

    fun  addRule(init: () -> Rule) {
        rules.add(init())
    }

    fun runRules(person:Person):Notification {
        val result=Notification()
        for (rule in rules) {
            rule.check(result,person)
        }
        return result
    }
}

fun main(args : Array<String>) {
    val tim = Person("Tim",income = 100)

    val concreteEngine = Engine()
    concreteEngine.rules.add(ExampleRule({p -> p.nationality!=null},"Missing Nationality"))
    println(concreteEngine.runRules(tim).errors)
    concreteEngine.rules.clear()
    concreteEngine.rules.add(MustHave("income"))
    concreteEngine.rules.add(MustHave("university"))
    println(concreteEngine.runRules(tim).errors)
    concreteEngine.rules.clear()
}
