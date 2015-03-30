package com.alelk.pws.xmlengine.exception;

import com.alelk.pws.xmlengine.PwsXmlEngineException;

/**
 * Created by alelkin on 25.03.2015.
 */
public class PwsXmlParserIncorrectSourceFormatException extends PwsXmlEngineException {
    public PwsXmlParserIncorrectSourceFormatException() {}
    public PwsXmlParserIncorrectSourceFormatException(String message){
        super(message);
    }
}
