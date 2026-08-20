package com.nicehcy2.common.error.exception;

import com.nicehcy2.common.error.GeneralException;
import com.nicehcy2.common.error.ResponseCode;

public class JwtHandler extends GeneralException {

    public JwtHandler(ResponseCode errorCode) { super(errorCode); }
}
