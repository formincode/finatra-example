package rewards

/**
 * Created by msrivastava on 7/21/17.
 */
class RewardService {
    static boolean on_consume_provided = true
    def static onConsume = {
        on_consume_provided = false
    }
    static boolean on_purchase_provided = true
    def static onPurchase = {
        on_purchase_provided = false
    }
    static boolean on_upgrade_provided = true
    def static onUpgrade = {
        on_upgrade_provided = false
    }

    void prepareClosures (Binding binding) {
        binding.onConsume = onConsume
        binding.onPurchase = onPurchase
        binding.onUpgrade = onUpgrade
        binding.reward = { spec, closure ->
            closure.delegate = delegate
            binding.result = true
            binding.and = true
            closure()
        }
        binding.condition = { closure ->
            closure.delegate = delegate

            if (binding.and)
                binding.result = (closure() && binding.result)
            else
                binding.result = (closure() || binding.result)
        }

        binding.allOf = { closure ->
            closure.delegate = delegate
            def storeResult = binding.result
            def storeAnd = binding.and
            binding.result = true // Starting premise is true
            binding.and = true

            closure()

            if (storeAnd) {
                binding.result = (storeResult && binding.result)
            } else {
                binding.result = (storeResult || binding.result)
            }
            binding.and = storeAnd
        }

        binding.anyOf = { closure ->
            closure.delegate = delegate
            def storeResult = binding.result
            def storeAnd = binding.and

            binding.result = false // Starting premise is false
            binding.and = false

            closure()
            if (storeAnd) {
                binding.result = (storeResult && binding.result)
            } else {
                binding.result = (storeResult || binding.result)
            }
            binding.and = storeAnd
        }

        binding.grant = { closure ->
            closure.delegate = delegate

            if (binding.result)
                closure()
        }
        binding.extend = { days ->
            def bbPlus = new BroadbandPlus()
            bbPlus.extend( binding.account, binding.media, days)
        }
        binding.points = { points ->
            def bbPlus = new BroadbandPlus()
            binding.account.points += points
        }
    }

    void prepareMedia(binding, media) {
        binding.media = media
        binding.isNewRelease = media.newRelease
        binding.isVideo = (media.type == "VIDEO")
        binding.isGame = (media.type == "GAME")
        binding.isSong = (media.type == "SONG")
    }

    static void loadRewardRules() {
        Binding binding = new Binding()

        binding.onConsume = onConsume
        binding.onPurchase = onPurchase
        binding.onUpgrade = onUpgrade

        GroovyShell shell = new GroovyShell(binding)
        shell.evaluate(new File("service/src/main/resources/rewards.groovy"))

        onConsume = binding.onConsume
        onPurchase = binding.onPurchase
        onUpgrade = binding.onUpgrade
    }

    void applyRewardsOnConsume(account, media) {
        if (on_consume_provided) {
            Binding binding = new Binding()
            binding.account = account
            prepareClosures(binding)
            prepareMedia(binding, media)

            GroovyShell shell = new GroovyShell(binding)
            shell.evaluate("onConsume.delegate = this;onConsume()")
        }
    }

    void applyRewardsOnPurchase(account, media) {
        if (on_purchase_provided) {
            Binding binding = new Binding()
            binding.account = account
            prepareClosures(binding)
            prepareMedia(binding, media)

            GroovyShell shell = new GroovyShell(binding)
            shell.evaluate("on_purchase.delegate = this;onPurchase()")
        }
    }

    void applyRewardsOnUpgrade(account, plan) {
        if (on_upgrade_provided) {
            Binding binding = new Binding()
            binding.account = account
            binding.to_plan = plan
            binding.from_plan = account.plan
            prepareClosures(binding)

            GroovyShell shell = new GroovyShell(binding)
            shell.evaluate("on_upgrade.delegate = this;onUpgrade()")
        }
    }

    static void main(String[] args) {
        def account = new Account(plan:"BASIC", points:120, spend:0.0)
        def up = new Media(title:"UP", type:"VIDEO", newRelease:true,
                price:3.99, points:40, daysAccess:1,
                publisher:"Disney")
        def terminator = new Media(title:"Terminator", type:"VIDEO",
                newRelease:false, price:2.99, points:30,
                daysAccess:1, publisher:"Fox")
        def halo3 = new Media(title:"Halo III", type:"GAME",
                newRelease:true, price:2.99, points:30,
                daysAccess:3, publisher:"Microsoft")
        def halo1 = new Media(title:"Halo", type:"GAME",
                newRelease:false, price:1.99, points:20,
                daysAccess:3,publisher:"Microsoft")
        def bbPlus = new BroadbandPlus()
        RewardService.loadRewardRules()

        def expected = account.points - up.points + up.points / 4
        bbPlus.consume(account, up)
        println(account.points == expected)
    }
}