package com.commercetools.sunrise.shoppingcart;

import com.commercetools.sunrise.common.models.ViewModel;

public class PaymentInfoBean extends ViewModel {

    private String type;

    public PaymentInfoBean() {
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
