package org.church.steward.domain.contact.transformer;

import org.church.steward.domain.activitylog.transformer.LoggableTransformer;
import org.church.steward.domain.contact.entity.ContactType;

public class ContactTypeTransformer implements LoggableTransformer {

    @Override
    public Object transform(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof ContactType type) {
            return type.getLabel();
        }

        throw new IllegalArgumentException();
    }

}
