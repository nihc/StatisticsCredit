package com.cardpay.pccredit.bank.model;

import java.math.BigDecimal;
import java.util.Date;

public class CustomerAccountBill {
    private String id;

    private String accountNumber;

    private String overdueAmount;

    private String currentMonthInterestAccount;

    private String paybackAccount;

    private Date createdDate;

    private String overdraftAccount;

    private BigDecimal billDataYear;

    private BigDecimal billDataMonth;

    private String paidInterestAccount;

    private String balanceAccount;

    private String perdayCreditlineAccount;
    
    private String cardNumber;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? null : id.trim();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber == null ? null : accountNumber.trim();
    }

    public String getOverdueAmount() {
        return overdueAmount;
    }

    public void setOverdueAmount(String overdueAmount) {
        this.overdueAmount = overdueAmount == null ? null : overdueAmount.trim();
    }

    public String getCurrentMonthInterestAccount() {
        return currentMonthInterestAccount;
    }

    public void setCurrentMonthInterestAccount(String currentMonthInterestAccount) {
        this.currentMonthInterestAccount = currentMonthInterestAccount == null ? null : currentMonthInterestAccount.trim();
    }

    public String getPaybackAccount() {
        return paybackAccount;
    }

    public void setPaybackAccount(String paybackAccount) {
        this.paybackAccount = paybackAccount == null ? null : paybackAccount.trim();
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getOverdraftAccount() {
        return overdraftAccount;
    }

    public void setOverdraftAccount(String overdraftAccount) {
        this.overdraftAccount = overdraftAccount == null ? null : overdraftAccount.trim();
    }

    public BigDecimal getBillDataYear() {
        return billDataYear;
    }

    public void setBillDataYear(BigDecimal billDataYear) {
        this.billDataYear = billDataYear;
    }

    public BigDecimal getBillDataMonth() {
        return billDataMonth;
    }

    public void setBillDataMonth(BigDecimal billDataMonth) {
        this.billDataMonth = billDataMonth;
    }

    public String getPaidInterestAccount() {
        return paidInterestAccount;
    }

    public void setPaidInterestAccount(String paidInterestAccount) {
        this.paidInterestAccount = paidInterestAccount == null ? null : paidInterestAccount.trim();
    }

    public String getBalanceAccount() {
        return balanceAccount;
    }

    public void setBalanceAccount(String balanceAccount) {
        this.balanceAccount = balanceAccount == null ? null : balanceAccount.trim();
    }

    public String getPerdayCreditlineAccount() {
        return perdayCreditlineAccount;
    }

    public void setPerdayCreditlineAccount(String perdayCreditlineAccount) {
        this.perdayCreditlineAccount = perdayCreditlineAccount == null ? null : perdayCreditlineAccount.trim();
    }

	public String getCardNumber() {
		return cardNumber;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}
}