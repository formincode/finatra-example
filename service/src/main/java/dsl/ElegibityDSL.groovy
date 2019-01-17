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

    static Script createScript() {
        Binding binding = new Binding()
        Script shell = new GroovyShell(binding).parse(new File("service/src/main/resources/elegibility.groovy"))
        return shell;
    }

    static void loadRules(Application application,Script shell) {
        shell.binding.variables.clear()
        prepareClosures(shell.binding)
        shell.binding.application = application
        shell.binding.result=true
        shell.binding.log = []

        while (shell.binding.result) {
            shell.run()
        }
        println(shell.binding.log)
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
            binding.result = closure()
        }
    }

    static void main(String[] args) {
        Script shell = createScript()
        loadRules(new Application(new Person("Tim",null,null,null)),shell)
        loadRules(new Application(new Person("frank",null,null,null)),shell)
    }
}