package model;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 云服务设施
 */
public class RemoteMDBUtil {
    //服务器公网ip
    public static String Host="http://47.96.102.28:8080/liteServer/";
    //数据库内网连接
    public static ServerAddress seed1 = new ServerAddress("dds-bp1375253460cc8433270.mongodb.rds.aliyuncs.com", 3717);
    //数据库外网连接
//    public static ServerAddress seed1 = new ServerAddress("dds-bp1375253460cc84-pub.mongodb.rds.aliyuncs.com", 3717);
    public static String username = "root";
    public static String password = "Aa123456";
    public static String DEFAULT_DB = "admin";
    public static String DB = "building";

    /**
     * 静态方法连接数据库
     * @return
     */
    public static MongoClient createMongoDBClient() {
        // 构建Seed列表
        List<ServerAddress> seedList = new ArrayList<ServerAddress>();
        seedList.add(seed1);
        // 构建鉴权信息
        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
        credentials.add(MongoCredential.createScramSha1Credential(username,DEFAULT_DB, password.toCharArray()));
        return new MongoClient(seedList, credentials);
    }

}
