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
        jsonMsg.setCode("205");
        jsonMsg.setData(0);
        return jsonMsg;
    }
    @RequestMapping(value = "/hello1", method = RequestMethod.GET)
    @ResponseBody
    public JsonMsg hello1(int i){
        MongoClient client= RemoteMDBUtil.createMongoDBClient();
        try {
            // 取得Collecton句柄
            MongoDatabase database = client.getDatabase(RemoteMDBUtil.DEMO_DB);
            MongoCollection<Document> collection = database.getCollection(RemoteMDBUtil.DEMO_COLL);
            // 读取数据
            MongoCursor<Document> cursor = collection.find().iterator();
            while (cursor.hasNext()) {
                JsonMsg jsonMsg=new JsonMsg();
                jsonMsg.setCode("205");
                jsonMsg.setData(cursor.next());
                return jsonMsg;
            }
        } finally {
            //关闭Client，释放资源
            client.close();
        }
        JsonMsg jsonMsg=new JsonMsg();
        jsonMsg.setCode("205");
        jsonMsg.setData(0);
        return jsonMsg;
    }
}
