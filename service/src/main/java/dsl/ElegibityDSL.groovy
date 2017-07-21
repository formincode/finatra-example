package dsl

import rulesengine.Application
import rulesengine.Person

/**
 * Created by msrivastava on 7/21/17.
 */
class ElegibityDSL {
    static boolean init_rules = true
    def static initRules = {
        init_rules = false
    }

    static void loadRules(Application application) {
        Binding binding = new Binding()
        binding.application = application
        binding.result=true
        binding.log = []

        prepareClosures(binding)

        Script shell = new GroovyShell(binding).parse(new File("service/src/main/resources/elegibility.groovy"))
        while (binding.result) {
            shell.run()
        }
    }

    static void prepareClosures (Binding binding) {

        binding.initRules = { closure ->
            closure.delegate = delegate
            closure()
        }

        binding.rule = { spec, closure ->
            closure.delegate = delegate
            binding.result = true
            closure()
        }

        binding.action = { closure ->
            closure.delegate = delegate

            if (binding.result) {
                closure()
            }
        }

        binding.condition = { closure ->
            closure.delegate = delegate

            binding.result = (closure() && binding.result)
        }
    }

    static void main(String[] args) {
        loadRules(new Application(new Person("Tim",null,null,null)))
    }
}