package org.church.steward.domain.budget.transformer;

import java.util.Objects;

import org.church.steward.domain.activitylog.transformer.LoggableTransformer;

public class BudgetAmountTransformer implements LoggableTransformer {

    @Override
    public Object transform(Object value) {
        if (Objects.isNull(value)) {
            // Budget의 amount가 null이면 자식 예산 항목이 등록된 것이다.
            return "(하위 예산 항목이 등록됨)";
        }

        return value;
    }

}
