package com.dotcms.rest.api.v1.sites.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;

import java.util.Map;

import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@JsonDeserialize(builder = RestRuleAction.Builder.class)
public class RestRuleAction extends Validated {

    public final String id;

    @NotNull
    public final String name;

    @NotNull
    public final String owningRule;

    public final int priority;

    @NotNull
    public final String actionlet;

    public final Map<String, RestRuleActionParameter> parameters;

    private RestRuleAction(Builder builder) {
        id = builder.id;
        name = builder.name;
        owningRule = builder.owningRule;
        priority = builder.priority;
        actionlet = builder.actionlet;
        parameters = builder.parameters;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String id; // not present on create
        @JsonProperty(required = true) private String name;
        @JsonProperty(required = true) private String owningRule;
        @JsonProperty(required = true) private String actionlet;
        @JsonProperty private Map<String, RestRuleActionParameter> parameters;
        @JsonProperty private int priority=0;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder owningRule(String owningRule) {
            this.owningRule = owningRule;
            return this;
        }

        public Builder actionlet(String actionlet) {
            this.actionlet = actionlet;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder parameters(Map<String, RestRuleActionParameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public RestRuleAction build() {
            return new RestRuleAction(this);
        }
    }
}
