package com.windys.salesaudit.utility;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.windys.salesaudit.model.DateControl;
import com.windys.salesaudit.model.FiscalData;
import com.windys.salesaudit.model.Site;
import com.windys.salesaudit.service.SalesAuditService;

public class SalesAuditUtility {

	@Autowired
	DateControl dateControl;

	@Autowired
	FiscalData fiscalData;

	@Autowired
	SalesAuditService salesAuditService;

	@Autowired
	Site site;
	
	private int       cutoffHour         = 19;

	private static final Logger Logger = LoggerFactory.getLogger(SalesAuditUtility.class);

	public String validateSalesAuditData(String selectedDate, String selectedRestaurant) throws ParseException {
		if (!selectedRestaurant.isEmpty() && !selectedDate.isEmpty()) {
			// Setup the DateControl object using milliseconds from the date selected.
			dateControl = new DateControl(selectedDate);
			Logger.debug("dateControl=" + dateControl);

			// Get the Fiscal year and period of the current and requested dates.
			DateControl currentDc = new DateControl(new Date().getTime());
			FiscalData currentFiscalData = salesAuditService.getFiscalCalInfo(currentDc.getBusinessDat());
			FiscalData selectedFiscalData = salesAuditService.getFiscalCalInfo(dateControl.getBusinessDat());
			boolean isPreviousPeriod = false;

			// Determine the relationship between today's fiscal period and the selected
			// period.
			if (currentFiscalData.getYearNum() == selectedFiscalData.getYearNum()) {
				if ((currentFiscalData.getPeriodNum() - selectedFiscalData.getPeriodNum()) == 1) {
					isPreviousPeriod = true;
				}
			} else if ((currentFiscalData.getPeriodNum() - selectedFiscalData.getPeriodNum()) == 1) {
				if ((currentFiscalData.getPeriodNum() == 1) && (selectedFiscalData.getPeriodNum() == 12)) {
					// Current period is 1 and selected period is 12 of previous year.
					isPreviousPeriod = true;
				}
			}

			Logger.debug("currentFiscalData = " + currentFiscalData);
			Logger.debug("selectedFiscalData = " + selectedFiscalData);
			Logger.debug("isPreviousPeriod = " + isPreviousPeriod);

			// If selected date is in the future return error.
			if (currentDc.getBusinessDat().compareTo(dateControl.getBusinessDat()) < 0) {
				return "invalid.date.future";
			}

			site = salesAuditService.getSiteInfo(selectedRestaurant);

			// If no site data found return error.
			if (!site.isDbRowExists()) {
				return "invalid.site.noData";
			}

			// If site is International site return error.
			if (site.getSiteStatus().equalsIgnoreCase("IS")) {
				return "invalid.site.international";
			}

			// Calculate various dates and times.
			LocalDate todaysDate = LocalDate.now();
			LocalDateTime now = LocalDateTime.now();

			LocalDateTime tuesCutoff = todaysDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
			                                     .atTime(LocalTime.of(cutoffHour, 0));

			LocalDateTime monCutoff = todaysDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
			                                    .atTime(LocalTime.of(cutoffHour, 0));

			LocalDate thisMonday = todaysDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

			LocalDate previousMonday = todaysDate.minusWeeks(1)
			                                     .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			LocalDate jodaSelectedDate = dateControl.getBusinessDat().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

			Logger.debug("todaysDate = " + todaysDate);
			Logger.debug("today is " + todaysDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));
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
				} else if ((now.isAfter(monCutoff)) && (isPreviousPeriod)) {
					return "invalid.date.after.monday";
				}
			}

		}
		return null;
	}

}
