package com.commercetools.sunrise.myaccount.addressbook.addaddress;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.WithFormFlow;
import com.commercetools.sunrise.common.controllers.WithTemplateName;
import com.commercetools.sunrise.framework.annotations.IntroducingMultiControllerComponents;
import com.commercetools.sunrise.framework.annotations.SunriseRoute;
import com.commercetools.sunrise.myaccount.addressbook.AddressBookAddressFormData;
import com.commercetools.sunrise.myaccount.addressbook.DefaultAddressBookAddressFormData;
import com.commercetools.sunrise.myaccount.addressbook.SunriseAddressBookManagementController;
import io.sphere.sdk.client.ClientErrorException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.SetDefaultBillingAddress;
import io.sphere.sdk.customers.commands.updateactions.SetDefaultShippingAddress;
import io.sphere.sdk.models.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@RequestScoped
@IntroducingMultiControllerComponents(SunriseAddAddressHeroldComponent.class)
public abstract class SunriseAddAddressController extends SunriseAddressBookManagementController implements WithTemplateName, WithFormFlow<AddressBookAddressFormData, Customer, Customer> {

    private static final Logger logger = LoggerFactory.getLogger(SunriseAddAddressController.class);

    @Override
    public Set<String> getFrameworkTags() {
        final Set<String> frameworkTags = super.getFrameworkTags();
        frameworkTags.addAll(asList("address-book", "add-address", "address"));
        return frameworkTags;
    }

    @Override
    public String getTemplateName() {
        return "my-account-new-address";
    }

    @Override
    public Class<? extends AddressBookAddressFormData> getFormDataClass() {
        return DefaultAddressBookAddressFormData.class;
    }

    @SunriseRoute("addAddressToAddressBookCall")
    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> {
            logger.debug("show new address form for address in locale={}", languageTag);
            return requireExistingCustomer()
                    .thenComposeAsync(this::showForm, HttpExecution.defaultContext());
        });
    }

    @SunriseRoute("addAddressToAddressBookProcessFormCall")
    public CompletionStage<Result> process(final String languageTag) {
        return doRequest(() -> {
            logger.debug("try to add address with in locale={}", languageTag);
            return requireExistingCustomer()
                    .thenComposeAsync(this::validateForm, HttpExecution.defaultContext());
        });
    }

    @Override
    public CompletionStage<? extends Customer> doAction(final AddressBookAddressFormData formData, final Customer customer) {
        final Address address = formData.toAddress();
        return addAddress(customer, address)
                .thenComposeAsync(updatedCustomer -> findAddressId(updatedCustomer, address)
                        .map(addressId -> setAddressAsDefault(updatedCustomer, addressId, formData))
                        .orElseGet(() -> completedFuture(updatedCustomer)));
    }

    @Override
    public CompletionStage<Result> handleClientErrorFailedAction(final Form<? extends AddressBookAddressFormData> form, final Customer customer, final ClientErrorException clientErrorException) {
        saveUnexpectedFormError(form, clientErrorException, logger);
        return asyncBadRequest(renderPage(form, customer, null));
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final AddressBookAddressFormData formData, final Customer oldCustomer, final Customer updatedCustomer) {
        return redirectToAddressBook();
    }

    @Override
    public CompletionStage<Html> renderPage(final Form<? extends AddressBookAddressFormData> form, final Customer customer, @Nullable final Customer updatedCustomer) {
        final Customer customerToRender = Optional.ofNullable(updatedCustomer).orElse(customer);
        final AddAddressPageContent pageContent = injector().getInstance(AddAddressPageContentFactory.class).create(form, customerToRender);
        return renderPageWithTemplate(pageContent, getTemplateName());
    }

    @Override
    public void fillFormData(final AddressBookAddressFormData formData, final Customer customer) {
        final Address address = Address.of(userContext().country())
                .withTitle(customer.getTitle())
                .withFirstName(customer.getFirstName())
                .withLastName(customer.getLastName())
                .withEmail(customer.getEmail());
        formData.applyAddress(address);
    }

    protected final Optional<String> findAddressId(final Customer customer, final Address addressWithoutId) {
        return customer.getAddresses().stream()
                .filter(address -> address.equalsIgnoreId(addressWithoutId))
                .findFirst()
                .map(Address::getId);
    }

    private CompletionStage<Customer> addAddress(final Customer customer, final Address address) {
        final AddAddress addAddressAction = AddAddress.of(address);
        return sphere().execute(CustomerUpdateCommand.of(customer, addAddressAction));
    }

    private <T extends AddressBookAddressFormData> CompletionStage<Customer> setAddressAsDefault(final Customer customer, final String addressId, final T formData) {
        final List<UpdateAction<Customer>> updateActions = new ArrayList<>();
        if (formData.isDefaultShippingAddress()) {
            updateActions.add(SetDefaultShippingAddress.of(addressId));
        }
        if (formData.isDefaultBillingAddress()) {
            updateActions.add(SetDefaultBillingAddress.of(addressId));
        }
        if (!updateActions.isEmpty()) {
            return sphere().execute(CustomerUpdateCommand.of(customer, updateActions));
        } else {
            return completedFuture(customer);
        }
    }
}
