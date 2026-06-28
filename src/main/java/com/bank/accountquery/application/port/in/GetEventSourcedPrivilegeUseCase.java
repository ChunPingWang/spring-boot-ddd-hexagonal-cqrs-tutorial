package com.bank.accountquery.application.port.in;

import com.bank.accountquery.application.command.privilege.result.EventSourcedPrivilegeState;
import com.bank.accountquery.application.command.privilege.result.PrivilegeEventView;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import java.util.List;

/** Input Port（事件溯源讀取）— 由事件流重建狀態、或檢視原始事件序列。 */
public interface GetEventSourcedPrivilegeUseCase {
    EventSourcedPrivilegeState replayState(PrivilegeId privilegeId);
    List<PrivilegeEventView> eventLog(PrivilegeId privilegeId);
}
