initRules {
    rule ( "test2" ) {
        condition {
            application.isGood
        }
        action {
            println("we have a winner")
        }
    }

    rule ( "test1" ) {
        condition {
             application.candidate.name!=null && !application.isGood
        }
        action {
            application.markAsGood()
        }
    }
}