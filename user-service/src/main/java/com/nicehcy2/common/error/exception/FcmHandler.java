package com.nicehcy2.common.error.exception;

import com.nicehcy2.common.error.GeneralException;
import com.nicehcy2.common.error.ResponseCode;

public class FcmHandler extends GeneralException {

    public FcmHandler(ResponseCode errorCode) { super(errorCode); }
}
