package demo.productcatalog;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.productcatalog.productoverview.SunriseProductOverviewController;
import com.commercetools.sunrise.productcatalog.productoverview.search.facetedsearch.FacetedSearchComponent;
import com.commercetools.sunrise.productcatalog.productoverview.search.pagination.PaginationComponent;
import com.commercetools.sunrise.productcatalog.productoverview.search.searchbox.SearchBoxComponent;
import com.commercetools.sunrise.productcatalog.productoverview.search.sort.SortSelectorComponent;

import javax.inject.Inject;

@RequestScoped
public class ProductOverviewController extends SunriseProductOverviewController {

    @Inject
    public void setSortSelectorComponent(final SortSelectorComponent component) {
        registerControllerComponent(component);
    }

    @Inject
    public void setPaginationComponent(final PaginationComponent component) {
        registerControllerComponent(component);
    }

    @Inject
    public void setSearchBoxComponent(final SearchBoxComponent component) {
        registerControllerComponent(component);
    }

    @Inject
    public void setFacetedSearchComponent(final FacetedSearchComponent component) {
        registerControllerComponent(component);
    }
}