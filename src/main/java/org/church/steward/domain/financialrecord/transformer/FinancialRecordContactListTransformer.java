package org.church.steward.domain.financialrecord.transformer;

import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.activitylog.transformer.LoggableTransformer;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;

public class FinancialRecordContactListTransformer implements LoggableTransformer {

    @Override
    public Object transform(Object value) {
        if (value instanceof List list) {
            ArrayList<String> contactNameList = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item instanceof FinancialRecordContact contact) {
                    contactNameList.add(contact.getContactName());
                } else {
                    throw new IllegalArgumentException();
                }
            }

            return String.join(", ", contactNameList);
        }

        throw new IllegalArgumentException();
    }

}
