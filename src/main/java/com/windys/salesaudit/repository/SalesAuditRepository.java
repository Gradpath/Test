package com.windys.salesaudit.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SalesAuditRepository {
	
	@Autowired
    private JdbcTemplate jdbcTemplate;
	
	

}
