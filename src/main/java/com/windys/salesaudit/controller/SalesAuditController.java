package com.windys.salesaudit.controller;

import java.io.Serializable;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.windys.salesaudit.model.AuditEntry;
import com.windys.salesaudit.model.DateControl;
import com.windys.salesaudit.service.SalesAuditService;
import com.windys.salesaudit.utility.SalesAuditUtility;

@RestController
@RequestMapping("/salesAudit")
public class SalesAuditController implements Serializable{

	private static final long serialVersionUID = -5012788994941694997L;
	private static final Logger Logger = LoggerFactory.getLogger(SalesAuditController.class);
	private Boolean   allowDataEdits     = false;
	
	@Autowired
	SalesAuditUtility salesAuditUtility;
	
	@Autowired
	AuditEntry auditEntry;
	
	@Autowired
	SalesAuditService salesAuditService;
	
	@GetMapping("/load")
    public ResponseEntity<AuditEntry> loadSalesAudit(@RequestParam(name = "selectedRestaurant") String selectedRestaurant,
                                            @RequestParam(name = "selectedDate") String selectedDate) throws ParseException {
		Logger.debug( "SalesAuditController:loadSalesAudit()");
        Logger.debug( "selectedRestaurant = " + selectedRestaurant);
        Logger.debug( "selectedDate = " + selectedDate);
	    
	    allowDataEdits = false;
	    
	    String validateResponse = null;
	    
	    validateResponse = salesAuditUtility.validateSalesAuditData(selectedDate,selectedRestaurant);
	    if(validateResponse.isEmpty()) {
	    	auditEntry = salesAuditService.getAuditEntry(selectedRestaurant, new DateControl(selectedDate));
	    	Logger.debug( "auditEntry.getAuditActual().getDbRowExists = " + auditEntry.getAuditActual().isDbRowExists());
	    }
		
		return null;
    }
	
	

}
