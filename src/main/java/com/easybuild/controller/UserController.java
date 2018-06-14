package com.easybuild.controller;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import model.JsonMsg;
import model.RemoteMDBUtil;
import net.sf.json.JSONObject;
import org.bson.Document;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/user")
public class UserController {

    @RequestMapping(value = "/register")
    @ResponseBody
    public JsonMsg register(JSONObject object){
        JsonMsg jsonMsg=new JsonMsg();
        jsonMsg.setCode("false");
        String userID=object.getString("userID");
        String password=object.getString("password");
        String nickname=object.getString("nickname");
        MongoClient client=RemoteMDBUtil.createMongoDBClient();;
        try{
            MongoDatabase database=client.getDatabase(RemoteMDBUtil.DB);
            MongoCollection collection=database.getCollection("user");
            MongoCursor<Document> docs=collection.find(new BasicDBObject("userID",userID) ).iterator();
            if(docs.hasNext())
            {
                jsonMsg.setCode("false");
            }
            else{
                Document doc=new Document();
                doc.append("userID",userID);
                doc.append("password",password);
                doc.append("nickname",nickname);
                collection.insertOne(doc);
                jsonMsg.setCode("true");
            }
        }
       finally {
            client.close();
            return jsonMsg;
        }
    }

    @RequestMapping(value = "/login")
    @ResponseBody
    public JsonMsg login(String userID,String password){
        JsonMsg jsonMsg=new JsonMsg();
        jsonMsg.setCode("false");
        MongoClient client = RemoteMDBUtil.createMongoDBClient();
        try {
            MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
            MongoCollection collection = database.getCollection("user");
            BasicDBObject queryCondition = new BasicDBObject();
            queryCondition.put("userID",userID );
            queryCondition.put("password", password);
            MongoCursor<Document> dbCursor = collection.find(queryCondition).iterator();
            if (dbCursor.hasNext()) {
                jsonMsg.setCode("true");
                Document doc=dbCursor.next();
                doc.append("profile_photo",RemoteMDBUtil.Host+"imgs/"+doc.getString("profile_photo"));
                jsonMsg.setData(doc);
            } else {
                jsonMsg.setCode("false");
            }
        }
        finally {
            client.close();
            return jsonMsg;
        }
    }

}
