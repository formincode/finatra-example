initRules {
    rule ( "test2" ) {
        condition {
            application.isGood
        }
        action {
            println("we have a winner")
        }
    }

    rule ( "test3" ) {
        condition {
              application.candidate.name=="frank"
        }
        action {
            println("we found frank")
        }
    }

    rule ( "test1" ) {
        condition {
             application.candidate.name!=null
        }
        action {
            application.markAsGood()
        }
    }
}