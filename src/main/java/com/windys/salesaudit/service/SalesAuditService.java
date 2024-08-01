package com.windys.salesaudit.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.windys.salesaudit.model.AuditEntry;
import com.windys.salesaudit.model.DataRecord;
import com.windys.salesaudit.model.DateControl;
import com.windys.salesaudit.model.FiscalData;
import com.windys.salesaudit.model.SalesAuditResponse;
import com.windys.salesaudit.model.Site;
import com.windys.salesaudit.repository.SalesAuditRepository;
import com.windys.salesaudit.utility.Constants;
import com.windys.salesaudit.utility.SalesAuditUtility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Service
public class SalesAuditService {
	
	private static final Logger Logger = LoggerFactory.getLogger(SalesAuditService.class);

	@Autowired
	SalesAuditRepository salesAuditRepository;
	
	@Autowired
	SalesAuditUtility salesAuditUtility;
	
	@Value("label_suffix_char")
	String label_suffix_char;
	
	@Value("cash.to.count.field.label")
	String cash_to_count_field_label;

	public FiscalData getFiscalCalInfo(Date businessDat) {
		// TODO Auto-generated method stub
		return null;
	}

	public Site getSiteInfo(String selectedRestaurant) {
		// TODO Auto-generated method stub
		return null;
	}

	public AuditEntry getAuditEntry(String selectedRestaurant, DateControl dateControl) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateAuditEntry(AuditEntry auditEntry, String userId) {
		DataRecord dr = auditEntry.getAuditActual();

		// Calculate Adjusted Net Sales.
		Double sum = this.getAdjustedNetSales(dr);
		dr.setNetSales(sum.toString());

		// Calculate Gross Sales.
		sum = this.getGrossSales(dr);
		dr.setGrossSales(sum.toString());

		// Calculate Cash Over Short.
		sum = this.getCashOverShort(dr);
		dr.setCashOverShort(sum.toString());

		salesAuditRepository.updateAuditEntry(auditEntry, userId);
	}

	public void insertAuditEntry(AuditEntry auditEntry, String userId, String coCode) {
		DataRecord dr = auditEntry.getAuditActual();

		// Calculate Adjusted Net Sales.
		Double sum = this.getAdjustedNetSales(dr);
		dr.setNetSales(sum.toString());

		// Calculate Gross Sales.
		sum = this.getGrossSales(dr);
		dr.setGrossSales(sum.toString());

		// Calculate Cash Over Short.
		sum = this.getCashOverShort(dr);
		dr.setCashOverShort(sum.toString());

		salesAuditRepository.insertAuditEntry(auditEntry, userId, coCode);
		salesAuditRepository.updateSSAXCPEntry(auditEntry, userId, coCode);
	}

	/**
	 * For the given DataRecord, calculate the Cash Over Short value.
	 * 
	 * @param dr
	 * @return Double
	 */
	private Double getCashOverShort(DataRecord dr) {
		Double sum = Double.valueOf(dr.getDeposits()) - Double.valueOf(dr.getCashToCount());

		return sum;
	}

	/**
	 * For the given DataRecord, calculate the AdjustedNetSales value.
	 * 
	 * @param dr
	 * @return
	 */
	private Double getAdjustedNetSales(DataRecord dr) {
		Double sum = Double.valueOf(dr.getCashToCount()) + Double.valueOf(dr.getCreditSales())
				+ Double.valueOf(dr.getDebitSales()) + Double.valueOf(dr.getGcRedeemed()) + Double.valueOf(dr.getMcx())
				+ Double.valueOf(dr.getMobilePay()) - Double.valueOf(dr.getSalesTax())
				- Double.valueOf(dr.getUpgradeKidsMeal()) - Double.valueOf(dr.getGcSales())
				- Double.valueOf(dr.getDtfaDeposit()) - Double.valueOf(dr.getLocalPromo())
				- Double.valueOf(dr.getNationalPromo()) - Double.valueOf(dr.getFoodSurcharge());

		return sum;
	}

	/**
	 * For the given DataRecord, calculate the Gross Sales.
	 * 
	 * @param dr
	 * @return Double
	 */
	private Double getGrossSales(DataRecord dr) {
		Double sum = Double.valueOf(dr.getNetSales()) + Double.valueOf(dr.getMgrMeals())
				+ Double.valueOf(dr.getEmpMeals()) + Double.valueOf(dr.getCoupons())
				+ Double.valueOf(dr.getLocalPromo()) + Double.valueOf(dr.getNationalPromo())
				+ Double.valueOf(dr.getDiscounts()) + Double.valueOf(dr.getSalesTax())
				+ Double.valueOf(dr.getUpgradeKidsMeal()) + Double.valueOf(dr.getGcSales())
				+ Double.valueOf(dr.getFoodSurcharge());

		return sum;
	}

	public SalesAuditResponse deleteSalesAudit(@Valid @NotBlank String selectedDate,
			@Valid @NotBlank String selectedRestaurant, AuditEntry auditEntry) throws ParseException {
		SalesAuditResponse salesAuditResponse = new SalesAuditResponse();
		if (auditEntry.getAuditActual() != null) {
			Logger.debug(
					"auditEntry.getAuditActual().getDbRowExists() = " + auditEntry.getAuditActual().isDbRowExists());
		} else {
			Logger.debug("auditEntry.getAuditActual() == null");
		}
		if (auditEntry.getPolledActual() != null) {
			Logger.debug(
					"auditEntry.getPolledActual().getDbRowExists() = " + auditEntry.getPolledActual().isDbRowExists());
		} else {
			Logger.debug("auditEntry.getPolledActual() == null");
		}
		if (!selectedRestaurant.isEmpty() && !selectedDate.isEmpty()) {
			// No audit record, do not try and delete. Return and display message.
			if (!auditEntry.getAuditActual().isDbRowExists()) {
				salesAuditResponse.setErrorMsg(new ArrayList<>(List.of("\"audit.data.none\"")));
				return salesAuditResponse;
			}

			// Delete associated record from the database..
			DateControl dateControl = new DateControl(selectedDate);
			Date busDate = dateControl.getBusinessDat();

			salesAuditRepository.insertAuditDelete(selectedRestaurant, busDate);

			if (auditEntry.getPolledActual().isDbRowExists()) {
				salesAuditRepository.insertPolledDelete(selectedRestaurant, busDate);
				salesAuditRepository.deletePolledEntry(selectedRestaurant, busDate);
			}

			salesAuditRepository.deleteAuditEntry(selectedRestaurant, busDate);
			salesAuditResponse.setActionMsg("message.data.deleted");
		}
		return salesAuditResponse;
	}

	public SalesAuditResponse validateSalesAudit(@Valid @NotBlank String selectedDate,
			@Valid @NotBlank String selectedRestaurant, AuditEntry auditEntry) {
		String labelSuffix = label_suffix_char;

		if (!selectedRestaurant.isEmpty() && !selectedDate.isEmpty()) {
			// Validate dollar amounts.

			salesAuditUtility.validateStringDecimal(
					StringUtils.removeEnd(cash_to_count_field_label, labelSuffix),
					auditEntry.getAuditActual().getCashToCount());
			validateStringDecimal(StringUtils.removeEnd(getText("sales.tax.field.label"), labelSuffix),
					auditEntry.getAuditActual().getSalesTax());
			validateStringDecimal(StringUtils.removeEnd(getText("localpromo.field.label"), labelSuffix),
					auditEntry.getAuditActual().getLocalPromo());
			validateStringDecimal(StringUtils.removeEnd(getText("nationalpromo.field.label"), labelSuffix),
					auditEntry.getAuditActual().getNationalPromo());
			validateStringDecimal(StringUtils.removeEnd(getText("kmupgrade.field.label"), labelSuffix),
					auditEntry.getAuditActual().getUpgradeKidsMeal());
			validateStringDecimal(StringUtils.removeEnd(getText("coupons.field.label"), labelSuffix),
					auditEntry.getAuditActual().getCoupons());
			validateStringDecimal(StringUtils.removeEnd(getText("discounts.field.label"), labelSuffix),
					auditEntry.getAuditActual().getDiscounts());
			validateStringDecimal(StringUtils.removeEnd(getText("manager.meals.field.label"), labelSuffix),
					auditEntry.getAuditActual().getMgrMeals());
			validateStringDecimal(StringUtils.removeEnd(getText("employee.meals.field.label"), labelSuffix),
					auditEntry.getAuditActual().getEmpMeals());
			validateStringDecimal(StringUtils.removeEnd(getText("gift.sales.field.label"), labelSuffix),
					auditEntry.getAuditActual().getGcSales());
			validateStringDecimal(StringUtils.removeEnd(getText("gift.redeemed.field.label"), labelSuffix),
					auditEntry.getAuditActual().getGcRedeemed());
			validateStringDecimal(StringUtils.removeEnd(getText("credit.sales.field.label"), labelSuffix),
					auditEntry.getAuditActual().getCreditSales());
			validateStringDecimal(StringUtils.removeEnd(getText("credit.refund.field.label"), labelSuffix),
					auditEntry.getAuditActual().getCreditRefund());
			validateStringDecimal(StringUtils.removeEnd(getText("debt.sales.field.label"), labelSuffix),
					auditEntry.getAuditActual().getDebitSales());
			validateStringDecimal(StringUtils.removeEnd(getText("food.surcharge.field.label"), labelSuffix),
					auditEntry.getAuditActual().getFoodSurcharge());
			validateStringDecimal(StringUtils.removeEnd(getText("manager.void.field.label"), labelSuffix),
					auditEntry.getAuditActual().getMgrVoid());
			validateStringDecimal(StringUtils.removeEnd(getText("refunds.field.label"), labelSuffix),
					auditEntry.getAuditActual().getRefunds());
			validateStringDecimal(StringUtils.removeEnd(getText("mobile.pay.field.label"), labelSuffix),
					auditEntry.getAuditActual().getMobilePay());
			validateStringDecimal(StringUtils.removeEnd(getText("mobile.pay.refund.field.label"), labelSuffix),
					auditEntry.getAuditActual().getMobilePayRefund());
			validateStringDecimal(StringUtils.removeEnd(getText("gift.card.cash.back.field.label"), labelSuffix),
					auditEntry.getAuditActual().getGcCashBack());
			validateStringDecimal(StringUtils.removeEnd(getText("mcx.field.label"), labelSuffix),
					auditEntry.getAuditActual().getMcx());
			validateStringDecimal(StringUtils.removeEnd(getText("local.prepay.field.label"), labelSuffix),
					auditEntry.getAuditActual().getLocalPrePay());
			validateStringDecimal(StringUtils.removeEnd(getText("dtfadeposits.field.label"), labelSuffix),
					auditEntry.getAuditActual().getDtfaDeposit());
			validateStringDecimal(StringUtils.removeEnd(getText("tax.exempt.field.label"), labelSuffix),
					auditEntry.getAuditActual().getTaxExempt());

			// Validate date entered.
			DateTime dt = Constants.dateFormatter.parseDateTime(selectedDate);
			Logger.debug("selectedDate = " + selectedDate);
			Logger.debug("dt.getDayOfYear() = " + dt.getDayOfYear());
			Logger.debug("dt.getYear() = " + dt.getYear());

		} else {
			Logger.debug("Nothing to validate in DailySalesEntry...");
		}
		return null;
	}

}
