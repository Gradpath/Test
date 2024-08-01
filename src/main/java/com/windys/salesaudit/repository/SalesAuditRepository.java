package com.windys.salesaudit.repository;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.windys.salesaudit.model.AuditEntry;
import com.windys.salesaudit.model.DateControl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Repository
public class SalesAuditRepository {
	
	@Autowired
    private JdbcTemplate jdbcTemplate;

	public void insertAuditEntry(AuditEntry auditEntry, String userId, String coCode) {
		// TODO Auto-generated method stub
		
	}

	public void updateSSAXCPEntry(AuditEntry auditEntry, String userId, String coCode) {
		// TODO Auto-generated method stub
		
	}

	public void updateAuditEntry(AuditEntry auditEntry, String userId) {
		// TODO Auto-generated method stub
		
	}

	public void deleteAuditEntry(@Valid @NotBlank String selectedRestaurant, DateControl dateControl,
			boolean dbRowExists) {
		// TODO Auto-generated method stub
		
	}

	public void insertAuditDelete(@Valid @NotBlank String selectedRestaurant, Date busDate) {
		// TODO Auto-generated method stub
		
	}

	public void insertPolledDelete(@Valid @NotBlank String selectedRestaurant, Date busDate) {
		// TODO Auto-generated method stub
		
	}

	public void deletePolledEntry(@Valid @NotBlank String selectedRestaurant, Date busDate) {
		// TODO Auto-generated method stub
		
	}

	public void deleteAuditEntry(@Valid @NotBlank String selectedRestaurant, Date busDate) {
		// TODO Auto-generated method stub
		
	}
	
	

}
