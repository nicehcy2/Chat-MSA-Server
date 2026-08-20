package com.nicehcy2.common.error.exception;

import com.nicehcy2.common.error.GeneralException;
import com.nicehcy2.common.error.ResponseCode;

public class UserHandler extends GeneralException {

    public UserHandler(ResponseCode errorCode) { super(errorCode); }
}
