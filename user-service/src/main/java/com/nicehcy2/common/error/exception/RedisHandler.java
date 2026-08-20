package com.nicehcy2.common.error.exception;

import com.nicehcy2.common.error.GeneralException;
import com.nicehcy2.common.error.ResponseCode;

public class RedisHandler extends GeneralException {

    public RedisHandler(ResponseCode errorCode) { super(errorCode); }
}
