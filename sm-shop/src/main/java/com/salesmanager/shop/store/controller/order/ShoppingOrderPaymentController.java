package com.salesmanager.shop.store.controller.order;

import com.ibs.pg.java.model.*;
import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.customer.attribute.CustomerOptionService;
import com.salesmanager.core.business.services.customer.attribute.CustomerOptionValueService;
import com.salesmanager.core.business.services.order.OrderService;
import com.salesmanager.core.business.services.payments.PaymentService;
import com.salesmanager.core.business.services.payments.TransactionService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.services.shoppingcart.ShoppingCartService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.OrderTotalSummary;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.customer.Address;
import com.salesmanager.shop.model.order.ShopOrder;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.store.controller.order.facade.OrderFacade;
import com.salesmanager.shop.store.controller.shoppingCart.facade.ShoppingCartFacade;
import com.salesmanager.shop.utils.LabelUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mobile.device.Device;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;

@Controller
@RequestMapping(Constants.SHOP_URI)
public class ShoppingOrderPaymentController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ShoppingOrderPaymentController.class);

    private final static String INIT_ACTION = "init";


    @Inject
    private ShoppingCartFacade shoppingCartFacade;

    @Inject
    private ShoppingCartService shoppingCartService;

    @Inject
    private LanguageService languageService;

    @Inject
    private PaymentService paymentService;

    @Inject
    private OrderService orderService;

    @Inject
    private CountryService countryService;

    @Inject
    private ZoneService zoneService;

    @Inject
    private OrderFacade orderFacade;

    @Inject
    private LabelUtils messages;

    @Inject
    private PricingService pricingService;

    @Inject
    private CustomerService customerService;

    @Inject
    private CustomerOptionService customerOptionService;

    @Inject
    private CustomerOptionValueService customerOptionValueService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private CoreConfiguration coreConfiguration;

    /**
     * Recalculates shipping and tax following a change in country or province
     *
     * @param order
     * @param request
     * @param response
     * @param locale
     * @return
     * @throws Exception
     */
    @RequestMapping(value = {"/order/payment/{action}/{paymentmethod}.html"}, method = RequestMethod.POST)
    public @ResponseBody
    String paymentAction(@Valid @ModelAttribute(value = "order") ShopOrder order, @PathVariable String action, @PathVariable String paymentmethod, Device device, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {

        Address billing = order.getCustomer().getBilling();
        OrderTotalSummary totalSummary = super.getSessionAttribute(Constants.ORDER_SUMMARY, request);
        double amount = totalSummary.getTotal().doubleValue()*7.8115;
        Language language = (Language) request.getAttribute("LANGUAGE");
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.MERCHANT_STORE);
        String shoppingCartCode = getSessionAttribute(Constants.SHOPPING_CART, request);

        Validate.notNull(shoppingCartCode, "shoppingCartCode does not exist in the session");
        AjaxResponse ajaxResponse = new AjaxResponse();
        com.salesmanager.core.model.shoppingcart.ShoppingCart cart = shoppingCartFacade.getShoppingCartModel(shoppingCartCode, store);
        Set<ShoppingCartItem> items = cart.getLineItems();
        List<ShoppingCartItem> cartItems = new ArrayList<ShoppingCartItem>(items);

        order.setShoppingCartItems(cartItems);
        String receiverName = billing.getFirstName() + billing.getLastName();
        String phone = billing.getPhone();
        String address = billing.getCountry() + billing.getZone() + billing.getAddress();

        long id = (long) ((Math.random())*100000000);
        String name = items.iterator().next().getProduct().getSku();
        OrderItem orderItem = new OrderItem(UUID.randomUUID().toString().substring(0,15), OrderItemType.OTHER, name, 1, name, amount);
        Order pgOrder = new Order(UUID.randomUUID().toString().substring(0,15), TransCode.TC01121990, "pay", amount, true).addOrderItem(orderItem);
        Risk risk = new Risk(receiverName, phone, address, GoodsType.REAL, true);

        HttpSession session = request.getSession();
        session.setAttribute("pgOrder",pgOrder);
        session.setAttribute("risk",risk);
        return "[]";
    }

    //cancel - success paypal order
    @RequestMapping(value = {"/paypal/checkout.html/{code}"}, method = RequestMethod.GET)
    public String returnPayPalPayment(@PathVariable String code, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {
        if (Constants.SUCCESS.equals(code)) {
            return "redirect:" + Constants.SHOP_URI + "/order/commitPreAuthorized.html";
        } else {//process as cancel
            return "redirect:" + Constants.SHOP_URI + "/order/checkout.html";
        }
    }

}
