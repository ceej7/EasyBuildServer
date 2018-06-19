package com.easybuild.controller;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import model.JsonMsg;
import model.RemoteMDBUtil;
import org.bson.Document;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.ArrayList;


@Controller
@RequestMapping(value = "/mytest")
public class ControllerDemo {

    /**
     * 测试用方法
     * @return
     */
    @RequestMapping(value = "/test1")
    @ResponseBody
    public JsonMsg hello(){
        JsonMsg jsonMsg=new JsonMsg();
        jsonMsg.setCode("249");
        jsonMsg.setData("这个测试，data是一个object");
        return jsonMsg;
    }
}
