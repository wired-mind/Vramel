package com.nxttxn.vramel.model.language;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.spi.Language;
import com.nxttxn.vramel.util.ExpressionToPredicateAdapter;
import com.nxttxn.vramel.util.IntrospectionSupport;
import com.nxttxn.vramel.util.ObjectHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/25/13
 * Time: 11:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionDefinition implements Expression, Predicate{
    private String expression;
    private Expression expressionValue;
    private Predicate predicate;
    private ExpressionDefinition expressionType;
    private Boolean trim;

    public ExpressionDefinition(Predicate predicate) {

        this.predicate = predicate;
    }

    public ExpressionDefinition(Expression expression) {

        setExpressionValue(expression);
    }

    public ExpressionDefinition() {

    }

    public ExpressionDefinition(String expression) {

        this.expression = expression;
    }


    public Object evaluate(Exchange exchange) {
        return evaluate(exchange, Object.class);
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {

        if (expressionValue == null) {
            expressionValue = createExpression(exchange.getContext());
        }
        checkNotNull(expressionValue);
        return expressionValue.evaluate(exchange, type);
    }

    @Override
    public boolean matches(Exchange exchange) {
        if (predicate == null) {
            predicate = createPredicate(exchange.getContext());
        }

        checkNotNull(predicate);
        return predicate.matches(exchange);
    }

    public Predicate createPredicate(VramelContext vramelContext) {
        if (predicate == null) {
            if (getExpressionType() != null) {
                predicate = getExpressionType().createPredicate(vramelContext);
            } else if (getExpressionValue() != null) {
                predicate = new ExpressionToPredicateAdapter(getExpressionValue());
            } else if (getExpression() != null) {
                checkNotNull(getLanguage());
                Language language = vramelContext.resolveLanguage(getLanguage());
                String exp = getExpression();
                // trim if configured to trim
                if (exp != null && isTrim()) {
                    exp = exp.trim();
                }
                predicate = language.createPredicate(exp);
                configurePredicate(vramelContext, predicate);
            }
        }
        return predicate;
    }

    public Expression createExpression(VramelContext vramelContext) {
        if (getExpressionValue() == null) {
            if (getExpressionType() != null) {
                setExpressionValue(getExpressionType().createExpression(vramelContext));
            } else if (getExpression() != null) {
                ObjectHelper.notNull("language", getLanguage());
                Language language = vramelContext.resolveLanguage(getLanguage());
                String exp = getExpression();
                // trim if configured to trim
                if (exp != null && isTrim()) {
                    exp = exp.trim();
                }
                setExpressionValue(language.createExpression(exp));
                configureExpression(vramelContext, getExpressionValue());
            }
        }
        return getExpressionValue();
    }

    protected void configurePredicate(VramelContext vramelContext, Predicate predicate) {
    }

    protected void configureExpression(VramelContext vramelContext, Expression expression) {
    }

    public String getLanguage() {
        return "";
    }

    public ExpressionDefinition getExpressionType() {
        return expressionType;
    }

    public Expression getExpressionValue() {
        return expressionValue;
    }

    public String getExpression() {
        return expression;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    protected void setExpressionType(ExpressionDefinition expressionType) {
        this.expressionType = expressionType;
    }

    protected void setExpressionValue(Expression expressionValue) {
        this.expressionValue = expressionValue;
    }

    public Boolean getTrim() {
        return trim;
    }

    public void setTrim(Boolean trim) {
        this.trim = trim;
    }

    public boolean isTrim() {
        // trim by default
        return trim == null || trim;
    }

    /**
     * Returns some descriptive text to describe this node
     */
    public String getLabel() {
        String language = getExpression();
        if (ObjectHelper.isEmpty(language)) {
            Predicate predicate = getPredicate();
            if (predicate != null) {
                return predicate.toString();
            }
            Expression expressionValue = getExpressionValue();
            if (expressionValue != null) {
                return expressionValue.toString();
            }
        } else {
            return language;
        }
        return "";
    }

    public Predicate createPredicate(FlowContext flowContext) {
        return createPredicate(flowContext.getVramelContext());
    }

    public Expression createExpression(FlowContext flowContext) {
        return createExpression(flowContext.getVramelContext());
    }

    /**
     * Sets a named property on the object instance using introspection
     */
    protected void setProperty(Object bean, String name, Object value) {
        try {
            IntrospectionSupport.setProperty(bean, name, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set property " + name + " on " + bean
                    + ". Reason: " + e, e);

        }
    }
}