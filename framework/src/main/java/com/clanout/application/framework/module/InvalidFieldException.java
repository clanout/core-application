package com.clanout.application.framework.module;

public class InvalidFieldException extends Exception
{
    private String fieldName;

    public InvalidFieldException(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getFieldName()
    {
        return fieldName;
    }
}
