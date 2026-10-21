package org.church.steward.domain.budget.transformer;

import java.util.Objects;

import org.church.steward.domain.activitylog.transformer.LoggableTransformer;
import org.church.steward.domain.budget.entity.Budget;

public class BudgetTransformer implements LoggableTransformer {

    @Override
    public Object transform(Object value) {
        if (Objects.isNull(value)) {
            return "(없음)";
        }

        if (value instanceof Budget budget) {
            return budget.getHierarchyName();
        }

        throw new IllegalArgumentException();
    }

}
