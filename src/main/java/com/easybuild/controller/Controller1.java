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
@RequestMapping(value = "/home")
public class Controller1 {

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    @ResponseBody
    public JsonMsg hello(){

        JsonMsg jsonMsg=new JsonMsg();
        jsonMsg.setCode("886");
        jsonMsg.setData("顾卓成也太帅了吧,我敲立马)");
        return jsonMsg;
    }
}
