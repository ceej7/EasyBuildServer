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
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping(value = "/buildup")
public class BuildUpController {
    MongoClient client= RemoteMDBUtil.createMongoDBClient();

    /**
     * 理想装机方案
     * @return
     */
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
            JSONObject jsonObject=JSONObject.fromObject(dbCursor.next().toJson());
            Iterator<String> iterator=jsonObject.keys();
            while(iterator.hasNext())
            {
                String tmp=iterator.next();
                jsonObject.put(tmp,jsonObject.getString(tmp));
            }
            res.add(jsonObject);
        }
        jsonMsg.setCode("true");
        jsonMsg.setData(res);
        return jsonMsg;
    }

    /**
     * depricated
     * @param json
     * @return
     */
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
            JSONObject jsonObject=JSONObject.fromObject(dbCursor.next().toJson());
            Iterator<String> iterator=jsonObject.keys();
            while(iterator.hasNext())
            {
                String tmp=iterator.next();
                jsonObject.put(tmp,jsonObject.getString(tmp));
            }
            res.add(jsonObject);
        }
        jsonMsg.setCode("true");
        jsonMsg.setData(res);
        return jsonMsg;
    }

    /**
     * 加入用户的装机方案
     * @param json
     * @return
     */
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
        JSONObject jsonObject=JSONObject.fromObject(json);
        Document doc=new Document();
        Iterator<String> iterator=jsonObject.keys();
        while (iterator.hasNext())
        {
            String tmp=iterator.next();
            if(!tmp.equals("userID"))
            {
                doc.put(tmp,new ObjectId(jsonObject.getString(tmp)));
            }
            else{
                doc.put(tmp,jsonObject.get(tmp));
            }
        }

        collection.insertOne(doc);
        jsonMsg.setCode("true");
        return jsonMsg;
    }

    /**
     * 根据userID
     * 得到用户所有的装机方案
     * @param json
     * @return
     */
    @RequestMapping(value = "/ideal2")
    @ResponseBody
    public JsonMsg searchIdeal2(@RequestBody String json)
    {
        JsonMsg jsonMsg = new JsonMsg();
        jsonMsg.setCode("false");

        String user="admin@qq.com";
        if(json!=null)
        {
            JSONObject object = JSONObject.fromObject(json);
            if (object.has("userID") ) {
                user=object.getString("userID");
            }
        }
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("plan");
        BasicDBObject queryCondition = new BasicDBObject();//创建查询条件
        JSONArray res=new JSONArray();
        queryCondition.put("userID", user);
        MongoCursor<Document> dbCursor = collection.find(queryCondition).iterator();
        while(dbCursor.hasNext())
        {
            JSONObject jsonObject=JSONObject.fromObject(dbCursor.next().toJson());
            Iterator<String> iterator=jsonObject.keys();
            while(iterator.hasNext())
            {
                    String tmp=iterator.next();
                if(!tmp.equals("_id")&&!tmp.equals("userID"))
                {
                    JSONObject hardwareJson=new JSONObject();
                    hardwareJson.put("favorite_id",jsonObject.getJSONObject(tmp).getString("$oid"));
                    hardwareJson.put("type",tmp);
                    JsonMsg jmsg=searchID(hardwareJson.toString());
                    jsonObject.put(tmp,jmsg.getData());
                }

                else{
                    jsonObject.put(tmp,jsonObject.getString(tmp));
                }
            }
            res.add(jsonObject);
        }
        jsonMsg.setCode("true");
        jsonMsg.setData(res);
        return jsonMsg;
    }

    /**
     * 通过itemID和favorite_id
     * 拿出冗余处理后的完整信息
     * @param json
     * @return
     */
    public JsonMsg searchID(@RequestBody String json) {
        //初始化返回值
        JsonMsg jsonMsg = new JsonMsg();
        jsonMsg.setCode("false");
        //解析传入http请求body中的json
        JSONObject object = JSONObject.fromObject(json);
        if (!object.has("type") || !object.has("favorite_id")) {
            return jsonMsg;
        }
        //得到type键的值
        String type = object.getString("type");
        ObjectId myid = new ObjectId(object.getString("favorite_id"));
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection(type);

        BasicDBObject queryCondition = new BasicDBObject();//创建查询条件
        queryCondition.put("_id", myid);

        MongoCursor<Document> dbCursor = collection.find(queryCondition).iterator();//开始查询
        if (dbCursor.hasNext()) {//遍历查询子结构
            JSONObject item = JSONObject.fromObject(dbCursor.next().toJson());
            if (type.equals("items")) {
                //添加冗余string123
                Iterator<String>  its=item.keys();
                while (its.hasNext())
                {
                    String tmp=its.next();
                    if(!tmp.equals("_id")&&!tmp.equals("title")&&!tmp.equals("img")&&!tmp.equals("prices")&&!tmp.equals("comments")&&!tmp.equals("itemID"))
                    {
                        JSONObject _item=item.getJSONObject(tmp);
                        if(_item.has("主体"))
                        {
                            JSONObject body=_item.getJSONObject("主体");
                            Iterator<String> ite=body.keys();
                            int cnt=1;
                            while(ite.hasNext())
                            {
                                String _tmp=ite.next();
                                if(!_tmp.equals("其他"))
                                {
                                    _item.put("string"+cnt,body.getString(_tmp));
                                    cnt++;
                                }
                            }
                            for (; cnt <=3 ; cnt++)
                            {
                                _item.put("string"+cnt,"未知");
                            }
                        }
                        else{
                            int cnt=1;
                            for (; cnt <=3 ; cnt++)
                            {
                                _item.put("string"+cnt,"未知");
                            }
                        }
                        item.put(tmp,_item);
                    }
                }
                //维护price
                Iterator<String> tmpkeys = item.getJSONObject("prices").keys();
                String pricekey = "";
                while (tmpkeys.hasNext()) {
                    pricekey = tmpkeys.next();
                }
                Double price = item.getJSONObject("prices").getDouble(pricekey);
                item.put("price", price);
                item.put("prices", " " + item.getJSONObject("prices").toString());
                //维护图片
                String img = item.getString("img");
                item.put("img", "http:" + img);
            }
            //是硬件则加上图片
            else {
                String hardwareimg = "";
                List<Object> ids = item.getJSONArray("itemIDs");
                if (ids.size() != 0) {
                    hardwareimg = hardwareimg + getImg(ids.get(0).toString());
                }
                if (hardwareimg.equals("")) {
                    hardwareimg = RemoteMDBUtil.Host + "imgs/" + type + ".png";
                }
                item.put("img", hardwareimg);
                //增加其他硬件的三个属性
                if(!type.equals("cpu")&&!type.equals("gpu"))
                {
                    if(item.has("主体"))
                    {
                        JSONObject body=item.getJSONObject("主体");
                        Iterator<String> ite=body.keys();
                        int cnt=1;
                        while(ite.hasNext())
                        {
                            String tmp=ite.next();
                            if(!tmp.equals("其他"))
                            {
                                item.put("string"+cnt,body.getString(tmp));
                                cnt++;
                            }
                        }
                        for (; cnt <=3 ; cnt++)
                        {
                            item.put("string"+cnt,"");
                        }
                    }
                }
            }
            jsonMsg.setCode(type);
            jsonMsg.setData(item);
        } else {
            jsonMsg.setCode("false");
        }
        return jsonMsg;
    }

    /**
     * 通过itemID得到图片URL
     */
    String getImg(String itemID) {
        //得到type键的值
        String type = "items";
        String myid = itemID;
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection(type);
        BasicDBObject queryCondition = new BasicDBObject();//创建查询条件
        queryCondition.put("itemID", myid);
        MongoCursor<Document> dbCursor = collection.find(queryCondition).iterator();//开始查询
        if (dbCursor.hasNext()) {//遍历查询子结构
            JSONObject item = JSONObject.fromObject(dbCursor.next().toJson());
            return "http:" + item.getString("img");
        } else {
            return "";
        }
    }

}
