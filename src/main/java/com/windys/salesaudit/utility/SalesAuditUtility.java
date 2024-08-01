package com.windys.salesaudit.utility;

import java.text.ParseException;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.windys.salesaudit.model.AuditEntry;
import com.windys.salesaudit.model.DateControl;
import com.windys.salesaudit.model.FiscalData;
import com.windys.salesaudit.model.SalesAuditResponse;
import com.windys.salesaudit.model.Site;
import com.windys.salesaudit.service.SalesAuditService;

public class SalesAuditUtility {

	@Autowired
	FiscalData fiscalData;

	@Autowired
	SalesAuditService salesAuditService;

	@Autowired
	Site site;

	private int cutoffHour = 19;

	private static final Logger Logger = LoggerFactory.getLogger(SalesAuditUtility.class);

	public String validateSalesAuditData(String selectedDate, String selectedRestaurant) throws ParseException {
		String isPreviousPeriod = "F";

		// Setup the DateControl object using milliseconds from the date selected.
		DateControl dateControl = new DateControl(selectedDate);
		Logger.debug("dateControl=" + dateControl);

		// Get the Fiscal year and period of the current and requested dates.
		DateControl currentDc = new DateControl(new Date().getTime());
		FiscalData currentFiscalData = salesAuditService.getFiscalCalInfo(currentDc.getBusinessDat());
		FiscalData selectedFiscalData = salesAuditService.getFiscalCalInfo(dateControl.getBusinessDat());

		// Determine the relationship between today's fiscal period and the selected
		// period.
		if (currentFiscalData.getYearNum() == selectedFiscalData.getYearNum()) {
			if ((currentFiscalData.getPeriodNum() - selectedFiscalData.getPeriodNum()) == 1) {
				isPreviousPeriod = "T";
			}
		} else if ((currentFiscalData.getPeriodNum() - selectedFiscalData.getPeriodNum()) == 1) {
			if ((currentFiscalData.getPeriodNum() == 1) && (selectedFiscalData.getPeriodNum() == 12)) {
				// Current period is 1 and selected period is 12 of previous year.
				isPreviousPeriod = "T";
			}
		}

		Logger.debug("currentFiscalData = " + currentFiscalData);
		Logger.debug("selectedFiscalData = " + selectedFiscalData);
		Logger.debug("isPreviousPeriod = " + isPreviousPeriod);

		// If selected date is in the future return error.
		if (currentDc.getBusinessDat().compareTo(dateControl.getBusinessDat()) < 0) {
			isPreviousPeriod = "invalid.date.future";
		}

		return isPreviousPeriod;
	}

	public String validateAfterSite(Site site) {
		// If no site data found return error.
		if (!site.isDbRowExists()) {
			return "invalid.site.noData";
		}

		// If site is International site return error.
		if (site.getSiteStatus().equalsIgnoreCase("IS")) {
			return "invalid.site.international";
		}
		return null;
	}

	public String validatePostAuditEntry(String selectedDate, String isPreviousPeriod) throws ParseException {

		DateControl dateControl = new DateControl(selectedDate);

		//Calculate various dates and times.
        LocalDate todaysDate = new LocalDate();
        DateTime  now        = new DateTime();
        DateTime  tuesCutoff = todaysDate.withDayOfWeek(DateTimeConstants.TUESDAY).toDateTime(new LocalTime(cutoffHour,0));
        DateTime  monCutoff  = todaysDate.withDayOfWeek(DateTimeConstants.MONDAY).toDateTime(new LocalTime(cutoffHour,0));
        LocalDate thisMonday = todaysDate.withDayOfWeek(DateTimeConstants.MONDAY);
        LocalDate previousMonday = todaysDate.minusWeeks(1).withDayOfWeek(DateTimeConstants.MONDAY);
        LocalDate jodaSelectedDate = new LocalDate(dateControl.getBusinessDat());
        
		Logger.debug("todaysDate = " + todaysDate);
		Logger.debug("today is " + todaysDate.dayOfWeek().getAsText());
		Logger.debug("now = " + now);
		Logger.debug("thisMonday = " + thisMonday);
		Logger.debug("previousMonday = " + previousMonday);
		Logger.debug("monCutoff = " + monCutoff);
		Logger.debug("tuesCutoff = " + tuesCutoff);

		// No edits allowed for data before previous week.
		if (jodaSelectedDate.isBefore(previousMonday)) {
			return "invalid.date.previous.week";
		}

		// Previous week data not editable Tuesday after cutoffHour (Monday after
		// cutoffHour on period end).
		if (jodaSelectedDate.isBefore(thisMonday)) {
			if (now.isAfter(tuesCutoff)) {
				return "invalid.date.after.tuesday";
			} else if ((now.isAfter(monCutoff)) && (isPreviousPeriod.equals("T"))) {
				return "invalid.date.after.monday";
			}
		}

		return null;
	}

	public SalesAuditResponse createSuccessResponse(AuditEntry auditEntry, Site site2) {

		SalesAuditResponse salesAuditResponse = new SalesAuditResponse();

		salesAuditResponse.setAllowDataEdits(true);
		// If site selected is franchise, advise.
		if (site.getSiteStatus().equalsIgnoreCase("FS")) {
			salesAuditResponse.setActionMsg("message.site.franchise");
		}
		salesAuditResponse.setSiteNum(auditEntry.getSiteNum());
		salesAuditResponse.setBusinessDat(auditEntry.getBusinessDat());
		salesAuditResponse.setAuditActual(auditEntry.getAuditActual());
		salesAuditResponse.setPolledActual(auditEntry.getPolledActual());
		salesAuditResponse.setRdcActual(auditEntry.getRdcActual());
		return salesAuditResponse;
	}

	public SalesAuditResponse createFailureResponse(String validateResponse) {
		SalesAuditResponse salesAuditResponse = new SalesAuditResponse();

		salesAuditResponse.setAllowDataEdits(false);
		salesAuditResponse.setErrorMsg(validateResponse);
		return salesAuditResponse;
	}

	public SalesAuditResponse createFailureResponseWithAuditEntry(AuditEntry auditEntry, String validateResponse) {
		SalesAuditResponse salesAuditResponse = new SalesAuditResponse();

		salesAuditResponse.setAllowDataEdits(false);
		salesAuditResponse.setErrorMsg(validateResponse);
		salesAuditResponse.setSiteNum(auditEntry.getSiteNum());
		salesAuditResponse.setBusinessDat(auditEntry.getBusinessDat());
		salesAuditResponse.setAuditActual(auditEntry.getAuditActual());
		salesAuditResponse.setPolledActual(auditEntry.getPolledActual());
		salesAuditResponse.setRdcActual(auditEntry.getRdcActual());
		return salesAuditResponse;
	}
	
	 /**
     * validateStringDecimal - field validation method for String decimals. 
     *
     * @param fieldName
     * @param fieldValue
     * @param minimum
     * @param maximum
     */
		public SalesAuditResponse validateStringDecimal(String fieldName, String fieldValue) {
			Logger.debug("validateStringDecimal");
			Logger.debug("fieldName=" + fieldName);
			Logger.debug("fieldValue=" + fieldValue);
			SalesAuditResponse salesAuditResponse = new SalesAuditResponse();
			// Check if a fieldValue was keyed
			if (fieldValue != null && !fieldValue.isEmpty()) {

				// Trim the value
				String fieldVal = fieldValue.trim();

				// Convert to validate and remove leading zeros
				Double val = 0.0;
				try {
					val = Double.parseDouble(fieldVal);
				} catch (NumberFormatException e) {
					salesAuditResponse.setErrorMsg("field.notNumeric");
					Logger.warn("Exception converting keyed " + fieldName + " to Double. fieldVal=" + fieldVal, e);
					return salesAuditResponse;
				}

				// Make sure fieldValue is not negative
				if (val < 0.0) {
					salesAuditResponse.setErrorMsg("field.invalidNegative");
					Logger.warn(fieldName + " is not >= 0 test failed. fieldVal=" + fieldVal);
					return salesAuditResponse;
				}
			} else {
				salesAuditResponse.setErrorMsg("field.notNumeric");
				Logger.debug("Field is null/not numeric");
			}
			return salesAuditResponse;
		}

}
