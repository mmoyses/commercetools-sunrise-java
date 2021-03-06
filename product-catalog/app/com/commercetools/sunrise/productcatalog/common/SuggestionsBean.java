package com.commercetools.sunrise.productcatalog.common;

import com.commercetools.sunrise.common.models.ViewModel;

import java.util.List;

public class SuggestionsBean extends ViewModel {

    private List<ProductThumbnailBean> list;

    public SuggestionsBean() {
    }

    public SuggestionsBean(final ProductListBean productListData) {
        this.list = productListData.getList();
    }

    public List<ProductThumbnailBean> getList() {
        return list;
    }

    public void setList(final List<ProductThumbnailBean> list) {
        this.list = list;
    }
}
