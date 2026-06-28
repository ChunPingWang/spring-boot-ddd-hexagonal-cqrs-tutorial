package com.bank.accountquery.application.query.privilege;

import com.bank.accountquery.domain.model.shared.CustomerId;

public record GetTransferPrivilegeQuery(
    CustomerId customerId
) {}
