package com.knowledge.marketing.groupbuy.rule;

import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JoinRuleChain {

    private List<JoinRule> rules;

    @Resource
    void setRules(List<JoinRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public void check(JoinValidationContext context) {
        rules.forEach(rule -> rule.check(context));
    }
}
