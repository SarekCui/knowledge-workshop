package com.knowledge.marketing.groupbuy.rule;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JoinRuleChain {

    private final List<JoinRule> rules;

    public JoinRuleChain(List<JoinRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public void check(JoinValidationContext context) {
        rules.forEach(rule -> rule.check(context));
    }
}
