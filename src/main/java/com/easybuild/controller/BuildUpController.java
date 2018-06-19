package com.easybuild.controller;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import model.JsonMsg;
import model.RemoteMDBUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bson.Document;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;

@Controller
@RequestMapping(value = "/buildup")
public class BuildUpController {
    MongoClient client= RemoteMDBUtil.createMongoDBClient();

    @RequestMapping(value = "/ideal")
    @ResponseBody
    public JsonMsg searchIdeal()
    {
        JsonMsg jsonMsg = new JsonMsg();
        jsonMsg.setCode("false");
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("plan");
        BasicDBObject queryCondition = new BasicDBObject();//创建查询条件
        JSONArray res=new JSONArray();
        queryCondition.put("userID", "admin@qq.com");
        MongoCursor<Document> dbCursor = collection.find(queryCondition).iterator();
        while(dbCursor.hasNext())
        {
            res.add(dbCursor.next());
        }
        jsonMsg.setCode("true");
        jsonMsg.setData(res);
        return jsonMsg;
    }

    @RequestMapping(value = "/user")
    @ResponseBody
    public JsonMsg searchUser(@RequestBody String json)
    {
        JsonMsg jsonMsg = new JsonMsg();
        jsonMsg.setCode("false");
        JSONObject object = JSONObject.fromObject(json);
        if (!object.has("userID") ) {
            return jsonMsg;
        }
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("plan");
        BasicDBObject queryCondition = new BasicDBObject();//创建查询条件
        JSONArray res=new JSONArray();
        queryCondition.put("userID",object.getString("userID"));
        MongoCursor<Document> dbCursor = collection.find(queryCondition).iterator();
        while(dbCursor.hasNext())
        {
            res.add(dbCursor.next());
        }
        jsonMsg.setCode("true");
        jsonMsg.setData(res);
        return jsonMsg;
    }

    @RequestMapping(value = "/add")
    @ResponseBody
    public JsonMsg appendUser(@RequestBody String json)
    {
        JsonMsg jsonMsg = new JsonMsg();
        jsonMsg.setCode("false");
        JSONObject object = JSONObject.fromObject(json);
        if (!object.has("userID") ) {
            return jsonMsg;
        }
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("plan");
        collection.insertOne(json);
        jsonMsg.setCode("true");
        return jsonMsg;
    }

}
