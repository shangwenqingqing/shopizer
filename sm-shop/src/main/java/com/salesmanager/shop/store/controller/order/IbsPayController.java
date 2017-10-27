package com.salesmanager.shop.store.controller.order;

import com.ibs.pg.java.InitiatePaymentRequest;
import com.ibs.pg.java.PgClient;
import com.ibs.pg.java.Response;
import com.ibs.pg.java.VerifyPaymentRequest;
import com.ibs.pg.java.model.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.util.UUID;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author WQ Shang
 */
@RestController
@RequestMapping("/pay")
public class IbsPayController {
    @Inject
    private PgClient client;

    @ResponseBody
    @RequestMapping(value = "/placeOrder", method = POST)
    public String placeOrder(@RequestBody BankCard bankCard, HttpSession session) {
        if ("".equals(bankCard.getCvv2())||"".equals(bankCard.getValidDate())){
            bankCard.setCvv2(null);
            bankCard.setValidDate(null);
        }
        PayerInfo payerInfo = new PayerInfo();
        payerInfo.setBankCard(bankCard);
        long appId = 1;
        String subject = "test";
        String userIp = "106.38.120.122";
        Order order = (Order) session.getAttribute("pgOrder");
        Risk risk = (Risk) session.getAttribute("risk");
        double amount = order.getAmount();
        InitiatePaymentRequest initiatePaymentRequest = new InitiatePaymentRequest(appId, UUID.randomUUID().toString(), order.getTransType(), amount, userIp,
                "http://59.110.8.169:8000/notification/payment", payerInfo, risk).addOrder(order).ofUMFBank();
        Response response = client.initiate(initiatePaymentRequest);
        session.setAttribute("paymentId", response.getPaymentId());

        return response.getMessage() == null ? "please input the verify code " : response.getMessage();

    }

    @ResponseBody
    @RequestMapping(value = "/confirm" , method = POST)
    public String confirmPay(@RequestBody VerifyCode reqBody, HttpSession session) {
        String paymentId = (String) session.getAttribute("paymentId");
        VerifyPaymentRequest verifyPaymentRequest = new VerifyPaymentRequest(paymentId, reqBody.getCode());
        System.out.println(verifyPaymentRequest.getPaymentId());
        client.verify(verifyPaymentRequest);
        return "success";
    }

    @ResponseBody
    @RequestMapping("/scanPay")
    public String initiateScanPay(@RequestBody ScanInfo scanInfo, HttpSession session) {
        PayerInfo payerInfo = new PayerInfo();
        payerInfo.setScanInfo(scanInfo);
        long appId = 1;
        String userIp = "106.38.120.122";
        Order order = (Order) session.getAttribute("pgOrder");
        Risk risk = (Risk) session.getAttribute("risk");
        InitiatePaymentRequest initiatePaymentRequest = new InitiatePaymentRequest(appId, UUID.randomUUID().toString(), order.getTransType(), order.getAmount(), userIp,
                "http://59.110.8.169:8000/notification/payment", payerInfo, risk).addOrder(order).ofUMFAli();
        Response response = client.initiate(initiatePaymentRequest);
        if (null != response.getMessage()) {
            throw new RuntimeException(response.getMessage());
        }
        return response.getScanCodeUrl();
    }
}
