package com.commercetools.sunrise.shoppingcart.removelineitem;

import com.commercetools.sunrise.common.controllers.ReverseRouter;
import com.commercetools.sunrise.common.forms.UserFeedback;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import com.google.inject.Injector;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.RemoveLineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.RequireCSRFCheck;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static io.sphere.sdk.utils.FutureUtils.recoverWithAsync;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.libs.concurrent.HttpExecution.defaultContext;

public abstract class SunriseRemoveLineItemController extends SunriseFrameworkCartController {
    private static final Logger logger = LoggerFactory.getLogger(SunriseRemoveLineItemController.class);

    @Inject
    private ReverseRouter reverseRouter;
    @Inject
    private Injector injector;

    @RequireCSRFCheck
    public CompletionStage<Result> removeLineItem(final String languageTag) {
        return doRequest(() -> {
            final Form<RemoveLineItemFormData> removeLineItemForm = formFactory().form(RemoveLineItemFormData.class).bindFromRequest();
            return removeLineItemForm.hasErrors() ? handleRemoveLineItemFormErrors(removeLineItemForm) : handleValidForm(removeLineItemForm);
        });
    }

    private CompletionStage<Result> handleValidForm(final Form<RemoveLineItemFormData> removeLineItemForm) {
        return getOrCreateCart()
                .thenComposeAsync(cart -> {
                    final String lineItemId = removeLineItemForm.get().getLineItemId();
                    final CompletionStage<Result> resultStage = removeLineItem(lineItemId, cart)
                            .thenComposeAsync(updatedCart -> handleSuccessfulCartChange(updatedCart), defaultContext());
                    return recoverWithAsync(resultStage, defaultContext(), throwable ->
                            handleRemoveLineItemError(throwable, removeLineItemForm, cart));
                }, defaultContext());
    }

    protected CompletionStage<Cart> removeLineItem(final String lineItemId, final Cart cart) {
        final RemoveLineItem removeLineItem = RemoveLineItem.of(lineItemId);
        return sphere().execute(CartUpdateCommand.of(cart, removeLineItem));
    }

    //TODO this is duplicated
    protected CompletionStage<Result> handleSuccessfulCartChange(final Cart cart) {
        overrideCartSessionData(cart);
        return completedFuture(redirect(reverseRouter.showCart(userContext().languageTag())));
    }

    protected CompletionStage<Result> handleRemoveLineItemFormErrors(final Form<RemoveLineItemFormData> removeLineItemForm) {
        injector.getInstance(UserFeedback.class).addErrors(removeLineItemForm);
        return completedFuture(redirect(reverseRouter.showCart(userContext().languageTag())));
    }

    protected CompletionStage<Result> handleRemoveLineItemError(final Throwable throwable,
                                                                final Form<RemoveLineItemFormData> removeLineItemForm,
                                                                final Cart cart) {
        injector.getInstance(UserFeedback.class).addErrors("The request to change line item quantity raised an exception");// TODO get from i18n
        return completedFuture(redirect(reverseRouter.showCart(userContext().languageTag())));
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("cart"));
    }
}
