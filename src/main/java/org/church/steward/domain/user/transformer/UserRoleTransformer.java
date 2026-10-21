package org.church.steward.domain.user.transformer;

import org.church.steward.domain.activitylog.transformer.LoggableTransformer;
import org.church.steward.domain.user.entity.UserRole;

public class UserRoleTransformer implements LoggableTransformer {

    @Override
    public Object transform(Object value) {
        if (value instanceof UserRole role) {
            return role.getLabel();
        }

        throw new IllegalArgumentException();
    }

}
