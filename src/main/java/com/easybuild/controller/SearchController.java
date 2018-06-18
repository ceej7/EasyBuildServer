package com.easybuild.controller;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import model.JsonMsg;
import model.RemoteMDBUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping(value = "/search")
public class SearchController {
    MongoClient client;
    /**
     * 统一化的search解析一底层
     * 解析type，进行服务映射
     * @param json
     * @return
     */
    @RequestMapping(value = "/easySearch")
    @ResponseBody
    public JsonMsg search(@RequestBody String json) {
        //初始化返回值
        JsonMsg jsonMsg=new JsonMsg();
        jsonMsg.setCode("false");
        //解析传入http请求body中的json
        JSONObject object=JSONObject.fromObject(json);
        //得到type键的值
        String type=object.getString("type");
        client= RemoteMDBUtil.createMongoDBClient();
        if(type.equals("items"))
        {
            jsonMsg=queryItems(object);
            jsonMsg.setCode("items");
        }
        else if(type.equals("cpu"))
        {
            jsonMsg=queryCPU(object);
            jsonMsg.setCode("cpu");
        }
        else if(type.equals("gpu"))
        {
            jsonMsg=queryGPU(object);
            jsonMsg.setCode("gpu");
        }
        else
        {

        }

        client.close();
        return jsonMsg;
    }

    @RequestMapping(value = "/idSearch")
    @ResponseBody
    public JsonMsg searchID(@RequestBody String json)
    {
        //初始化返回值
        JsonMsg jsonMsg=new JsonMsg();
        jsonMsg.setCode("false");
        //解析传入http请求body中的json
        JSONObject object=JSONObject.fromObject(json);
        if(!object.has("type")||!object.has("favorite_id"))
        {
            return jsonMsg;
        }
        //得到type键的值
        String type=object.getString("type");
        ObjectId myid=new ObjectId(object.getString("favorite_id"));
        MongoClient client = RemoteMDBUtil.createMongoDBClient();
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection(type);
        BasicDBObject queryCondition = new BasicDBObject();
        queryCondition.put("_id", myid);
        MongoCursor<Document> dbCursor = collection.find(queryCondition).iterator();
        if (dbCursor.hasNext()) {
            JSONObject item=JSONObject.fromObject(dbCursor.next().toJson());
            if(type.equals("items"))
             {
                Iterator<String > tmpkeys=item.getJSONObject("prices").keys();
                String pricekey="";
                while(tmpkeys.hasNext())
                {
                    pricekey=tmpkeys.next();
                }
                Double price=item.getJSONObject("prices").getDouble(pricekey);
                item.put("price",price);
                 String img=item.getString("img");
                 item.put("img","http:"+img);
            }
            jsonMsg.setCode(type);
            jsonMsg.setData(item);
        }
        else{
            jsonMsg.setCode("false");
        }
        return jsonMsg;
    }

    /**
     * 根据Json中参数查找商品
     * @param object
     * @return
     */
    public JsonMsg queryItems(JSONObject object)
    {
        Long d0=new Date().getTime();
        //1.include="0000000000"
        //2. key
        //3.low high
        //4.order
        //查询初始化
        JsonMsg jsonMsg=new JsonMsg();
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("items");
        BasicDBObject queryCondition = new BasicDBObject();
        ArrayList<JSONObject> ary=new ArrayList<JSONObject>();
        BasicDBList values=new BasicDBList();
        ArrayList<JSONObject> result=new  ArrayList<JSONObject>();

        //栏目筛选
        String include=object.getString("include");
        if(include.charAt(0)=='1')
        {
            values.add(new BasicDBObject("cpu",new BasicDBObject("$type",3)));
        }
        if(include.charAt(1)=='1')
        {
            values.add(new BasicDBObject("gpu",new BasicDBObject("$type",3)));
        }
        if(include.charAt(2)=='1')
        {
            values.add(new BasicDBObject("case",new BasicDBObject("$type",3)));
        }
        if(include.charAt(3)=='1')
        {
            values.add(new BasicDBObject("power",new BasicDBObject("$type",3)));
        }
        if(include.charAt(4)=='1')
        {
            values.add(new BasicDBObject("cooler_water",new BasicDBObject("$type",3)));
        }
        if(include.charAt(5)=='1')
        {
            values.add(new BasicDBObject("cooler_wind",new BasicDBObject("$type",3)));
        }
        if(include.charAt(6)=='1')
        {
            values.add(new BasicDBObject("hhd",new BasicDBObject("$type",3)));
        }
        if(include.charAt(7)=='1')
        {
            values.add(new BasicDBObject("ssd",new BasicDBObject("$type",3)));
        }
        if(include.charAt(8)=='1')
        {
            values.add(new BasicDBObject("memory",new BasicDBObject("$type",3)));
        }
        if(include.charAt(9)=='1')
        {
            values.add(new BasicDBObject("motherboard",new BasicDBObject("$type",3)));
        }
        queryCondition.put("$or",values);

        //确定low和high
        Double low=-100d;
        Double high=10000000d;
        if(object.has("low"))
        {
            if(!StringUtils.replace(object.getString("low")," ","").equals(""))
                low=object.getDouble("low");

        }
        if(object.has("high"))
        {
            if(!StringUtils.replace(object.getString("high")," ","").equals(""))
                 high=object.getDouble("high");
        }
        //确定key
        String key=object.getString("key");
        List<String> keys_raw= Arrays.asList(StringUtils.split(key," "));
        ArrayList<String> keys=new ArrayList<String>();
        for (int i = 0; i < keys_raw.size(); i++) {
            keys.add(  StringUtils.replace(keys_raw.get(i)," ","").toLowerCase()  );//小写&去空格
        }
        BasicDBObject proj=new BasicDBObject();
        proj.put("title",1);
        proj.put("img",1);
        proj.put("prices",1);
        proj.put("itemID",1);
        proj.put("comments",1);
        //根据key和price筛选
        BasicDBList values_and=new BasicDBList();
        int i=0;
        for (; i < keys.size(); i++) {
            BasicDBObject _cond1=new BasicDBObject();
            BasicDBObject _cond2=new BasicDBObject();
            _cond2.put("$regex",keys.get(i));
            _cond2.put("$options","i");
            _cond1.put("title",_cond2);
            values_and.add(_cond1);
        }
        if(keys.size()!=0)
        {
            queryCondition.put("$and",values_and);
        }
        MongoCursor<Document> dbCursor = collection.find(queryCondition).projection(proj).iterator();
        while(dbCursor.hasNext())
        {
            JSONObject item=JSONObject.fromObject(dbCursor.next().toJson());
            boolean hasKey=true;
            if(hasKey)
            {
                Iterator<String > tmpkeys=item.getJSONObject("prices").keys();
                String pricekey="";
                while(tmpkeys.hasNext())
                {
                    pricekey=tmpkeys.next();
                }
                Double price=item.getJSONObject("prices").getDouble(pricekey);
                if (price<=high&&price>=low)
                {
                    item.put("price",price);
                    item.put("img","http:"+item.getString("img"));
                    result.add(item);
                }
            }
        }
        Long d1=new Date().getTime();
        //排序
        String seq=object.getString("order");
        if(seq.equals("comprehensive"))
        {
            Collections.sort(result, new SortByCom());
        }
        else if (seq.equals("cost_performance"))
        {
            Collections.sort(result, new SortByPer());
        }
        else if (seq.equals("price_des"))
        {
            Collections.sort(result, new SortByPriceDes());
        }
        else if (seq.equals("price_asc"))
        {
            Collections.sort(result, new SortByPriceAsc());
        }
        Long d2=new Date().getTime();
        jsonMsg.setCode("true");
        jsonMsg.setData(JSONArray.fromObject(result));
        return jsonMsg;
    }
    public JsonMsg queryCPU(JSONObject object)//注意熟悉的大小写
    {
        JsonMsg jsonMsg=new JsonMsg();
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("cpu");
        ArrayList<JSONObject> ary=new ArrayList<JSONObject>();
        BasicDBObject values=new BasicDBObject();
        ArrayList<JSONObject> result=new  ArrayList<JSONObject>();
        if(object.has("Name"))
        {
            queryKeyString(object,"Name",values);
        }
        if(object.has("Codename"))
        {
            queryKeyString(object,"Codename",values);
        }
        if(object.has("Cores"))
        {
            queryKeyInt(object,"Cores",values);
        }
        if(object.has("Threads"))
        {
            queryKeyInt(object,"Threads",values);
        }
        if(object.has("Socket"))
        {
            queryKeyString(object,"Socket",values);
        }
        if(object.has("Process"))
        {
            queryKeyInt(object,"Process",values);
        }
        if(object.has("CacheL1"))
        {
            queryKeyInt(object,"CacheL1",values);
        }
        if(object.has("TDP"))
        {
            queryKeyInt(object,"TDP",values);
        }
        MongoCursor<Document> dbCursor = collection.find(values).iterator();
        while(dbCursor.hasNext())
        {
            JSONObject item=JSONObject.fromObject(dbCursor.next().toJson());
            result.add(item);
        }
        Collections.sort(result, new SortByDate());
        jsonMsg.setCode("true");
        jsonMsg.setData(JSONArray.fromObject(result));
        return jsonMsg;
    }
    public JsonMsg queryGPU(JSONObject object)
    {
        JsonMsg jsonMsg=new JsonMsg();
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("gpu");
        ArrayList<JSONObject> ary=new ArrayList<JSONObject>();
        BasicDBObject values=new BasicDBObject();
        ArrayList<JSONObject> result=new  ArrayList<JSONObject>();
        if(object.has("Name"))
        {
            queryKeyString(object,"Name",values);
        }
        if(object.has("Chip"))
        {
            queryKeyString(object,"Chip",values);
        }
        if(object.has("Bus"))
        {
            queryKeyString(object,"Bus",values);
        }
        if(object.has("Memory_Size"))
        {
            queryKeyInt(object,"Memory_Size",values);
        }
        if(object.has("Memory_Type"))
        {
            queryKeyString(object,"Memory_Type",values);
        }
        if(object.has("Memory_Bus"))
        {
            queryKeyInt(object,"Memory_Bus",values);
        }
        if(object.has("GPU_Clock"))
        {
            queryKeyInt(object,"GPU_Clock",values);
        }
        MongoCursor<Document> dbCursor = collection.find(values).iterator();
        while(dbCursor.hasNext())
        {
            JSONObject item=JSONObject.fromObject(dbCursor.next().toJson());
            result.add(item);
        }
        Collections.sort(result, new SortByDate());
        jsonMsg.setCode("true");
        jsonMsg.setData(JSONArray.fromObject(result));
        return jsonMsg;
    }
    public void queryKeyString(JSONObject object,String name,BasicDBObject values)
    {
        BasicDBObject query1=new BasicDBObject();
        query1.put("$regex",object.getString(name));
        query1.put("$options","i");
        values.put(name,query1);
    }
    public void queryKeyInt(JSONObject object,String name,BasicDBObject values)
    {
        BasicDBObject query1=new BasicDBObject();
//        if(object.getJSONObject(name).has("low"))
//        {
//            query1.put("$gte",object.getJSONObject(name).getInt("low"));
//        }
//        if(object.getJSONObject(name).has("high"))
//        {
//            query1.put("$lte",object.getJSONObject(name).getInt("high"));
//        }
        values.put(name,object.getInt(name));
    }

    /**
     * sort时用到的方法
     */
    class SortByCom implements Comparator {

        public int compare(Object o1, Object o2) {
            JSONObject j1=(JSONObject)o1;
            JSONObject j2=(JSONObject)o2;
            if(j1.getInt("comments")<j2.getInt("comments"))
                return 1;
            return -1;
        }
    }
    class SortByPer implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1=(JSONObject)o1;
            JSONObject j2=(JSONObject)o2;
            if(j1.getDouble("comments")/j1.getDouble("price")<j2.getDouble("comments")/j2.getDouble("price"))
                return 1;
            return -1;
        }
    }
    class SortByPriceDes implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1=(JSONObject)o1;
            JSONObject j2=(JSONObject)o2;
            if(j1.getDouble("price")<j2.getDouble("price"))
                return 1;
            return -1;
        }
    }
    class SortByPriceAsc implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1=(JSONObject)o1;
            JSONObject j2=(JSONObject)o2;
            if(j1.getDouble("price")>j2.getDouble("price"))
                return 1;
            return -1;
        }
    }
    class SortByDate implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1=(JSONObject)o1;
            JSONObject j2=(JSONObject)o2;
            if(!j1.has("Released"))
            {
                j1.put("Released","1990-01-01");
            }
            if(!j2.has("Released"))
            {
                j2.put("Released","1990-01-01");
            }
            return j2.getString("Released").compareTo(j1.getString("Released"));
        }
    }
}
