package com.nicehcy2.entity;

import lombok.Getter;

@Getter
public enum JobGroup {

    STUDENT("학생"),
    EMPLOYEE("직장인"),
    HOMEMAKER("주부"),
    SELF_EMPLOYED("자영업자"),
    UNDECIDED("미선택");

    private final String jobGroup;

    JobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }
}
