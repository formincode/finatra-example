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
        println(binding.log)
    }

    static void prepareClosures (Binding binding) {

        binding.initRules = { closure ->
            closure()
        }

        binding.rule = { spec, closure ->
            binding.result = true
            if (!binding.log.contains(spec)) {
                binding.current=spec
                closure()
                binding.current=null
            } else {
                binding.result = false
            }
        }

        binding.action = { closure ->
            if (binding.result) {
                binding.log.add(binding.current)
                closure()
            }
        }

        binding.condition = { closure ->
            binding.result = (closure() && binding.result)
        }
    }

    static void main(String[] args) {
        loadRules(new Application(new Person("Tim",null,null,null)))
        loadRules(new Application(new Person("frank",null,null,null)))
    }
}