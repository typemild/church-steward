package org.church.steward.domain.activitylog.transformer;

import java.util.Objects;

public class BooleanTypeTransformer implements LoggableTransformer {

    @Override
    public Object transform(Object value) {
        if (Objects.isNull(value)) {
            return "";
        }

        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "O" : "X";
        }

        throw new IllegalArgumentException();
    }

}
