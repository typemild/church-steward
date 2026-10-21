package org.church.steward.domain.budget.transformer;

import org.church.steward.domain.activitylog.transformer.LoggableTransformer;
import org.church.steward.domain.budget.entity.BudgetType;

public class BudgetTypeTransformer implements LoggableTransformer {

    @Override
    public Object transform(Object value) {
        if (value instanceof BudgetType type) {
            return type.getLabel();
        }

        throw new IllegalArgumentException();
    }

}
