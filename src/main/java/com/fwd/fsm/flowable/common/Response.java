package com.fwd.fsm.flowable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String code ;

    private String msg;

    private transient Object data;

    public static Response success() {
        return new Response("0", "success", null);
    }

    public static Response success(String code, String msg, Object data) {
        return new Response(code, msg, data);
    }

    public static Response success(String msg, Object data) {
        return new Response("0", msg, data);
    }

    public static Response success(Object data) {
        return new Response("0", "success", data);
    }

    public static Response fail() {
        return new Response("1", "fail", null);
    }

    public static Response fail(String code, String msg, Object data) {
        return new Response(code, msg, data);
    }

    public static Response fail(String msg, Object data) {
        return new Response("1", msg, data);
    }

    public static Response fail(Object data) {
        return new Response("1", "fail", data);
    }

}
