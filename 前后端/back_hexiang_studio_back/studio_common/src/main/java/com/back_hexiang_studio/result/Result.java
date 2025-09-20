package com.back_hexiang_studio.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    //分别返回状态码，信息，数据
    private Integer code;
    private String msg;
    private T data;

    //返回为任意数据，接受任意数据,Result<T> 意味着返回的是一个通用的结果封装类。<>中的T表示泛型，可以接受任意类型的数据。
    public static  <T>  Result<T>  success() {
        Result<T> result=new Result<T>();
        result.setCode(200);
        return  result;
    }
    public static  <T>  Result<T>  success(T object) {
        Result<T> result=new Result<T>();
        result.data=object;
        result.setCode(200);
        return  result;
    }

    public static  <T>  Result<T>  error(int code ,String msg) {
        Result<T> result=new Result<T>();
        result.setMsg(msg);
        result.setCode(code);
        return  result;
    }
    public static  <T>  Result<T>  error(String msg) {
        Result<T> result=new Result<T>();
        result.setMsg(msg);
        result.setCode(500);
        return  result;
    }
    // 添加 isSuccess 方法
    public boolean isSuccess() {
        return code != null && code == 200;
    }








}
