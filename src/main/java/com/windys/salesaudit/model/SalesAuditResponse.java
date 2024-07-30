package com.windys.salesaudit.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SalesAuditResponse extends AuditEntry{
	
	private boolean allowDataEdits;
	private String errorMsg;
	private String actionMsg;
	
	
	public boolean isAllowDataEdits() {
		return allowDataEdits;
	}
	public void setAllowDataEdits(boolean allowDataEdits) {
		this.allowDataEdits = allowDataEdits;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public String getActionMsg() {
		return actionMsg;
	}
	public void setActionMsg(String actionMsg) {
		this.actionMsg = actionMsg;
	}
	
}
