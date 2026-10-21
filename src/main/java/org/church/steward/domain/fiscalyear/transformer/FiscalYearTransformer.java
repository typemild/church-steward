package org.church.steward.domain.fiscalyear.transformer;

import org.church.steward.domain.activitylog.transformer.LoggableTransformer;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;

public class FiscalYearTransformer implements LoggableTransformer {

    @Override
    public Object transform(Object value) {
        if (value instanceof FiscalYear fiscalYear) {
            return fiscalYear.getName();
        }

        throw new IllegalArgumentException();
    }

}
