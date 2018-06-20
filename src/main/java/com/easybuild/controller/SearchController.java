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

import java.lang.reflect.Array;
import java.util.*;

@Controller
@RequestMapping(value = "/search")
public class SearchController {
    MongoClient client=RemoteMDBUtil.createMongoDBClient();
    //分页条目
    static int limit=100;

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
        JsonMsg jsonMsg = new JsonMsg();
        jsonMsg.setCode("false");
        //解析传入http请求body中的json
        JSONObject object = JSONObject.fromObject(json);
        //得到type键的值
        String type = object.getString("type");

        if (type.equals("items"))//解析规范的http请求
        {
            jsonMsg = queryItems(object);
            jsonMsg.setCode("items");
        } else if (type.equals("cpu")) {
            jsonMsg = queryCPU(object);
            jsonMsg.setCode("cpu");
        } else if (type.equals("gpu")) {
            jsonMsg = queryGPU(object);
            jsonMsg.setCode("gpu");
        } else if (type.equals("case") || type.equals("cooler_water") || type.equals("cooler_wind") || type.equals("hdd") || type.equals("memory") || type.equals("motherboard") || type.equals("power") || type.equals("ssd")) {
            jsonMsg = queryHemiStructuredHardware(object);
            jsonMsg.setCode(type);
        }
        return jsonMsg;
    }

    /**
     * 根据数据库id来搜索item
     * @param json
     * @return
     */
    @RequestMapping(value = "/idSearch")
    @ResponseBody
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
     * 根据itemID来搜索item
     * @param json
     * @return
     */
    @RequestMapping(value = "/itemIDSearch")
    @ResponseBody
    public JsonMsg searchitemID(@RequestBody String json) {
        //初始化返回值
        JsonMsg jsonMsg = new JsonMsg();
        jsonMsg.setCode("false");
        //解析传入http请求body中的json
        JSONObject object = JSONObject.fromObject(json);
        if (!object.has("itemID")) {
            return jsonMsg;
        }
        //得到type键的值
        String type = "items";
        String myid = object.getString("itemID");
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection(type);

        BasicDBObject queryCondition = new BasicDBObject();//创建查询条件
        queryCondition.put("itemID", myid);

        MongoCursor<Document> dbCursor = collection.find(queryCondition).iterator();//开始查询
        if (dbCursor.hasNext()) {//遍历查询子结构
            JSONObject item = JSONObject.fromObject(dbCursor.next().toJson());
            if (type.equals("items")) {


                //维护price
                Iterator<String> tmpkeys = item.getJSONObject("prices").keys();
                String pricekey = "";
                while (tmpkeys.hasNext()) {
                    pricekey = tmpkeys.next();
                }
                Double price = item.getJSONObject("prices").getDouble(pricekey);
                item.put("price", price);
                item.put("prices", " " + item.getJSONObject("prices").toString());
                String img = item.getString("img");
                item.put("img", "http:" + img);



            }
            jsonMsg.setCode(type);
            jsonMsg.setData(item);
        } else {
            jsonMsg.setCode("false");
        }
        return jsonMsg;
    }

    /**
     * 根据Json中参数查找商品
     * @param object
     * @return
     */
    public JsonMsg queryItems(JSONObject object) {
//        Long d0=new Date().getTime();
        //查询初始化
        JsonMsg jsonMsg = new JsonMsg();
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("items");
        BasicDBObject queryCondition = new BasicDBObject();
        ArrayList<JSONObject> ary = new ArrayList<JSONObject>();
        BasicDBList values = new BasicDBList();
        ArrayList<JSONObject> result = new ArrayList<JSONObject>();

        String include = object.getString("include");//栏目筛选
        if (include.charAt(0) == '1') {
            values.add(new BasicDBObject("cpu", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(1) == '1') {
            values.add(new BasicDBObject("gpu", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(2) == '1') {
            values.add(new BasicDBObject("case", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(3) == '1') {
            values.add(new BasicDBObject("power", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(4) == '1') {
            values.add(new BasicDBObject("cooler_water", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(5) == '1') {
            values.add(new BasicDBObject("cooler_wind", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(6) == '1') {
            values.add(new BasicDBObject("hdd", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(7) == '1') {
            values.add(new BasicDBObject("ssd", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(8) == '1') {
            values.add(new BasicDBObject("memory", new BasicDBObject("$type", 3)));
        }
        if (include.charAt(9) == '1') {
            values.add(new BasicDBObject("motherboard", new BasicDBObject("$type", 3)));
        }
        queryCondition.put("$or", values);

        Double low = -100d;//确定low和high，价格上下限，错误处理
        Double high = 10000000d;
        if (object.has("low")) {
            if (!StringUtils.replace(object.getString("low"), " ", "").equals(""))
                low = object.getDouble("low");

        }
        if (object.has("high")) {
            if (!StringUtils.replace(object.getString("high"), " ", "").equals(""))
                high = object.getDouble("high");
        }

        String key = object.getString("key");//确定所有关键词，条件key以空格分隔存入keys
        List<String> keys_raw = Arrays.asList(StringUtils.split(key, " "));
        ArrayList<String> keys = new ArrayList<String>();
        for (int i = 0; i < keys_raw.size(); i++) {
            keys.add(StringUtils.replace(keys_raw.get(i), " ", "").toLowerCase());//小写&去空格
        }
        //
        BasicDBObject proj = new BasicDBObject();//查询属性投影
        proj.put("title", 1);
        proj.put("img", 1);
        proj.put("prices", 1);
        proj.put("itemID", 1);
        proj.put("comments", 1);

        BasicDBList values_and = new BasicDBList();//构造key的查询条件，在title中以$and条件搜索
        int i = 0;
        for (; i < keys.size(); i++) {
            BasicDBObject _cond1 = new BasicDBObject();
            BasicDBObject _cond2 = new BasicDBObject();
            _cond2.put("$regex", keys.get(i));
            _cond2.put("$options", "i");
            _cond1.put("title", _cond2);
            values_and.add(_cond1);
        }
        if (keys.size() != 0) {
            queryCondition.put("$and", values_and);
        }

        MongoCursor<Document> dbCursor = collection.find(queryCondition).projection(proj).iterator();//开始查询
        while (dbCursor.hasNext()) {
            JSONObject item = JSONObject.fromObject(dbCursor.next().toJson());
            boolean hasKey = true;
            if (hasKey) {
                Iterator<String> tmpkeys = item.getJSONObject("prices").keys();           //嵌套文档查询价格
                String pricekey = "";
                while (tmpkeys.hasNext()) {
                    pricekey = tmpkeys.next();
                }
                Double price = item.getJSONObject("prices").getDouble(pricekey);
                if (price <= high && price >= low) {
                    item.put("price", price);
                    item.put("prices", " " + item.getJSONObject("prices").toString());
                    item.put("img", "http:" + item.getString("img"));
                    result.add(item);
                }
            }
        }
        Long d1 = new Date().getTime();
        //排序
        String seq = object.getString("order");
        if (seq.equals("comprehensive")) {
            Collections.sort(result, new SortByCom());
        } else if (seq.equals("cost_performance")) {
            Collections.sort(result, new SortByPer());
        } else if (seq.equals("price_des")) {
            Collections.sort(result, new SortByPriceDes());
        } else if (seq.equals("price_asc")) {
            Collections.sort(result, new SortByPriceAsc());
        }
        Long d2 = new Date().getTime();
        jsonMsg.setCode("true");
        while(result.size()>limit)
        {
            result.remove(limit);
        }
        jsonMsg.setData(JSONArray.fromObject(result));
        return jsonMsg;
    }

    /**
     * 查询CPU数据
     * 注意属性的大小写
     * @param object
     * @return
     */
    public JsonMsg queryCPU(JSONObject object) {
        JsonMsg jsonMsg = new JsonMsg();
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("cpu");
        ArrayList<JSONObject> ary = new ArrayList<JSONObject>();
        BasicDBObject values = new BasicDBObject();
        ArrayList<JSONObject> result = new ArrayList<JSONObject>();
        if (object.has("Name") && !object.getString("Name").equals("")) {
            queryKeyString(object, "Name", values);
        }
        if (object.has("Codename") && !object.getString("Codename").equals("")) {
            queryKeyString(object, "Codename", values);
        }
        if (object.has("Cores") && !object.getString("Cores").equals("")) {
            queryKeyInt(object, "Cores", values);
        }
        if (object.has("Threads") && !object.getString("Threads").equals("")) {
            queryKeyInt(object, "Threads", values);
        }
        if (object.has("Socket") && !object.getString("Socket").equals("")) {
            queryKeyString(object, "Socket", values);
        }
        if (object.has("Process") && !object.getString("Process").equals("")) {
            queryKeyInt(object, "Process", values);
        }
        if (object.has("CacheL1") && !object.getString("CacheL1").equals("")) {
            queryKeyInt(object, "CacheL1", values);
        }
        if (object.has("TDP") && !object.getString("TDP").equals("")) {
            queryKeyInt(object, "TDP", values);
        }
        MongoCursor<Document> dbCursor = collection.find(values).iterator();
        while (dbCursor.hasNext()) {
            JSONObject item = JSONObject.fromObject(dbCursor.next().toJson());
            //是硬件则加上图片
            String type = "cpu";
            String hardwareimg = "";
            hardwareimg = RemoteMDBUtil.Host + "imgs/" + type + ".png";
            item.put("img", hardwareimg);
            result.add(item);
        }
        Collections.sort(result, new SortByDate());
        jsonMsg.setCode("true");
        while(result.size()>limit)
        {
            result.remove(limit);
        }
        jsonMsg.setData(JSONArray.fromObject(result));
        return jsonMsg;
    }

    /**
     * 查询GPU数据
     * @param object
     * @return
     */
    public JsonMsg queryGPU(JSONObject object) {
        JsonMsg jsonMsg = new JsonMsg();
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection("gpu");
        ArrayList<JSONObject> ary = new ArrayList<JSONObject>();
        BasicDBObject values = new BasicDBObject();
        ArrayList<JSONObject> result = new ArrayList<JSONObject>();
        if (object.has("Name") && !object.getString("Name").equals("")) {
            queryKeyString(object, "Name", values);
        }
        if (object.has("Chip") && !object.getString("Chip").equals("")) {
            queryKeyString(object, "Chip", values);
        }
        if (object.has("Bus") && !object.getString("Bus").equals("")) {
            queryKeyString(object, "Bus", values);
        }
        if (object.has("Memory_Size") && !object.getString("Memory_Size").equals("")) {
            queryKeyInt(object, "Memory_Size", values);
        }
        if (object.has("Memory_Type") && !object.getString("Memory_Type").equals("")) {
            queryKeyString(object, "Memory_Type", values);
        }
        if (object.has("Memory_Bus") && !object.getString("Memory_Bus").equals("")) {
            queryKeyInt(object, "Memory_Bus", values);
        }
        if (object.has("GPU_Clock") && !object.getString("GPU_Clock").equals("")) {
            queryKeyInt(object, "GPU_Clock", values);
        }
        MongoCursor<Document> dbCursor = collection.find(values).iterator();
        while (dbCursor.hasNext()) {
            JSONObject item = JSONObject.fromObject(dbCursor.next().toJson());
            //是硬件则加上图片
            String type = "gpu";
            String hardwareimg = "";
            hardwareimg = RemoteMDBUtil.Host + "imgs/" + type + ".png";
            item.put("img", hardwareimg);
            result.add(item);
        }
        Collections.sort(result, new SortByDate());
        jsonMsg.setCode("true");
        while(result.size()>limit)
        {
            result.remove(limit);
        }
        jsonMsg.setData(JSONArray.fromObject(result));
        return jsonMsg;
    }

    /**
     * 查询剩下半同构化的数据
     * @param object
     * @return
     */
    public JsonMsg queryHemiStructuredHardware(JSONObject object) {
        //变量初始化
        JsonMsg jsonMsg = new JsonMsg();
        MongoDatabase database = client.getDatabase(RemoteMDBUtil.DB);
        MongoCollection collection = database.getCollection(object.getString("type"));
        ArrayList<JSONObject> ary = new ArrayList<JSONObject>();
        BasicDBObject values = new BasicDBObject();
        ArrayList<JSONObject> result = new ArrayList<JSONObject>();

        String key = object.getString("key");//处理key参数,得到筛选关键词
        List<String> keys_raw = Arrays.asList(StringUtils.split(key, " "));
        ArrayList<String> keys = new ArrayList<String>();
        for (int i = 0; i < keys_raw.size(); i++) {
            keys.add(StringUtils.replace(keys_raw.get(i), " ", "").toLowerCase());//小写&去空格
        }
        MongoCursor<Document> dbCursor = collection.find().iterator();//开始查询
        while (dbCursor.hasNext()) {
            JSONObject item = JSONObject.fromObject(dbCursor.next().toJson());
            boolean hasKey = false;                   //key匹配查询
            if (keys.size() != 0) {
                int i = 0;
                for (; i < keys.size(); i++) {
                    if (!getInnerString(item).toLowerCase().contains(keys.get(i))) {
                        break;
                    }
                }
                if (i == keys.size()) {
                    hasKey = true;
                }
            } else {
                hasKey = true;
            }
            if (hasKey) {
//                是硬件则加上图片
                String type = object.getString("type");
                String hardwareimg = "";
                hardwareimg = RemoteMDBUtil.Host + "imgs/" + type + ".png";
                item.put("img", hardwareimg);
                //其他硬件加上三个参数
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
                    for (; cnt <=3 ; cnt++) {
                        item.put("string"+cnt,"");
                    }
                    result.add(item);
                }
            }
        }
        jsonMsg.setCode("true");
        while(result.size()>limit)
        {
            result.remove(limit);
        }
        jsonMsg.setData(JSONArray.fromObject(result));
        return jsonMsg;
    }

    public void queryKeyString(JSONObject object, String name, BasicDBObject values) {
        BasicDBObject query1 = new BasicDBObject();
        query1.put("$regex", object.getString(name));
        query1.put("$options", "i");
        values.put(name, query1);
    }

    public void queryKeyInt(JSONObject object, String name, BasicDBObject values) {
        BasicDBObject query1 = new BasicDBObject();
        values.put(name, object.getInt(name));
    }

    /**
     * 根据itemID查找图片
     * @param itemID
     * @return
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

    /**
     * 嵌套查询内部
     * @param json
     * @return
     */
    String getInnerString(JSONObject json) {
        String res = "";
        Iterator<String> iterator = json.keys();
        while (iterator.hasNext()) {
            String tmp = iterator.next();
            if (!tmp.equals("_id") && !tmp.equals("itemIDs")) {
                res = res + json.getJSONObject(tmp).toString();
            }
        }
        return res;
    }

    /**
     * sort内部类接口方法
     */
    class SortByCom implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            if (j1.getInt("comments") < j2.getInt("comments"))
                return 1;
            return -1;
        }
    }

    class SortByPer implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            if (j1.getDouble("comments") / j1.getDouble("price") < j2.getDouble("comments") / j2.getDouble("price"))
                return 1;
            return -1;
        }
    }

    class SortByPriceDes implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            if (j1.getDouble("price") < j2.getDouble("price"))
                return 1;
            return -1;
        }
    }

    class SortByPriceAsc implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            if (j1.getDouble("price") > j2.getDouble("price"))
                return 1;
            return -1;
        }
    }

    class SortByDate implements Comparator {
        public int compare(Object o1, Object o2) {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            if (!j1.has("Released")) {
                j1.put("Released", "1990-01-01");
            }
            if (!j2.has("Released")) {
                j2.put("Released", "1990-01-01");
            }
            return j2.getString("Released").compareTo(j1.getString("Released"));
        }
    }


}
